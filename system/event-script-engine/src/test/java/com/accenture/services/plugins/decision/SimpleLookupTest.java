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

package com.accenture.services.plugins.decision;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleLookupTest {

    private final SimpleLookup plugin = new SimpleLookup();

    private static final Map<String, Object> TABLE_OF_LISTS = Map.of(
            "keys", List.of("community-property", "separate-property"),
            "community-property", List.of("CA", "TX"),
            "separate-property", List.of("NY"));

    // the shape of a node authored as keys=[ "community-property", "separate-property" ] etc.:
    // every value is a JSON array written as text
    private static final Map<String, Object> TABLE_OF_JSON_TEXT = Map.of(
            "purpose", "a property the lookup ignores",
            "keys", "[ \"community-property\", \"separate-property\" ]",
            "community-property", "[ \"CA\", \"TX\" ]",
            "separate-property", "[ \"NY\" ]");

    @Test
    void shouldUseLookupAsPluginName() {
        assertEquals("lookup", plugin.getName());
    }

    @Test
    void shouldResolveTheRuleThatListsTheValue() {
        assertEquals("community-property", plugin.calculate(TABLE_OF_LISTS, "TX"));
        assertEquals("separate-property", plugin.calculate(TABLE_OF_LISTS, "NY"));
        // f:lookup(state-rules, input.body.state) with the node's JSON-text values
        assertEquals("community-property", plugin.calculate(TABLE_OF_JSON_TEXT, "CA"));
        assertEquals("separate-property", plugin.calculate(TABLE_OF_JSON_TEXT, "NY"));
    }

    @Test
    void shouldAcceptTheWholeTableAsJsonText() {
        var table = "{\"keys\": [\"low\", \"high\"], \"low\": [1, 2, 3], \"high\": [4, 5]}";
        assertEquals("low", plugin.calculate(table, 2));
        assertEquals("high", plugin.calculate(table, "5"));
    }

    @Test
    void shouldCompareValuesAsTextIgnoringCase() {
        assertEquals("community-property", plugin.calculate(TABLE_OF_LISTS, "tx"));
        var numeric = Map.of("keys", List.of("odd", "even"), "odd", List.of(1, 3), "even", List.of(2, 4));
        assertEquals("even", plugin.calculate(numeric, 4));
        assertEquals("odd", plugin.calculate(numeric, "3"));
    }

    @Test
    void shouldReturnNullWhenNoRuleListsTheValue() {
        // a miss is null so that a mapping can supply a default with f:defaultValue
        assertNull(plugin.calculate(TABLE_OF_LISTS, "ZZ"));
        assertNull(plugin.calculate(TABLE_OF_JSON_TEXT, null));
    }

    @Test
    void shouldRejectInvalidInput() {
        var oneArgument = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(TABLE_OF_LISTS));
        assertEquals("Expected two input values - actual=1", oneArgument.getMessage());
        var notATable = assertThrows(IllegalArgumentException.class, () -> plugin.calculate("state-rules", "TX"));
        assertEquals("First argument must be a decision table as a map or JSON text", notATable.getMessage());
        var missingTable = assertThrows(IllegalArgumentException.class, () -> plugin.calculate(null, "TX"));
        assertEquals("First argument must be a decision table as a map or JSON text", missingTable.getMessage());
        var noKeys = assertThrows(IllegalArgumentException.class,
                () -> plugin.calculate(Map.of("community-property", List.of("CA")), "CA"));
        assertEquals("Missing keys in input", noKeys.getMessage());
        var missingRule = assertThrows(IllegalArgumentException.class,
                () -> plugin.calculate(Map.of("keys", List.of("community-property")), "CA"));
        assertEquals("Missing key in input: community-property", missingRule.getMessage());
        var notAList = assertThrows(IllegalArgumentException.class,
                () -> plugin.calculate(Map.of("keys", "community-property", "community-property", "CA"), "CA"));
        assertEquals("Input is not a list of values", notAList.getMessage());
    }
}
