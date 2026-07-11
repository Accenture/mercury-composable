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

package com.accenture.soa.demo.tasks;

import com.accenture.soa.demo.support.SyncErrorHandler;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The system-of-record tasks derive their trace context from the input headers and do not publish
 * (the flows do), so all three variants are directly testable. The SoaReply* tasks hand replies to
 * the Redis return-route coordinator and are exercised by the full multi-terminal run instead.
 */
class SoaTaskTest {

    private Map<String, String> flowHeaders(String cid) {
        Map<String, String> headers = new HashMap<>();
        headers.put("my_route", "system.of.record");
        headers.put("my_trace_id", "trace-9999");
        headers.put("my_trace_path", "KAFKA soa.request");
        if (cid != null) {
            headers.put("cid", cid);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    @Test
    void byteArrayVariantEchoesTheRequestWithMetadata() {
        byte[] request = "{\"action\":\"create\",\"order\":\"A-100\"}".getBytes(StandardCharsets.UTF_8);
        EventEnvelope result = new SystemOfRecord().handleEvent(flowHeaders("cid-1"), request, 1);
        assertEquals("cid-1", result.getHeaders().get("cid"));
        Map<String, Object> response = SimpleMapper.getInstance().getMapper().readValue(
                new String((byte[]) result.getBody(), StandardCharsets.UTF_8), Map.class);
        assertEquals("processed", response.get("status"));
        assertEquals("system-of-record", response.get("processedBy"));
        assertEquals("trace-9999", response.get("traceId"));
        assertEquals("create", ((Map<String, Object>) response.get("request")).get("action"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void jsonSchemaVariantSharesTheSameLogic() {
        Map<String, Object> request = Map.of("action", "update", "order", "B-200");
        EventEnvelope result = new SystemOfRecordJson().handleEvent(flowHeaders("cid-2"), request, 1);
        assertEquals("cid-2", result.getHeaders().get("cid"));
        Map<String, Object> response = SimpleMapper.getInstance().getMapper().readValue(
                new String((byte[]) result.getBody(), StandardCharsets.UTF_8), Map.class);
        assertEquals("processed", response.get("status"));
        assertEquals("update", ((Map<String, Object>) response.get("request")).get("action"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void avroVariantBuildsTheFlatClosedShapeReply() {
        Map<String, Object> request = Map.of("action", "create");
        EventEnvelope result = new SystemOfRecordAvro().handleEvent(flowHeaders(null), request, 1);
        assertFalse(result.getHeaders().containsKey("cid"));
        Map<String, Object> response = SimpleMapper.getInstance().getMapper().readValue(
                new String((byte[]) result.getBody(), StandardCharsets.UTF_8), Map.class);
        // flat reply matching the Avro SyncDemoMessage record exactly
        assertEquals(Map.of("action", "create", "status", "processed",
                "processedBy", "system-of-record", "traceId", "trace-9999"), response);
    }

    @Test
    void errorHandlerStatusPolicy() {
        SyncErrorHandler handler = new SyncErrorHandler();
        // a 408 timeout from sync.await passes through
        Map<String, Object> timeout = handler.handleEvent(Map.of(),
                Map.of("status", 408, "message", "Timeout for 5000 ms"), 1);
        assertEquals(408, timeout.get("status"));
        assertEquals("Timeout for 5000 ms", timeout.get("message"));
        assertEquals("error", timeout.get("type"));
        // any 5xx (backend unreachable) is re-mapped to a retriable 503
        Map<String, Object> backendDown = handler.handleEvent(Map.of(),
                Map.of("status", 500, "message", "Kafka publish failed"), 1);
        assertEquals(503, backendDown.get("status"));
        // defaults apply when the flow maps nothing
        Map<String, Object> defaults = handler.handleEvent(Map.of(), new HashMap<>(), 1);
        assertEquals(503, defaults.get("status"));
        assertEquals("Internal error", defaults.get("message"));
    }
}
