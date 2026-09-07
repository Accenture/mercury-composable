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
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.platformlambda.core.models.EventEnvelope;

import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Claims-fixture pin (claims-registry claim id: {@code kafka-success-threshold}).
 *
 * <p>Documented behavior: the flow adapter treats any flow reply status <b>below 400</b> as
 * success - the message is acknowledged (offset committed) with <b>no retry and no dead-letter</b>.
 * A 201, 204, or 3xx reply is success, exactly like 200; 400 is the first failing status
 * ({@code KafkaFlowConsumer.deliver}: {@code response.getStatus() < 400}).</p>
 *
 * <p>Uses the same engine-free {@code invokeFlow} override seam as {@link KafkaFlowConsumerTest};
 * the embedded-broker path is unnecessary for this boundary pin.</p>
 */
// resource: these KafkaFlowConsumer fixtures are never start()ed, so the single-thread poll-loop
// executor never submits a task and never spawns a thread - there is no live resource to close.
@SuppressWarnings({"resource", "java:S2095"})
class ClaimKafkaSuccessThresholdTest {

    private static final String DLQ_TOPIC = "orders-poison";

    private static KafkaConsumerBinding binding() {
        return KafkaConsumerBinding.builder().topic("orders").flowId("order-flow")
                .dlqTopic(DLQ_TOPIC).build();
    }

    private static MockProducer<String, byte[]> autoCompletingProducer() {
        return new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
    }

    private static ConsumerRecord<String, byte[]> inboundRecord() {
        return new ConsumerRecord<>("orders", 0, 7L, "k", "payload".getBytes(UTF_8));
    }

    @ParameterizedTest(name = "flow reply status {0} is success: acknowledged, no retry, no DLQ")
    @ValueSource(ints = {201, 204, 302, 399})
    void statusBelow400IsSuccessNoRetryNoDeadLetter(int status) {
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        // retries are AVAILABLE (2) so "no retry" is a behavioral choice, not a policy accident
        RetryPolicy policy = new RetryPolicy(2, 0, new KafkaRequestPublisher(dlqProducer));
        FixedStatusConsumer consumer = new FixedStatusConsumer(status, policy);

        boolean commit = consumer.routeToFlow(inboundRecord());

        assertTrue(commit, "a status-" + status + " flow reply is success -> offset acknowledged");
        assertEquals(1, consumer.attempts.get(), "success on first attempt -> no retry");
        assertEquals(0, dlqProducer.history().size(), "a sub-400 reply is never dead-lettered");
    }

    @Test
    void status400IsTheFirstFailure() {
        // pins the boundary itself: loosening the threshold (e.g. to < 500) would make this pass as
        // success and fail the retry/dead-letter assertions below
        MockProducer<String, byte[]> dlqProducer = autoCompletingProducer();
        RetryPolicy policy = new RetryPolicy(1, 0, new KafkaRequestPublisher(dlqProducer));
        FixedStatusConsumer consumer = new FixedStatusConsumer(400, policy);

        boolean commit = consumer.routeToFlow(inboundRecord());

        assertTrue(commit, "durably dead-lettered, so the offset still commits");
        assertEquals(2, consumer.attempts.get(), "status 400 is a failure -> 1 initial attempt + 1 retry");
        assertEquals(1, dlqProducer.history().size(), "the exhausted 400 reply is dead-lettered");
    }

    /** A consumer whose flow always replies with the given status (engine never touched). */
    private static class FixedStatusConsumer extends KafkaFlowConsumer {
        final AtomicInteger attempts = new AtomicInteger();
        private final int status;

        FixedStatusConsumer(int status, RetryPolicy policy) {
            super(null, binding(), 200, policy, null);
            this.status = status;
        }

        @Override
        EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) {
            attempts.incrementAndGet();
            return new EventEnvelope().setStatus(status);
        }
    }
}
