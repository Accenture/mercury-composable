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

/**
 * demo.refund.processor is a task:// routing target: the adapter invokes it directly with the
 * decoded payload as the body and the business cid injected as my_correlation_id. Directly testable
 * with the reserved my_* headers the adapter's dispatch would carry.
 */
class RefundProcessorTest {

    private final RefundProcessor processor = new RefundProcessor();

    @Test
    void acknowledgesTheRefundWithTheBusinessCid() {
        Map<String, String> headers = new HashMap<>();
        headers.put("my_route", "demo.refund.processor");
        headers.put("my_trace_id", "trace-1234");
        headers.put("my_trace_path", "KAFKA demo.orders");
        headers.put("my_correlation_id", "refund-001");
        Map<String, Object> refund = Map.of("event", Map.of("kind", "refund"), "orderId", "abc123");
        Map<String, Object> ack = processor.handleEvent(headers, refund, 1);
        assertEquals("refund recorded", ack.get("status"));
        assertEquals(refund, ack.get("refund"));
        assertEquals("refund-001", ack.get("cid"));
    }
}
