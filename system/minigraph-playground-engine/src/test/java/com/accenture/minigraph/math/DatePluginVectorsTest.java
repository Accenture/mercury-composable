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

import com.accenture.models.PluginFunction;
import com.accenture.services.plugins.date.AddDays;
import com.accenture.services.plugins.date.DaysBetween;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the shared conformance vectors of the addDays and daysBetween simple plugins, written engine-neutral so that
 * the same file can drive a twin in another engine: the same dates, the same counts and the same refusals.
 */
class DatePluginVectorsTest {
    private static final Map<String, PluginFunction> PLUGINS = Map.of(
            "addDays", new AddDays(),
            "daysBetween", new DaysBetween());
    private static final Map<String, String> ERRORS = Map.of(
            "arity", "Expected two input values",
            "not-a-date", "Expected a date written as YYYY-MM-DD",
            "not-a-calendar-date", "Not a calendar date",
            "bad-days", "must be a whole number",
            "out-of-range", "falls outside 0000-01-01 to 9999-12-31");

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> vectors() throws IOException {
        try (var in = DatePluginVectorsTest.class.getResourceAsStream("/date-plugin-vectors.json")) {
            assertNotNull(in, "date-plugin-vectors.json is missing");
            var doc = new Gson().fromJson(new String(in.readAllBytes()), Map.class);
            assertEquals("mercury-date-plugin-vectors", doc.get("format"));
            return (List<Map<String, Object>>) doc.get("vectors");
        }
    }

    /** Gson reads every JSON number as Double; an integral one is handed to the plugin as a Long, as a flow would. */
    private static Object call(Map<String, Object> vector) {
        var plugin = PLUGINS.get((String) vector.get("plugin"));
        assertNotNull(plugin, "unknown plugin in " + vector.get("id"));
        var args = ((List<?>) vector.get("args")).stream().map(a -> {
            if (a instanceof Double d && d == Math.rint(d) && !Double.isInfinite(d)) {
                return d.longValue();
            }
            return a;
        }).toArray();
        return plugin.calculate(args);
    }

    @Test
    void everyVectorHoldsInThePlugins() throws IOException {
        var all = vectors();
        assertTrue(all.size() > 50, "the vector file looks truncated: " + all.size());
        var ids = new HashSet<String>();
        for (var v : all) {
            var id = (String) v.get("id");
            assertTrue(ids.add(id), "duplicate vector id " + id);
            if (v.containsKey("expect")) {
                var expect = v.get("expect");
                var actual = call(v);
                if (expect instanceof Number n) {
                    assertInstanceOf(Integer.class, actual, id);
                    assertEquals(n.longValue(), ((Number) actual).longValue(), id);
                } else {
                    assertEquals(expect, actual, id);
                }
            } else {
                var code = (String) v.get("error");
                var e = assertThrows(RuntimeException.class, () -> call(v), id + " must fail with " + code);
                var pattern = ERRORS.get(code);
                assertNotNull(pattern, "unknown error code " + code + " in " + id);
                assertTrue(Pattern.compile(pattern).matcher(String.valueOf(e.getMessage())).find(),
                        id + ": '" + e.getMessage() + "' does not match " + code);
            }
        }
    }
}
