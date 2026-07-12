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

package com.accenture.kafka.demo.tasks;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * demo.processor is self-contained (the flow does the publishing): it wraps the inbound text with
 * processing metadata. The task derives its trace context from the input headers, so it is directly
 * testable with the reserved my_route / my_trace_id headers a flow would carry.
 */
class DemoProcessorTest {

    private final DemoProcessor processor = new DemoProcessor();

    @SuppressWarnings("unchecked")
    @Test
    void wrapsTheInboundTextWithProcessingMetadata() {
        Map<String, String> headers = new HashMap<>();
        headers.put("my_route", "demo.processor");
        headers.put("my_trace_id", "trace-1234");
        headers.put("my_trace_path", "KAFKA demo.inbound");
        headers.put("cid", "order-001");
        byte[] input = "hello kafka".getBytes(StandardCharsets.UTF_8);
        EventEnvelope result = processor.handleEvent(headers, input, 1);
        assertEquals("order-001", result.getHeaders().get("cid"));
        assertInstanceOf(byte[].class, result.getBody());
        Map<String, Object> response = SimpleMapper.getInstance().getMapper().readValue(
                new String((byte[]) result.getBody(), StandardCharsets.UTF_8), Map.class);
        assertEquals("hello kafka", response.get("received"));
        assertEquals("kafka-demo", response.get("processedBy"));
        assertEquals("trace-1234", response.get("traceId"));
        assertNotNull(response.get("processedAt"));
    }

    @Test
    void withoutCorrelationIdNoCidHeaderIsStamped() {
        Map<String, String> headers = new HashMap<>();
        headers.put("my_route", "demo.processor");
        EventEnvelope result = processor.handleEvent(
                headers, "plain".getBytes(StandardCharsets.UTF_8), 1);
        assertFalse(result.getHeaders().containsKey("cid"));
    }
}
