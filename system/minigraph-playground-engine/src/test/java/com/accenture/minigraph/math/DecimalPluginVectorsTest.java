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
import com.accenture.services.plugins.arithmetic.DecimalAdd;
import com.accenture.services.plugins.arithmetic.DecimalCompare;
import com.accenture.services.plugins.arithmetic.DecimalDiv;
import com.accenture.services.plugins.arithmetic.DecimalMod;
import com.accenture.services.plugins.arithmetic.DecimalMultiply;
import com.accenture.services.plugins.arithmetic.DecimalRound;
import com.accenture.services.plugins.arithmetic.DecimalSubtract;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the shared conformance vectors of the decimal simple plugins (RFC-0001 item 8). Every vector that also
 * carries the same computation as a DECIMAL statement must give the same answer there, so the plugin family and
 * the statement cannot drift apart: they are two implementations of one specification.
 */
class DecimalPluginVectorsTest {
    private static final Map<String, PluginFunction> PLUGINS = Map.of(
            "decimalAdd", new DecimalAdd(),
            "decimalSubtract", new DecimalSubtract(),
            "decimalMultiply", new DecimalMultiply(),
            "decimalDiv", new DecimalDiv(),
            "decimalMod", new DecimalMod(),
            "decimalRound", new DecimalRound(),
            "decimalCompare", new DecimalCompare());
    private static final Map<String, String> ERRORS = Map.of(
            "division-by-zero", "Division by zero",
            "round-arity", "takes three arguments",
            "rounding-mode", "mode of decimalRound must be",
            "scale", "scale of decimalRound must be",
            "boolean-operand", "Boolean operand",
            "not-a-number", "Cannot convert|Expected a decimal number",
            "arity", "Expected");

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> vectors() throws IOException {
        try (var in = DecimalPluginVectorsTest.class.getResourceAsStream("/decimal-plugin-vectors.json")) {
            assertNotNull(in, "decimal-plugin-vectors.json is missing");
            var doc = new Gson().fromJson(new String(in.readAllBytes()), Map.class);
            assertEquals("mercury-decimal-plugin-vectors", doc.get("format"));
            return (List<Map<String, Object>>) doc.get("vectors");
        }
    }

    private static Object call(Map<String, Object> vector) {
        var plugin = PLUGINS.get((String) vector.get("plugin"));
        assertNotNull(plugin, "unknown plugin in " + vector.get("id"));
        var args = ((List<?>) vector.get("args")).toArray();
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

    @Test
    void thePluginsAgreeWithTheDecimalStatement() throws IOException {
        int checked = 0;
        for (var v : vectors()) {
            if (v.containsKey("expression")) {
                var id = (String) v.get("id");
                assertEquals(v.get("expect"), DecimalEvaluator.evaluate((String) v.get("expression")),
                        id + ": the DECIMAL statement disagrees with the vector");
                checked++;
            }
        }
        assertTrue(checked > 40, "too few vectors are cross-checked against the statement: " + checked);
    }
}
