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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * demo.catch.all is the default flow's task, so it must handle BOTH body shapes: the raw byte[] of a
 * record serializer: 'json' could not parse, and a decoded Map/List that simply matched no rule.
 */
class CatchAllProcessorTest {

    private final CatchAllProcessor processor = new CatchAllProcessor();

    private static Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("my_route", "demo.catch.all");
        headers.put("my_trace_id", "trace-1234");
        headers.put("my_trace_path", "KAFKA demo.orders");
        return headers;
    }

    @Test
    void rawBytesAreShownAsText() {
        Map<String, Object> response = processor.handleEvent(headers(),
                "not-json".getBytes(StandardCharsets.UTF_8), 1);
        assertEquals("not-json", response.get("received"));
        assertEquals("raw bytes (not a JSON object/array)", response.get("shape"));
        assertEquals("default", response.get("routedBy"));
        assertEquals("demo-catch-all-flow", response.get("processedBy"));
    }

    @Test
    void unmatchedJsonObjectPassesThroughAsMap() {
        Map<String, Object> body = Map.of("hello", "world");
        Map<String, Object> response = processor.handleEvent(headers(), body, 1);
        assertEquals(body, response.get("received"));
        assertEquals("map (JSON object, no rule matched)", response.get("shape"));
    }

    @Test
    void unmatchedJsonArrayPassesThroughAsList() {
        List<Object> body = List.of(Map.of("n", 1));
        Map<String, Object> response = processor.handleEvent(headers(), body, 1);
        assertEquals(body, response.get("received"));
        assertEquals("list (JSON array, no rule matched)", response.get("shape"));
    }
}
