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

package com.accenture.services.plugins.types;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseJsonTest {

    private final ParseJson plugin = new ParseJson();

    @Test
    void shouldUseJsonAsPluginName() {
        assertEquals("json", plugin.getName());
    }

    @Test
    void shouldParseEmptyListAndEmptyMap() {
        // the headline use case: 'f:json(text([])) -> my_empty_list' creates an empty list
        Object emptyList = plugin.calculate("[]");
        assertInstanceOf(List.class, emptyList);
        assertTrue(((List<?>) emptyList).isEmpty());
        Object emptyMap = plugin.calculate("{}");
        assertInstanceOf(Map.class, emptyMap);
        assertTrue(((Map<?, ?>) emptyMap).isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldParseNestedJsonObject() {
        // 'f:json(text({"hello": [1, 2, {"nested": "demo"}]})) -> my_nested_dataset'
        Object result = plugin.calculate("{\"hello\": [1, 2, {\"nested\": \"demo\"}]}");
        assertInstanceOf(Map.class, result);
        Map<String, Object> map = (Map<String, Object>) result;
        assertInstanceOf(List.class, map.get("hello"));
        List<Object> hello = (List<Object>) map.get("hello");
        assertEquals(3, hello.size());
        // whole numbers are parsed as Long, decimals as Double (ToNumberPolicy.LONG_OR_DOUBLE)
        assertEquals(1L, hello.get(0));
        assertEquals(2L, hello.get(1));
        assertEquals(Map.of("nested", "demo"), hello.get(2));
    }

    @Test
    void shouldParseJsonArrayWithMixedNumbers() {
        Object result = plugin.calculate("[10, 2.5, \"x\", true, null]");
        assertInstanceOf(List.class, result);
        List<?> list = (List<?>) result;
        assertEquals(10L, list.get(0));
        assertEquals(2.5d, list.get(1));
        assertEquals("x", list.get(2));
        assertEquals(true, list.get(3));
        assertNull(list.get(4));
    }

    @Test
    void shouldAcceptByteArrayAndPaddedInput() {
        Object fromBytes = plugin.calculate((Object) Utility.getInstance().getUTF("{\"a\": 1}"));
        assertEquals(Map.of("a", 1L), fromBytes);
        // leading/trailing whitespace is trimmed before evaluation
        Object padded = plugin.calculate("  [\"y\"]  ");
        assertEquals(List.of("y"), padded);
    }

    @Test
    void shouldReturnEmptyMapForBlankInput() {
        // lenient by design: blank input yields an empty map instead of aborting the flow
        Object empty = plugin.calculate("");
        assertInstanceOf(Map.class, empty);
        assertTrue(((Map<?, ?>) empty).isEmpty());
        Object blank = plugin.calculate("   ");
        assertInstanceOf(Map.class, blank);
        assertTrue(((Map<?, ?>) blank).isEmpty());
    }

    @Test
    void shouldRejectNonJsonInput() {
        // scalars are not accepted - only JSON object {...} or array [...] forms
        var ex1 = assertThrows(IllegalArgumentException.class, () -> plugin.calculate("42"));
        assertEquals("Input is not JSON: 42", ex1.getMessage());
        assertThrows(IllegalArgumentException.class, () -> plugin.calculate("hello"));
        // mismatched outer brackets
        assertThrows(IllegalArgumentException.class, () -> plugin.calculate("{\"a\": 1]"));
    }

    @Test
    void shouldRejectInvalidInputTypeOrArgumentCount() {
        String expected = "Input must be a JSON in string or byte array";
        // null (an absent model variable, or a nested plugin) must raise the clear error, not NPE
        var ex1 = assertThrows(IllegalArgumentException.class, () -> plugin.calculate((Object) null));
        assertEquals(expected, ex1.getMessage());
        Object mapInput = Map.of("k", "v");
        var ex2 = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(mapInput));
        assertEquals(expected, ex2.getMessage());
        var ex3 = assertThrows(IllegalArgumentException.class, plugin::calculate);
        assertEquals(expected, ex3.getMessage());
        var ex4 = assertThrows(IllegalArgumentException.class, () -> plugin.calculate("[]", "{}"));
        assertEquals(expected, ex4.getMessage());
    }

    @Test
    void shouldParseLenientJsonVariants() {
        // the engine's JSON parser is lenient: unquoted keys are accepted
        assertEquals(Map.of("bad", 1L), plugin.calculate("{bad: 1}"));
        // and a trailing comma silently appends a null element - a known footgun,
        // pinned here so a parser upgrade that changes it is caught
        Object result = plugin.calculate("[1, 2,]");
        assertInstanceOf(List.class, result);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals(2L, list.get(1));
        assertNull(list.get(2));
    }

    @Test
    void shouldNormalizeParserErrorForMalformedJson() {
        // malformed JSON that passes the outer-bracket check must surface as the plugin's
        // own IllegalArgumentException (HTTP 400 at flow level), never a raw parser exception
        var ex = assertThrows(IllegalArgumentException.class, () -> plugin.calculate("{not valid json}"));
        assertTrue(ex.getMessage().startsWith("Unable to parse JSON: "), ex.getMessage());
        // single line - no multi-line parser troubleshooting text
        assertFalse(ex.getMessage().contains("\n"));
    }
}
