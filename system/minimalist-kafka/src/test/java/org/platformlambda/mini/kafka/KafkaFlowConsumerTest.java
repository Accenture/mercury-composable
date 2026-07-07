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
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import com.accenture.automation.EventScriptManager;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.Utility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The poll loop, flow routing, trace propagation, and commit-after-process are exercised end-to-end in
 * {@code KafkaFlowAdapterTest}; here we unit-test the record-to-dataset decoding and the retry/dead-letter
 * orchestration (by overriding the flow-invocation seam, so no running engine is needed).
 */
// resource: these KafkaFlowConsumer fixtures are never start()ed, so the single-thread poll-loop executor
// never submits a task and never spawns a thread - there is no live resource to close.
@SuppressWarnings({"resource", "java:S2095"})
class KafkaFlowConsumerTest {

    private static final String DLQ_TOPIC = "orders-poison";

    private static KafkaConsumerBinding.Builder binding() {
        return KafkaConsumerBinding.builder().topic("orders").flowId("order-flow");
    }

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
        Map<String, Object> metadata = (Map<String, Object>) dataset.get("metadata");
        assertEquals("topic-1", metadata.get("topic"));
        assertEquals(0, metadata.get("partition"));
        assertEquals(0L, metadata.get("offset"));
        assertEquals("k", metadata.get("key"));
    }

    @Test
    void metadataCarriesTheRecordsOwnTopicAndPartition() {
        // the record's actual topic/partition, not the binding's configured topic-pattern - the only way a
        // pattern-subscribed flow (or a reprocessing flow reading a dlq-topic) recovers the true source
        ConsumerRecord<String, byte[]> consumerRecord =
                new ConsumerRecord<>("events.de", 3, 42L, 1_700_000_000_000L, TimestampType.CREATE_TIME,
                        -1, -1, "order-9", "payload".getBytes(UTF_8), new RecordHeaders(), Optional.empty());

        Map<String, Object> metadata = KafkaFlowConsumer.toMetadata(consumerRecord);

        assertEquals("events.de", metadata.get("topic"));
        assertEquals(3, metadata.get("partition"));
        assertEquals(42L, metadata.get("offset"));
        assertEquals(1_700_000_000_000L, metadata.get("timestamp"));
        assertEquals("order-9", metadata.get("key"));
    }

    @Test
    void metadataOmitsKeyWhenRecordHasNone() {
        ConsumerRecord<String, byte[]> consumerRecord =
                new ConsumerRecord<>("orders", 0, 0L, null, "payload".getBytes(UTF_8));

        Map<String, Object> metadata = KafkaFlowConsumer.toMetadata(consumerRecord);

        assertFalse(metadata.containsKey("key"), "no key on the record -> no 'key' entry in metadata");
    }

    @Test
    void retriesThenDeadLettersAPoisonMessage() {
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(2, 0, new KafkaRequestPublisher(dlqProducer));
        AlwaysFailingConsumer consumer =
                new AlwaysFailingConsumer(Integer.MAX_VALUE, policy, binding().dlqTopic(DLQ_TOPIC).build());

        boolean commit = consumer.routeToFlow(inboundRecord());

        assertTrue(commit, "durably dead-lettered, so the offset is committed (not reprocessed forever)");
        assertEquals(3, consumer.attempts.get(), "1 initial attempt + 2 retries");
        assertEquals(1, dlqProducer.history().size(), "exhausted message routed to the DLQ");
        ProducerRecord<String, byte[]> dead = dlqProducer.history().getFirst();
        assertEquals(DLQ_TOPIC, dead.topic(), "the binding's explicitly configured dlq-topic, used verbatim");
        assertEquals("payload", new String(dead.value(), UTF_8));
        assertEquals("orders", new String(dead.headers().lastHeader("dlq.origin.topic").value(), UTF_8));
        assertNotNull(dead.headers().lastHeader("dlq.error"), "failure cause recorded on the DLQ record");
    }

    @Test
    void noDlqTopicConfiguredCommitsToAvoidStorm() {
        // a binding with no dlq-topic drops an exhausted message (no suffix fallback anymore)
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(0, 0, new KafkaRequestPublisher(dlqProducer));
        AlwaysFailingConsumer consumer =
                new AlwaysFailingConsumer(Integer.MAX_VALUE, policy, binding().build());   // no dlq-topic

        boolean commit = consumer.routeToFlow(inboundRecord());

        assertTrue(commit, "no dlq-topic configured -> drop-with-ERROR and commit (no endless redelivery)");
        assertEquals(0, dlqProducer.history().size(), "nothing published when no dlq-topic is configured");
    }

    @Test
    void deadLettersNon200FlowResponse() {
        // a flow that replies with a non-200 status is a failure (not a thrown exception) -> dead-lettered
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(0, 0, new KafkaRequestPublisher(dlqProducer));

        boolean commit = new FlowErrorConsumer(policy, binding().dlqTopic(DLQ_TOPIC).build())
                .routeToFlow(inboundRecord());

        assertTrue(commit, "a non-200 flow response is durably dead-lettered, so the offset commits");
        assertEquals(1, dlqProducer.history().size(), "the non-200 message routed to the DLQ");
    }

    @Test
    void dlqWriteFailureCommitsToAvoidStorm() {
        // a producer that never acknowledges -> the confirmed DLQ write times out (the last defense fails)
        MockProducer<String, byte[]> stuckProducer =
                new MockProducer<>(false, null, new StringSerializer(), new ByteArraySerializer());
        RetryPolicy policy = new RetryPolicy(0, 0, new KafkaRequestPublisher(stuckProducer));
        AlwaysFailingConsumer consumer =
                new AlwaysFailingConsumer(Integer.MAX_VALUE, policy, binding().dlqTopic(DLQ_TOPIC).build());

        boolean commit = consumer.routeToFlow(inboundRecord());

        assertTrue(commit, "a failed DLQ write is logged loudly and committed (message dropped) rather than "
                + "redelivered forever -> no recovery storm");
    }

    @Test
    void noDeadLetterPublisherCommitsToAvoidStorm() {
        RetryPolicy policy = new RetryPolicy(0, 0, null);   // no publisher available
        AlwaysFailingConsumer consumer =
                new AlwaysFailingConsumer(Integer.MAX_VALUE, policy, binding().dlqTopic(DLQ_TOPIC).build());

        assertTrue(consumer.routeToFlow(inboundRecord()),
                "no DLQ publisher -> drop-with-ERROR and commit (no endless redelivery)");
    }

    @Test
    void retrySucceedsBeforeDeadLettering() {
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(3, 0, new KafkaRequestPublisher(dlqProducer));
        // fail once, then succeed
        AlwaysFailingConsumer consumer = new AlwaysFailingConsumer(1, policy, binding().dlqTopic(DLQ_TOPIC).build());

        boolean commit = consumer.routeToFlow(inboundRecord());

        assertTrue(commit);
        assertEquals(2, consumer.attempts.get(), "failed once, succeeded on the retry");
        assertEquals(0, dlqProducer.history().size(), "no dead-letter on eventual success");
    }

    @Test
    void pinsPartitionWhenConfigured() {
        MockConsumer<String, byte[]> mock = new MockConsumer<>("earliest");
        RetryPolicy policy = new RetryPolicy(0, 0, null);
        KafkaConsumerBinding pinned = binding().partition(2).build();
        new KafkaFlowConsumer(mock, pinned, 1000, policy, null).subscribeOrAssign(mock);
        assertEquals(Set.of(new TopicPartition("orders", 2)), mock.assignment(), "pinned via manual assign");
        assertTrue(mock.subscription().isEmpty(), "no group subscription when pinned");
    }

    @Test
    void subscribesWhenNoPartition() {
        MockConsumer<String, byte[]> mock = new MockConsumer<>("earliest");
        RetryPolicy policy = new RetryPolicy(0, 0, null);
        new KafkaFlowConsumer(mock, binding().build(), 1000, policy, null).subscribeOrAssign(mock);
        assertEquals(Set.of("orders"), mock.subscription(), "group-managed subscribe when not pinned");
        assertTrue(mock.assignment().isEmpty(), "no manual assignment when subscribing");
    }

    @Test
    void autoCommitModeSkipsManualCommit() {
        MockConsumer<String, byte[]> mock = new MockConsumer<>("earliest");
        TopicPartition tp = new TopicPartition("orders", 0);
        AtomicInteger processed = new AtomicInteger();
        // runs at the start of the background thread's first poll(), right after subscribeOrAssign()
        mock.schedulePollTask(() -> {
            mock.rebalance(List.of(tp));
            mock.updateBeginningOffsets(Map.of(tp, 0L));
            mock.addRecord(new ConsumerRecord<>("orders", 0, 0L, "k", "payload".getBytes(UTF_8)));
        });
        RetryPolicy policy = new RetryPolicy(0, 0, null);
        KafkaConsumerBinding autoCommitBinding = binding().autoCommit(true).build();
        KafkaFlowConsumer consumer = new KafkaFlowConsumer(mock, autoCommitBinding, 1000, policy, null) {
            @Override
            EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) {
                processed.incrementAndGet();
                return new EventEnvelope().setStatus(200);
            }
        };

        consumer.start();
        long deadline = System.currentTimeMillis() + 2000;
        while (processed.get() == 0 && System.currentTimeMillis() < deadline) {
            Utility.getInstance().sleep(10);
        }
        assertEquals(1, processed.get(), "the record was processed by the flow");
        Map<TopicPartition, OffsetAndMetadata> committed = mock.committed(Set.of(tp));
        consumer.close();

        assertNull(committed.get(tp), "auto-commit mode must not manually commit - Kafka's own "
                + "periodic timer owns offset commits, not KafkaFlowConsumer");
    }

    @Test
    void businessCorrelationIdSurvivesRpcRequestPlumbing() {
        // EventScriptManager reads the business cid from the "correlation_id" HEADER, not from
        // EventEnvelope.getCorrelationId() (that field is overwritten by PostOffice.request's RPC inbox
        // plumbing before EventScriptManager ever sees the envelope) - so the forward envelope must carry
        // both, mirroring FlowExecutor.request's established pattern.
        EventEnvelope[] captured = new EventEnvelope[1];
        RetryPolicy policy = new RetryPolicy(0, 0, null);
        KafkaFlowConsumer consumer = new KafkaFlowConsumer(null, binding().build(), 1000, policy, null) {
            @Override
            EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) {
                captured[0] = forward;
                return new EventEnvelope().setStatus(200);
            }
        };

        consumer.routeToFlow(inboundRecord());

        assertEquals("cid-1", captured[0].getHeader(EventScriptManager.BUSINESS_CORRELATION_ID),
                "the business correlation-id must ride the header EventScriptManager actually reads");
        assertEquals("cid-1", captured[0].getCorrelationId());
    }

    @Test
    void subscribesWithPatternWhenConfigured() {
        MockConsumer<String, byte[]> mock = new MockConsumer<>("earliest");
        mock.updatePartitions("orders-1", List.of(new PartitionInfo("orders-1", 0, null, new Node[0], new Node[0])));
        KafkaConsumerBinding pattern = KafkaConsumerBinding.builder()
                .topicPattern("orders-\\d").flowId("order-flow").groupId("orders-group").build();
        RetryPolicy policy = new RetryPolicy(0, 0, null);

        new KafkaFlowConsumer(mock, pattern, 1000, policy, null).subscribeOrAssign(mock);

        assertEquals(Set.of("orders-1"), mock.subscription(), "pattern-matched topic joins the subscription");
        assertTrue(mock.assignment().stream().allMatch(tp -> tp.topic().equals("orders-1")));
    }

    private static MockProducer<String, byte[]> autoCompletingProducer() {
        return new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
    }

    private static ConsumerRecord<String, byte[]> inboundRecord() {
        ConsumerRecord<String, byte[]> r = new ConsumerRecord<>("orders", 0, 7L, "k", "payload".getBytes(UTF_8));
        r.headers().add("cid", "cid-1".getBytes(UTF_8));
        return r;
    }

    /** A consumer whose flow invocation throws the first {@code failTimes} attempts (engine never touched). */
    private static class AlwaysFailingConsumer extends KafkaFlowConsumer {
        final AtomicInteger attempts = new AtomicInteger();
        final int failTimes;

        AlwaysFailingConsumer(int failTimes, RetryPolicy policy, KafkaConsumerBinding binding) {
            // 200ms = the DLQ confirm-write timeout, keeping the failed-DLQ-write test fast
            super(null, binding, 200, policy, null);
            this.failTimes = failTimes;
        }

        @Override
        EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) throws ExecutionException {
            if (attempts.incrementAndGet() <= failTimes) {
                throw new ExecutionException("flow failed", new RuntimeException("boom"));
            }
            return new EventEnvelope().setStatus(200);   // success on the surviving attempt
        }
    }

    /** A consumer whose flow always replies with a non-200 status (a flow-level failure, not an exception). */
    private static class FlowErrorConsumer extends KafkaFlowConsumer {
        FlowErrorConsumer(RetryPolicy policy, KafkaConsumerBinding binding) {
            super(null, binding, 200, policy, null);
        }

        @Override
        EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) {
            return new EventEnvelope().setStatus(500).setBody("flow error");
        }
    }
}
