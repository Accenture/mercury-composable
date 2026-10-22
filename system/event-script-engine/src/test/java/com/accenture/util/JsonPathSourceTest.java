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

package com.accenture.util;

import com.accenture.setup.TestBase;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.jsonpath.JsonPathException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RFC-0010 WP3: a JSONPath source is checked against RFC 9535 when a flow or a graph is compiled. The vectors in
 * jsonpath-source-vectors.json are shared byte for byte with the Rust engine.
 */
class JsonPathSourceTest extends TestBase {
    private static final DataMappingHelper helper = DataMappingHelper.getInstance();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> vectors() {
        InputStream in = JsonPathSourceTest.class.getResourceAsStream("/jsonpath-source-vectors.json");
        assertNotNull(in, "the vector file must exist");
        return SimpleMapper.getInstance().getMapper().readValue(Utility.getInstance().stream2str(in), Map.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> section(String name) {
        return (List<Map<String, Object>>) vectors().get(name);
    }

    private static JsonPathSource.Form form(String name) {
        return JsonPathSource.Form.valueOf(name.toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    @TestFactory
    List<DynamicTest> sourcesAreCheckedWhereEachReferenceLands() {
        List<DynamicTest> tests = new ArrayList<>();
        for (var v : section("sources")) {
            var source = (String) v.get("source");
            var form = form((String) v.get("form"));
            tests.add(DynamicTest.dynamicTest(v.get("form") + " " + source, () -> {
                if (v.containsKey("sites")) {
                    var sites = JsonPathSource.references(source, form).stream()
                            .map(r -> r.site().name().toLowerCase(Locale.ROOT)).toList();
                    assertEquals(v.get("sites"), sites, "where each reference lands");
                } else {
                    assertThrows(JsonPathException.class, () -> JsonPathSource.references(source, form));
                }
                if (Boolean.TRUE.equals(v.get("valid"))) {
                    assertDoesNotThrow(() -> JsonPathSource.check(source, form));
                } else {
                    assertThrows(JsonPathException.class, () -> JsonPathSource.check(source, form));
                }
            }));
        }
        assertEquals(33, tests.size(), "source vector count");
        return tests;
    }

    @TestFactory
    List<DynamicTest> pluginArgumentsSplitOutsideAJsonPathsBracketsAndLiterals() {
        List<DynamicTest> tests = new ArrayList<>();
        for (var v : section("plugin_arguments")) {
            var arguments = (String) v.get("arguments");
            tests.add(DynamicTest.dynamicTest("f:x(" + arguments + ")", () -> {
                var split = helper.splitTopLevelArguments(arguments).stream().map(String::trim).toList();
                assertEquals(v.get("split"), split);
            }));
        }
        assertEquals(11, tests.size(), "plugin argument vector count");
        return tests;
    }

    @TestFactory
    List<DynamicTest> aReferenceKeyIsNeverAJsonPath() {
        List<DynamicTest> tests = new ArrayList<>();
        for (var v : section("reference_keys")) {
            var key = (String) v.get("key");
            tests.add(DynamicTest.dynamicTest("{" + key + "}", () -> {
                if (Boolean.TRUE.equals(v.get("refused"))) {
                    var ex = assertThrows(IllegalArgumentException.class, () -> JsonPathSource.refuseJsonPathKey(key));
                    assertEquals("JSONPath is not supported in a reference - {" + key +
                            "}; map the value to a model variable first", ex.getMessage());
                } else {
                    assertDoesNotThrow(() -> JsonPathSource.refuseJsonPathKey(key));
                }
            }));
        }
        assertEquals(5, tests.size(), "reference key vector count");
        return tests;
    }

    @Test
    void aPositionIsStatedInTheSourceAsWritten() {
        // the stand-in 'x' is ten characters shorter than {model.key}, so the parser's position moves by ten
        var plain = assertThrows(JsonPathException.class,
                () -> JsonPathSource.check("$.input.body.x[?(@.a in [1])]", JsonPathSource.Form.PLAIN));
        var source = "$.input.body.{model.key}[?(@.a in [1])]";
        var templated = assertThrows(JsonPathException.class,
                () -> JsonPathSource.check(source, JsonPathSource.Form.EVENT_SCRIPT));
        assertEquals(plain.getPosition() + 10, templated.getPosition());
        assertEquals(plain.getReason(), templated.getReason());
        assertEquals(templated.getReason() + " at position " + templated.getPosition() + " of '" + source + "'",
                templated.getMessage());
        // a reference with no place in the query is refused where it stands
        var noSite = assertThrows(JsonPathException.class,
                () -> JsonPathSource.references("${model.key}", JsonPathSource.Form.EVENT_SCRIPT));
        assertEquals("a reference is not allowed here at position 1 of '${model.key}'", noSite.getMessage());
    }

    @Test
    void aPluginsJsonPathArgumentIsChecked() {
        assertDoesNotThrow(() -> helper.validateJsonPaths("f:length($.input.body['a','b'])",
                JsonPathSource.Form.EVENT_SCRIPT));
        assertDoesNotThrow(() -> helper.validateJsonPaths("f:concat(model.a, text(b))",
                JsonPathSource.Form.EVENT_SCRIPT));
        var ex = assertThrows(JsonPathException.class, () -> helper.validateJsonPaths(
                "f:concat(model.a, $.input.body.items.first())", JsonPathSource.Form.EVENT_SCRIPT));
        assertTrue(ex.getMessage().endsWith(" of '$.input.body.items.first()'"), ex.getMessage());
    }

    @Test
    void aUnionAndAQuotedCommaReachThePluginAsOneArgument() {
        // the splitter used to cut a JSONPath argument at any comma outside parentheses
        var data = new MultiLevelMap(Map.of("items", List.of(
                Map.of("tag", "a,b"), Map.of("tag", "c"), Map.of("tag", "a,b")),
                "a", List.of(1, 2), "b", List.of(3)));
        assertEquals(2, helper.getLhsElement("f:length($.items[?(@.tag == 'a,b')])", data));
        assertEquals(2, helper.getLhsElement("f:length($['a','b'])", data));
    }
}
