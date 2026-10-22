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

package org.platformlambda.mini.jsonpath;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The JSONPath Compliance Test Suite (github.com/jsonpath-standard/jsonpath-compliance-test-suite, BSD-2 licensed;
 * cts.json at the commit in src/test/resources/cts/COMMIT, with its LICENSE beside it), run case by case against
 * the strict compiler. A valid case must select the expected values and the expected normalized paths - one of the
 * listed alternatives where the standard leaves an order open - and an invalid one must be refused. The suite is
 * the independent oracle; the Rust engine runs the same file.
 */
class ComplianceSuiteTest {
    private static final String SELECTOR = "selector";

    @Test
    void theSuiteIsThePinnedOne() throws IOException {
        List<Map<String, Object>> cases = cases();
        assertEquals(715, cases.size());
        assertEquals(249, cases.stream().filter(ComplianceSuiteTest::isInvalid).count());
    }

    @TestFactory
    Stream<DynamicTest> everyCaseOfTheSuite() throws IOException {
        return cases().stream().map(c -> DynamicTest.dynamicTest((String) c.get("name"), () -> run(c)));
    }

    private static void run(Map<String, Object> c) {
        String selector = (String) c.get(SELECTOR);
        if (isInvalid(c)) {
            assertThrows(JsonPathException.class, () -> JsonPath.compile(selector), selector);
            return;
        }
        JsonPath path = JsonPath.compile(selector);
        Object document = c.get("document");
        List<Object> values = path.select(document);
        List<String> paths = path.selectPaths(document);
        if (c.containsKey("result")) {
            assertEquals(c.get("result"), values, selector);
            if (c.containsKey("result_paths")) {
                assertEquals(c.get("result_paths"), paths, selector);
            }
        } else {
            List<?> results = (List<?>) c.get("results");
            int k = results.indexOf(values);
            assertTrue(k >= 0, selector + " selected " + values + ", none of " + results);
            if (c.containsKey("results_paths")) {
                assertEquals(((List<?>) c.get("results_paths")).get(k), paths, selector);
            }
        }
    }

    private static boolean isInvalid(Map<String, Object> c) {
        return Boolean.TRUE.equals(c.get("invalid_selector"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> cases() throws IOException {
        try (InputStream in = ComplianceSuiteTest.class.getResourceAsStream("/cts/cts.json")) {
            assertNotNull(in, "the compliance suite is missing from the test resources");
            Map<String, Object> suite = (Map<String, Object>) TestJson.parse(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return (List<Map<String, Object>>) suite.get("tests");
        }
    }
}
