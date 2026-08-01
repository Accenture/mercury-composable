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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.W3cTrace;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for second-level routing at the consumer level: rule selection driving the outbound
 * envelope (per-record flow_id, the task:// dispatch contract), the {@code serializer: 'json'}
 * best-effort decode, and task-failure dead-lettering - all through the invokeFlow seam, so no
 * running engine is needed (the end-to-end proof lives in {@link KafkaFlowAdapterTest}).
 */
// resource: these KafkaFlowConsumer fixtures are never start()ed, so the single-thread poll-loop
// executor never submits a task and never spawns a thread - there is no live resource to close.
// The MockProducer in the dead-letter tests is an in-memory fake owned by the test method.
@SuppressWarnings({"resource", "java:S2095"})
class KafkaFlowRoutingConsumerTest {

    private static final RetryPolicy NO_RETRY = new RetryPolicy(0, 0, null);
    private static final String CATCH_ALL_RULE = "default -> flow://catch-all-flow";

    private static KafkaConsumerBinding routingBinding(String... rules) {
        return KafkaConsumerBinding.builder().topic("mixed")
                .routingRules(RoutingRuleSet.compile(List.of(rules))).build();
    }

    private static ConsumerRecord<String, byte[]> inbound(byte[] payload, Map<String, String> headers) {
        ConsumerRecord<String, byte[]> consumerRecord = new ConsumerRecord<>("mixed", 0, 7L, "k", payload);
        headers.forEach((k, v) -> consumerRecord.headers().add(k, v.getBytes(StandardCharsets.UTF_8)));
        return consumerRecord;
    }

    /** Run one record through routeToFlow with the invokeFlow seam capturing the outbound envelope. */
    private static EventEnvelope captureForward(KafkaConsumerBinding binding,
                                                ConsumerRecord<String, byte[]> consumerRecord) {
        EventEnvelope[] captured = new EventEnvelope[1];
        KafkaFlowConsumer consumer = new KafkaFlowConsumer(null, binding, 1000, NO_RETRY, null) {
            @Override
            EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) {
                captured[0] = forward;
                return new EventEnvelope().setStatus(200);
            }
        };
        assertTrue(consumer.routeToFlow(consumerRecord), "a successful target run must allow the commit");
        return captured[0];
    }

    @Test
    void headerRuleSelectsTheFlowPerRecord() {
        KafkaConsumerBinding binding = routingBinding(
                "input.header.type(order) -> flow://order-flow", CATCH_ALL_RULE);
        EventEnvelope order = captureForward(binding,
                inbound("x".getBytes(StandardCharsets.UTF_8), Map.of("type", "order")));
        assertEquals("event.script.manager", order.getTo());
        assertEquals("order-flow", order.getHeader("flow_id"));
        EventEnvelope other = captureForward(binding,
                inbound("x".getBytes(StandardCharsets.UTF_8), Map.of("type", "invoice")));
        assertEquals("catch-all-flow", other.getHeader("flow_id"));
    }

    @Test
    void taskTargetCopiesHeadersAndPayloadWithBusinessCidTag() {
        KafkaConsumerBinding binding = routingBinding(
                "input.header.type(refund) -> task://v1.refund.processor", CATCH_ALL_RULE);
        byte[] payload = "refund-data".getBytes(StandardCharsets.UTF_8);
        EventEnvelope forward = captureForward(binding,
                inbound(payload, Map.of("type", "refund", "cid", "cid-42", "x-app", "demo")));
        assertEquals("v1.refund.processor", forward.getTo());
        // the whole payload is the body - no dataset wrapper on the task path
        assertArrayEquals(payload, (byte[]) forward.getBody());
        assertNull(forward.getHeader("flow_id"));
        // all inbound record headers are copied verbatim
        assertEquals("refund", forward.getHeader("type"));
        assertEquals("demo", forward.getHeader("x-app"));
        // the business correlation-id rides the engine-managed my_cid tag, never an envelope header
        assertEquals("cid-42", forward.getTag(EventEmitter.BUSINESS_CID_TAG));
    }

    @Test
    void taskTargetChainsOntoTheInboundTraceparent() {
        KafkaConsumerBinding binding = routingBinding("default -> task://raw.handler");
        String traceId = "11112222333344445555666677778888";
        String spanId = "aaaabbbbccccdddd";
        EventEnvelope forward = captureForward(binding, inbound("x".getBytes(StandardCharsets.UTF_8),
                Map.of("traceparent", W3cTrace.format(traceId, spanId))));
        assertEquals(traceId, forward.getTraceId());
        assertEquals(spanId, forward.getSpanId());
    }

    @Test
    void jsonSerializerEnablesBodyRulesAndMapDelivery() {
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("mixed").jsonSerializer(true)
                .routingRules(RoutingRuleSet.compile(List.of(
                        "input.body.event.kind(refund) -> task://v1.refund.processor", CATCH_ALL_RULE)))
                .build();
        byte[] json = "{\"event\":{\"kind\":\"refund\"},\"amount\":10}".getBytes(StandardCharsets.UTF_8);
        EventEnvelope forward = captureForward(binding, inbound(json, Map.of()));
        assertEquals("v1.refund.processor", forward.getTo());
        Map<?, ?> body = assertInstanceOf(Map.class, forward.getBody());
        assertEquals("refund", ((Map<?, ?>) body.get("event")).get("kind"));
    }

    @Test
    void malformedJsonFallsThroughToDefaultWithRawBytes() {
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("mixed").jsonSerializer(true)
                .routingRules(RoutingRuleSet.compile(List.of(
                        "input.body.event.kind(refund) -> task://v1.refund.processor", CATCH_ALL_RULE)))
                .build();
        byte[] malformed = "{not-json".getBytes(StandardCharsets.UTF_8);
        EventEnvelope forward = captureForward(binding, inbound(malformed, Map.of()));
        // no special poison handling: the raw bytes pass to the default target's input dataset as-is
        assertEquals("catch-all-flow", forward.getHeader("flow_id"));
        Map<?, ?> dataset = assertInstanceOf(Map.class, forward.getBody());
        assertArrayEquals(malformed, (byte[]) dataset.get("body"));
    }

    @Test
    void bestEffortJsonParsesObjectsAndArraysAndKeepsBytesOtherwise() {
        assertInstanceOf(Map.class,
                KafkaFlowConsumer.bestEffortJson("{\"a\":1}".getBytes(StandardCharsets.UTF_8)));
        assertInstanceOf(List.class,
                KafkaFlowConsumer.bestEffortJson("[1,2]".getBytes(StandardCharsets.UTF_8)));
        byte[] scalar = "42".getBytes(StandardCharsets.UTF_8);
        assertSame(scalar, KafkaFlowConsumer.bestEffortJson(scalar), "a scalar keeps the raw bytes");
        byte[] malformed = "{oops".getBytes(StandardCharsets.UTF_8);
        assertSame(malformed, KafkaFlowConsumer.bestEffortJson(malformed), "malformed JSON keeps the raw bytes");
        byte[] empty = new byte[0];
        assertSame(empty, KafkaFlowConsumer.bestEffortJson(empty));
    }

    @Test
    void failedTaskTargetIsDeadLettered() {
        MockProducer<String, byte[]> dlq =
                new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
        RetryPolicy policy = new RetryPolicy(1, 0, new KafkaRequestPublisher(dlq));
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("mixed").dlqTopic("mixed-dlq")
                .routingRules(RoutingRuleSet.compile(List.of("default -> task://always.failing"))).build();
        KafkaFlowConsumer consumer = new KafkaFlowConsumer(null, binding, 200, policy, null) {
            @Override
            EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) {
                return new EventEnvelope().setStatus(500);
            }
        };
        assertTrue(consumer.routeToFlow(inbound("x".getBytes(StandardCharsets.UTF_8), Map.of())),
                "a durably dead-lettered message must allow the commit");
        assertEquals(1, dlq.history().size());
        assertEquals("mixed-dlq", dlq.history().getFirst().topic());
        // the failure cause names the task target, not a flow
        String dlqError = new String(dlq.history().getFirst().headers().lastHeader("dlq.error").value(),
                StandardCharsets.UTF_8);
        assertTrue(dlqError.contains("task 'always.failing'"), dlqError);
    }

    @Test
    void synchronousDispatchErrorJoinsTheRetryDlqEnvelope() {
        // a task route released AFTER startup validation makes po.request throw SYNCHRONOUSLY
        // ("Route X not found") - it must dead-letter like any other failure, not kill the poll thread
        MockProducer<String, byte[]> dlq =
                new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
        RetryPolicy policy = new RetryPolicy(0, 0, new KafkaRequestPublisher(dlq));
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder().topic("mixed").dlqTopic("mixed-dlq")
                .routingRules(RoutingRuleSet.compile(List.of("default -> task://vanished.route"))).build();
        KafkaFlowConsumer consumer = new KafkaFlowConsumer(null, binding, 200, policy, null) {
            @Override
            EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath) {
                throw new IllegalArgumentException("Route vanished.route not found");
            }
        };
        assertTrue(consumer.routeToFlow(inbound("x".getBytes(StandardCharsets.UTF_8), Map.of())),
                "a durably dead-lettered message must allow the commit");
        assertEquals(1, dlq.history().size());
        String dlqError = new String(dlq.history().getFirst().headers().lastHeader("dlq.error").value(),
                StandardCharsets.UTF_8);
        assertTrue(dlqError.contains("Route vanished.route not found"), dlqError);
    }
}
