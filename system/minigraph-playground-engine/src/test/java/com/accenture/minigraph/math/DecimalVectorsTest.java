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

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the shared conformance vectors of the DECIMAL statement (RFC-0001). The file is engine-neutral: an
 * expected error is a code, and each engine matches the code against its own message.
 */
class DecimalVectorsTest {
    private static final Map<String, String> ERRORS = Map.ofEntries(
            Map.entry("division-by-zero", "Division by zero"),
            Map.entry("refused-function", "is not available in a DECIMAL statement"),
            Map.entry("refused-constant", "is not available in a DECIMAL statement"),
            Map.entry("whole-number", "must be a whole number"),
            Map.entry("bound", "is limited to"),
            Map.entry("round-arity", "takes three arguments"),
            Map.entry("rounding-mode", "mode of round\\(\\) must be"),
            Map.entry("unknown-function", "Unknown function"),
            Map.entry("unknown-identifier", "Unknown identifier"),
            Map.entry("identifier-is-function", "Identifier is a function"),
            Map.entry("boolean-operand", "Boolean operand"),
            Map.entry("boolean-result", "Boolean result"),
            Map.entry("not-a-number", "Expected number"),
            Map.entry("type-mismatch", "Type mismatch"),
            Map.entry("invalid-number", "Invalid number"));

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> vectors() throws IOException {
        try (var in = DecimalVectorsTest.class.getResourceAsStream("/decimal-vectors.json")) {
            assertNotNull(in, "decimal-vectors.json is missing");
            var doc = new Gson().fromJson(new String(in.readAllBytes()), Map.class);
            assertEquals("mercury-decimal-vectors", doc.get("format"));
            return (List<Map<String, String>>) doc.get("vectors");
        }
    }

    @Test
    void everyVectorHoldsInTheDecimalEvaluator() throws IOException {
        var all = vectors();
        assertTrue(all.size() > 100, "the vector file looks truncated: " + all.size());
        var ids = new HashSet<String>();
        for (var v : all) {
            var id = v.get("id");
            assertTrue(ids.add(id), "duplicate vector id " + id);
            var expression = v.get("expression");
            if (v.containsKey("expect")) {
                assertEquals(v.get("expect"), DecimalEvaluator.evaluate(expression), id + ": " + expression);
            } else {
                var code = v.get("error");
                var e = assertThrows(RuntimeException.class, () -> DecimalEvaluator.evaluate(expression),
                        id + ": " + expression + " must fail with " + code);
                if ("parse-error".equals(code)) {
                    assertInstanceOf(ParseException.class, e, id);
                } else {
                    var pattern = ERRORS.get(code);
                    assertNotNull(pattern, "unknown error code " + code + " in " + id);
                    assertTrue(Pattern.compile(pattern).matcher(String.valueOf(e.getMessage())).find(),
                            id + ": '" + e.getMessage() + "' does not match " + code);
                }
            }
        }
    }
}
