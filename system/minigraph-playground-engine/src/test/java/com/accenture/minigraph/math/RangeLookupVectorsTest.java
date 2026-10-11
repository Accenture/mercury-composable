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
package com.accenture.minigraph.math;

import com.accenture.services.plugins.decision.SimpleRangeLookup;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the shared conformance vectors of the rangeLookup simple plugin - byte-identical in the Rust repository, where
 * the same file drives the Rust twin, so the two engines place a value in the same band and refuse the same tables.
 */
class RangeLookupVectorsTest {
    private static final SimpleRangeLookup PLUGIN = new SimpleRangeLookup();
    private static final Map<String, String> ERRORS = Map.of(
            "arity", "Expected two or three input values",
            "missing-keys", "Missing keys in input",
            "missing-key", "Missing key in input",
            "bad-range", "must be written as min\\.\\.max",
            "empty-range", "is empty",
            "mixed-table", "mixes numeric and text bounds",
            "mixed-band", "mixes a numeric and a text bound",
            "not-a-number", "Expected a decimal number in rangeLookup|Cannot convert",
            "boolean-operand", "Boolean operand in rangeLookup");

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> vectors() throws IOException {
        try (var in = RangeLookupVectorsTest.class.getResourceAsStream("/range-lookup-vectors.json")) {
            assertNotNull(in, "range-lookup-vectors.json is missing");
            var doc = new Gson().fromJson(new String(in.readAllBytes()), Map.class);
            assertEquals("mercury-range-lookup-vectors", doc.get("format"));
            return (List<Map<String, Object>>) doc.get("vectors");
        }
    }

    /** Gson reads every JSON number as Double; an integral one is handed to the plugin as a Long, as a flow would. */
    private static Object[] args(Map<String, Object> vector) {
        return ((List<?>) vector.get("args")).stream().map(a -> {
            if (a instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d)) {
                return d.longValue();
            }
            return a;
        }).toArray();
    }

    @Test
    void everyVectorHoldsInThePlugin() throws IOException {
        var all = vectors();
        assertTrue(all.size() > 30, "the vector file looks truncated: " + all.size());
        var ids = new HashSet<String>();
        for (var v : all) {
            var id = (String) v.get("id");
            assertTrue(ids.add(id), "duplicate vector id " + id);
            if (v.containsKey("error")) {
                var code = (String) v.get("error");
                var pattern = ERRORS.get(code);
                assertNotNull(pattern, "unknown error code " + code + " in " + id);
                var args = args(v);
                var e = assertThrows(RuntimeException.class, () -> PLUGIN.calculate(args), id + " must fail with " + code);
                assertTrue(Pattern.compile(pattern).matcher(String.valueOf(e.getMessage())).find(),
                        id + ": " + e.getMessage());
            } else {
                assertEquals(v.get("expect"), PLUGIN.calculate(args(v)), id);
            }
        }
    }
}
