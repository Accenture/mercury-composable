/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.mini.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.platformlambda.core.models.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The poll loop, flow routing, trace propagation, and commit-after-process are exercised end-to-end in
 * {@code KafkaFlowAdapterTest}; here we unit-test the record-to-dataset decoding and the retry/dead-letter
 * orchestration (by overriding the flow-invocation seam, so no running engine is needed).
 */
class KafkaFlowConsumerTest {

    @Test
    @SuppressWarnings("unchecked")
    void decodesRecordIntoFlowDataset() {
        ConsumerRecord<String, byte[]> consumerRecord =
                new ConsumerRecord<>("topic-1", 0, 0L, "k", "{\"a\":1}".getBytes(UTF_8));
        consumerRecord.headers().add("cid", "cid-1".getBytes(UTF_8));
        consumerRecord.headers().add("traceparent",
                "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01".getBytes(UTF_8));

        Map<String, Object> dataset = KafkaFlowConsumer.toDataset(consumerRecord);

        Map<String, String> headers = (Map<String, String>) dataset.get("header");
        assertEquals("cid-1", headers.get("cid"));
        assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", headers.get("traceparent"));
        assertEquals("{\"a\":1}", new String((byte[]) dataset.get("body"), UTF_8));
    }

    @Test
    void retriesThenDeadLettersAPoisonMessage() {
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(2, 0, ".dlq", new KafkaRequestPublisher(dlqProducer));
        AlwaysFailingConsumer consumer = new AlwaysFailingConsumer(Integer.MAX_VALUE, policy);

        boolean commit = consumer.routeToFlow(record("payload"));

        assertTrue(commit, "durably dead-lettered, so the offset is committed (not reprocessed forever)");
        assertEquals(3, consumer.attempts.get(), "1 initial attempt + 2 retries");
        assertEquals(1, dlqProducer.history().size(), "exhausted message routed to the DLQ");
        ProducerRecord<String, byte[]> dead = dlqProducer.history().getFirst();
        assertEquals("orders.dlq", dead.topic(), "per-topic DLQ = <topic> + suffix");
        assertEquals("payload", new String(dead.value(), UTF_8));
        assertEquals("orders", new String(dead.headers().lastHeader("dlq.origin.topic").value(), UTF_8));
        assertTrue(dead.headers().lastHeader("dlq.error") != null, "failure cause recorded on the DLQ record");
    }

    @Test
    void usesConfigurableDlqSuffix() {
        // suffix conventions are arbitrary across orgs; the knob covers them all (still per-topic, never global)
        assertEquals("orders.dlq", deadLetterTopicFor(".dlq"));
        assertEquals("orders-DLQ", deadLetterTopicFor("-DLQ"));
        assertEquals("orders_DLQ", deadLetterTopicFor("_DLQ"));   // a real client convention
    }

    private static String deadLetterTopicFor(String suffix) {
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(0, 0, suffix, new KafkaRequestPublisher(dlqProducer));
        new AlwaysFailingConsumer(Integer.MAX_VALUE, policy).routeToFlow(record("payload"));
        return dlqProducer.history().getFirst().topic();
    }

    @Test
    void deadLetterWriteFailureBlocksTheCommit() {
        // a producer that never acknowledges -> the confirmed DLQ write times out
        MockProducer<String, byte[]> stuckProducer =
                new MockProducer<>(false, null, new StringSerializer(), new ByteArraySerializer());
        RetryPolicy policy = new RetryPolicy(0, 0, ".dlq", new KafkaRequestPublisher(stuckProducer));
        AlwaysFailingConsumer consumer = new AlwaysFailingConsumer(Integer.MAX_VALUE, policy);

        boolean commit = consumer.routeToFlow(record("payload"));

        assertFalse(commit, "a failed dead-letter write must NOT commit -> the message redelivers (no loss)");
    }

    @Test
    void noDeadLetterPublisherBlocksTheCommit() {
        RetryPolicy policy = new RetryPolicy(0, 0, ".dlq", null);   // no publisher available
        AlwaysFailingConsumer consumer = new AlwaysFailingConsumer(Integer.MAX_VALUE, policy);

        assertFalse(consumer.routeToFlow(record("payload")),
                "no DLQ publisher -> cannot store the message -> do not commit");
    }

    @Test
    void retrySucceedsBeforeDeadLettering() {
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(3, 0, ".dlq", new KafkaRequestPublisher(dlqProducer));
        AlwaysFailingConsumer consumer = new AlwaysFailingConsumer(1, policy);   // fail once, then succeed

        boolean commit = consumer.routeToFlow(record("payload"));

        assertTrue(commit);
        assertEquals(2, consumer.attempts.get(), "failed once, succeeded on the retry");
        assertEquals(0, dlqProducer.history().size(), "no dead-letter on eventual success");
    }

    @Test
    void pinsPartitionWhenConfigured() {
        MockConsumer<String, byte[]> mock = new MockConsumer<>("earliest");
        RetryPolicy policy = new RetryPolicy(0, 0, ".dlq", null);
        new KafkaFlowConsumer(mock, "orders", "order-flow", 1000, policy, 2).subscribeOrAssign(mock);
        assertEquals(Set.of(new TopicPartition("orders", 2)), mock.assignment(), "pinned via manual assign");
        assertTrue(mock.subscription().isEmpty(), "no group subscription when pinned");
    }

    @Test
    void subscribesWhenNoPartition() {
        MockConsumer<String, byte[]> mock = new MockConsumer<>("earliest");
        RetryPolicy policy = new RetryPolicy(0, 0, ".dlq", null);
        new KafkaFlowConsumer(mock, "orders", "order-flow", 1000, policy, null).subscribeOrAssign(mock);
        assertEquals(Set.of("orders"), mock.subscription(), "group-managed subscribe when not pinned");
        assertTrue(mock.assignment().isEmpty(), "no manual assignment when subscribing");
    }

    private static MockProducer<String, byte[]> autoCompletingProducer() {
        return new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
    }

    private static ConsumerRecord<String, byte[]> record(String body) {
        ConsumerRecord<String, byte[]> r = new ConsumerRecord<>("orders", 0, 7L, "k", body.getBytes(UTF_8));
        r.headers().add("cid", "cid-1".getBytes(UTF_8));
        return r;
    }

    /** A consumer whose flow invocation fails the first {@code failTimes} attempts (engine never touched). */
    private static class AlwaysFailingConsumer extends KafkaFlowConsumer {
        final AtomicInteger attempts = new AtomicInteger();
        final int failTimes;

        AlwaysFailingConsumer(int failTimes, RetryPolicy policy) {
            // small flow timeout doubles as the DLQ confirm-write timeout, keeping the failure test fast
            super(null, "orders", "order-flow", 200, policy, null);
            this.failTimes = failTimes;
        }

        @Override
        void invokeFlow(EventEnvelope forward, String traceId, String tracePath) throws ExecutionException {
            if (attempts.incrementAndGet() <= failTimes) {
                throw new ExecutionException("flow failed", new RuntimeException("boom"));
            }
        }
    }
}
