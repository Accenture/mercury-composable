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
import com.accenture.services.plugins.decision.SimpleMaxRange;
import com.accenture.services.plugins.decision.SimpleMinRange;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the shared conformance vectors of the minRange and maxRange simple plugins - byte-identical in the Rust
 * repository, where the same file drives the Rust twins, so the two engines type a bound the same way and refuse
 * the same ranges.
 */
class RangeBoundsVectorsTest {
    private static final Map<String, PluginFunction> PLUGINS = Map.of(
            "minRange", new SimpleMinRange(), "maxRange", new SimpleMaxRange());
    private static final Map<String, String> ERRORS = Map.of(
            "arity", "Expected one or two input values",
            "bad-range", "must be written as min\\.\\.max",
            "mixed-range", "mixes a numeric and a text bound",
            "empty-range", "is empty");

    private static JsonArray vectors() throws IOException {
        try (var in = RangeBoundsVectorsTest.class.getResourceAsStream("/range-bounds-vectors.json")) {
            assertNotNull(in, "range-bounds-vectors.json is missing");
            var doc = JsonParser.parseString(new String(in.readAllBytes())).getAsJsonObject();
            assertEquals("mercury-range-bounds-vectors", doc.get("format").getAsString());
            return doc.getAsJsonArray("vectors");
        }
    }

    /**
     * A vector value typed by its JSON literal, as the Rust twin reads it: a whole-number literal within the 64-bit
     * range is a Long, any other number (a fraction part, an exponent, beyond 64 bits) a Double, a string text, null null.
     */
    private static Object typed(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            var primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                var literal = primitive.getAsNumber().toString();
                if (literal.indexOf('.') < 0 && literal.indexOf('e') < 0 && literal.indexOf('E') < 0) {
                    try {
                        return Long.parseLong(literal);
                    } catch (NumberFormatException e) {
                        // beyond the 64-bit range: a double, below
                    }
                }
                return Double.parseDouble(literal);
            }
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            var list = new ArrayList<>();
            element.getAsJsonArray().forEach(item -> list.add(typed(item)));
            return list;
        }
        var map = new LinkedHashMap<String, Object>();
        element.getAsJsonObject().entrySet().forEach(entry -> map.put(entry.getKey(), typed(entry.getValue())));
        return map;
    }

    private static Object[] args(JsonObject vector) {
        var list = new ArrayList<>();
        vector.getAsJsonArray("args").forEach(item -> list.add(typed(item)));
        return list.toArray();
    }

    @Test
    void everyVectorHoldsInThePlugins() throws IOException {
        var all = vectors();
        assertTrue(all.size() > 30, "the vector file looks truncated: " + all.size());
        var ids = new HashSet<String>();
        for (var element : all) {
            var v = element.getAsJsonObject();
            var id = v.get("id").getAsString();
            assertTrue(ids.add(id), "duplicate vector id " + id);
            var plugin = PLUGINS.get(v.get("plugin").getAsString());
            assertNotNull(plugin, "unknown plugin in " + id);
            var args = args(v);
            if (v.has("error")) {
                var code = v.get("error").getAsString();
                var pattern = ERRORS.get(code);
                assertNotNull(pattern, "unknown error code " + code + " in " + id);
                var e = assertThrows(RuntimeException.class, () -> plugin.calculate(args), id + " must fail with " + code);
                assertTrue(Pattern.compile(pattern).matcher(String.valueOf(e.getMessage())).find(),
                        id + ": " + e.getMessage());
            } else {
                assertEquals(typed(v.get("expect")), plugin.calculate(args), id);
            }
        }
    }
}
