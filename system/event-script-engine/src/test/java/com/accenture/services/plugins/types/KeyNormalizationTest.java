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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * f:camelCase / f:snakeCase - legacy systems (often XML-to-JSON transformations)
 * deliver maps whose key formats vary per source; both plugins converge them.
 * Segmentation: underscore/hyphen/dot separators, lower-or-digit to upper
 * transitions, and the acronym rule (an upper-case run followed by a lower-case
 * letter splits before its last upper: XMLKey -> XML + Key). The Rust engine
 * pins the identical algorithm and error messages (portable-flow contract).
 */
class KeyNormalizationTest {

    private final CamelCaseNormalization camel = new CamelCaseNormalization();
    private final SnakeCaseNormalization snake = new SnakeCaseNormalization();

    @Test
    void legacyVariantsConvergeToOneKey() {
        // the field's observed variance - all three forms are the same logical key
        for (String variant : List.of("MyExampleKey", "My_Example_key", "my_example_Key")) {
            assertEquals(Map.of("myExampleKey", 1), camel.calculate(Map.of(variant, 1)));
            assertEquals(Map.of("my_example_key", 1), snake.calculate(Map.of(variant, 1)));
        }
    }

    @Test
    void acronymRunSplitsBeforeItsLastUpperCaseLetter() {
        assertEquals(Map.of("myXmlKey", 1), camel.calculate(Map.of("myXMLKey", 1)));
        assertEquals(Map.of("my_xml_key", 1), snake.calculate(Map.of("myXMLKey", 1)));
        assertEquals(Map.of("xmlHttpRequest", 1), camel.calculate(Map.of("XMLHttpRequest", 1)));
        assertEquals(Map.of("customer_id", 1), snake.calculate(Map.of("customerID", 1)));
    }

    @Test
    void hyphenAndDotAreSeparators() {
        assertEquals(Map.of("myExampleKey", 1), camel.calculate(Map.of("my-example.key", 1)));
        assertEquals(Map.of("my_example_key", 1), snake.calculate(Map.of("My-Example.Key", 1)));
    }

    @Test
    void digitsRideWithTheirSegment() {
        assertEquals(Map.of("address1Line", 1), camel.calculate(Map.of("address1Line", 1)));
        assertEquals(Map.of("address1_line", 1), snake.calculate(Map.of("Address1_Line", 1)));
        assertEquals(Map.of("key2Value", 1), camel.calculate(Map.of("key2_value", 1)));
    }

    @Test
    void normalizationRecursesThroughNestedMapsAndLists() {
        Map<String, Object> payload = Map.of("Outer_Key",
                Map.of("Inner_List", List.of(Map.of("Item_Name", "Some_Value"), "scalar")));
        Object normalized = camel.calculate(payload);
        // values are never touched - only keys; scalars inside lists pass through
        assertEquals(Map.of("outerKey",
                Map.of("innerList", List.of(Map.of("itemName", "Some_Value"), "scalar"))), normalized);
    }

    @Test
    void topLevelListOfMapsIsNormalized() {
        Object normalized = snake.calculate(List.of(Map.of("MyKey", 1), Map.of("OtherKey", 2)));
        assertEquals(List.of(Map.of("my_key", 1), Map.of("other_key", 2)), normalized);
    }

    @Test
    void collidingKeysResolveToTheLaterEntry() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("MyKey", 1);
        payload.put("my_key", 2);
        assertEquals(Map.of("myKey", 2), camel.calculate(payload));
    }

    @Test
    void normalizationIsIdempotent() {
        Map<String, Object> messy = Map.of("My_Example_Key", Map.of("customerID", 1));
        Object once = camel.calculate(messy);
        assertEquals(once, camel.calculate(once));
        Object snakeOnce = snake.calculate(messy);
        assertEquals(snakeOnce, snake.calculate(snakeOnce));
    }

    @Test
    void keyWithoutSegmentsIsKeptAsIs() {
        assertEquals(Map.of("___", 1), camel.calculate(Map.of("___", 1)));
        assertEquals(Map.of("-.-", 1), snake.calculate(Map.of("-.-", 1)));
    }

    @Test
    void invalidInputIsRejectedWithThePluginName() {
        var noInput = assertThrows(IllegalArgumentException.class, () -> camel.calculate());
        assertEquals("One input is required for camelCase key normalization", noInput.getMessage());
        var nullInput = assertThrows(IllegalArgumentException.class, () -> snake.calculate((Object) null));
        assertEquals("Input cannot be null for snakeCase key normalization", nullInput.getMessage());
        var scalar = assertThrows(IllegalArgumentException.class, () -> camel.calculate("MyKey"));
        assertEquals("Input must be a map or a list for camelCase key normalization", scalar.getMessage());
    }
}
