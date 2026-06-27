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

package org.platformlambda.support;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@code sync-to-async} status policy: a timeout (408) passes through unchanged, while
 * any 5xx (Kafka publish / Redis registration failure - the async backend is unreachable) becomes a
 * retriable 503. The 408 pass-through is also exercised end-to-end by {@code RestFlowMvpTest}.
 */
class SyncErrorHandlerTest {

    private final SyncErrorHandler handler = new SyncErrorHandler();

    @Test
    void timeoutPassesThroughAs408() {
        Map<String, Object> result = handler.handleEvent(Map.of(), Map.of("status", 408, "message", "timed out"), 1);
        assertEquals(408, result.get("status"));
        assertEquals("timed out", result.get("message"));
        assertEquals("error", result.get("type"));
    }

    @Test
    void publishFailureBecomes503() {
        Map<String, Object> result = handler.handleEvent(Map.of(), Map.of("status", 500, "message", "broker down"), 1);
        assertEquals(503, result.get("status"), "a backend-unreachable 5xx should be re-mapped to 503");
        assertEquals("broker down", result.get("message"));
    }

    @Test
    void defaultsToServiceUnavailableWhenStatusMissing() {
        Map<String, Object> result = handler.handleEvent(Map.of(), Map.of(), 1);
        assertEquals(503, result.get("status"));
        assertEquals("Internal error", result.get("message"));
    }
}
