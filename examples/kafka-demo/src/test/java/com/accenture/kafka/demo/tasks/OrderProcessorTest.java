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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * demo.order.processor receives the serializer-decoded Map (the flow's 'input.body -> *') plus the
 * routing key under the 'type' header. It is directly testable with the reserved my_* headers a
 * flow would carry.
 */
class OrderProcessorTest {

    private final OrderProcessor processor = new OrderProcessor();

    @Test
    void wrapsTheOrderWithRoutingAndProcessingMetadata() {
        Map<String, String> headers = new HashMap<>();
        headers.put("my_route", "demo.order.processor");
        headers.put("my_trace_id", "trace-1234");
        headers.put("my_trace_path", "KAFKA demo.orders");
        headers.put("type", "order");
        Map<String, Object> order = Map.of("item", "keyboard", "qty", 1);
        Map<String, Object> response = processor.handleEvent(headers, order, 1);
        assertEquals(order, response.get("order"));
        assertEquals("input.header.type(order)", response.get("routedBy"));
        assertEquals("demo-order-flow", response.get("processedBy"));
        assertEquals("trace-1234", response.get("traceId"));
        assertNotNull(response.get("processedAt"));
    }

    @Test
    void echoesTheWildcardMatchedRoutingKey() {
        Map<String, String> headers = new HashMap<>();
        headers.put("my_route", "demo.order.processor");
        headers.put("type", "order-42");
        Map<String, Object> response = processor.handleEvent(headers, Map.of("item", "mouse"), 1);
        assertEquals("input.header.type(order-42)", response.get("routedBy"));
    }
}
