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

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the compliance suite does not reach: the refusals' messages and positions, the nesting limit, the number
 * types of a Java tree, non-JSON leaves, normalized path escapes and documents too deep for a recursive walk.
 */
class EngineTest {
    private static final String VALUES = "values";

    private static JsonPathException refusal(String query) {
        return assertThrows(JsonPathException.class, () -> JsonPath.compile(query));
    }

    @Test
    void aRefusalNamesTheReasonAndThePosition() {
        JsonPathException e = refusal("$[?foo(@)]");
        assertEquals("unknown function foo() at position 3 of '$[?foo(@)]'", e.getMessage());
        assertEquals(3, e.getPosition());
        assertTrue(refusal("$[?@.a[*] == 1]").getMessage().startsWith("a comparison takes a singular query"));
        assertTrue(refusal("$[?1]").getMessage().startsWith("expected a comparison operator after a literal"));
        assertTrue(refusal("$[?length(@.a)]").getMessage().startsWith("length() yields a value"));
        assertTrue(refusal("$[?match(@.a, 'x') == true]").getMessage().startsWith("match() is a test"));
        assertTrue(refusal("$[?count(@.a, 1) == 1]").getMessage().startsWith("count() takes 1 argument, not 2"));
        assertTrue(refusal("$[?match(@.a) == 1]").getMessage().startsWith("match() takes 2 arguments, not 1"));
        assertTrue(refusal("$[?count(1) == 1]").getMessage().startsWith("count() takes a query"));
        assertTrue(refusal("$[?length(@.*) == 1]").getMessage().startsWith("length() takes a value"));
        assertTrue(refusal("$[?!1]").getMessage().startsWith("expected a query or a function"));
        assertTrue(refusal("$.a b").getMessage().startsWith("unexpected ' '"));
        assertTrue(refusal("$\u0001").getMessage().startsWith("unexpected U+0001"));
        assertTrue(refusal("x").getMessage().startsWith("expected a query that begins with '$'"));
        assertThrows(JsonPathException.class, () -> JsonPath.compile(null));
    }

    @Test
    void literalsAndIndexesAreRefusedOutsideTheGrammar() {
        assertTrue(refusal("$[?@.a == 1e999999999999]").getMessage().startsWith("the number is out of range"));
        assertTrue(refusal("$[?@.a == 01]").getMessage().startsWith("a number has no leading zero"));
        assertTrue(refusal("$[?@.a == 1.]").getMessage().startsWith("expected a digit after the decimal point"));
        assertTrue(refusal("$[?@.a == 1e]").getMessage().startsWith("expected a digit in the exponent"));
        assertTrue(refusal("$[?@.a == -]").getMessage().startsWith("expected a digit"));
        assertTrue(refusal("$[9007199254740992]").getMessage().startsWith("an index is out of the range"));
        assertTrue(refusal("$[12345678901234567]").getMessage().startsWith("an index is out of the range"));
        assertTrue(refusal("$[-0]").getMessage().startsWith("an index is 0 or an integer"));
        assertTrue(refusal("$['a\\x']").getMessage().startsWith("invalid escape in a string"));
        assertTrue(refusal("$['a").getMessage().startsWith("the string is not closed"));
        assertTrue(refusal("$['\\u12']").getMessage().startsWith("expected four hexadecimal digits"));
        assertTrue(refusal("$['\\uDC00']").getMessage().startsWith("a low surrogate must follow"));
        assertTrue(refusal("$['\\uD800x']").getMessage().startsWith("a high surrogate must be followed"));
        assertTrue(refusal("$['\\uD800\\u0041']").getMessage().startsWith("a high surrogate must be followed"));
        assertTrue(refusal("$['\uD800']").getMessage().startsWith("a string cannot hold"));
        assertEquals(List.of(9007199254740991L, -9007199254740991L), List.of(
                ((Selector.Index) indexOf("$[9007199254740991]")).index(),
                ((Selector.Index) indexOf("$[-9007199254740991]")).index()));
    }

    private static Selector indexOf(String query) {
        return new Parser(query).parseQuery().segments().getFirst().selectors().getFirst();
    }

    @Test
    void nestingDeeperThanTheLimitIsRefused() {
        String deep = "$[?" + "(".repeat(Parser.MAX_DEPTH) + "@.a" + ")".repeat(Parser.MAX_DEPTH) + "]";
        assertTrue(refusal(deep).getMessage().startsWith("the query nests deeper than 64 levels"));
        String allowed = "$[?" + "(".repeat(Parser.MAX_DEPTH - 2) + "@.a" + ")".repeat(Parser.MAX_DEPTH - 2) + "]";
        assertEquals(List.of(Map.of("a", 1)), JsonPath.compile(allowed).select(List.of(Map.of("a", 1), 2)));
        String functions = "$[?" + "length(value(".repeat(40) + "@" + "))".repeat(40) + " == 1]";
        assertTrue(refusal(functions).getMessage().startsWith("the query nests deeper than 64 levels"));
    }

    @Test
    void numbersCompareByValueWhateverTheirJavaType() {
        List<Object> values = List.of(1, 1L, (short) 1, (byte) 1, 1.0, 1.0f, BigDecimal.ONE, BigInteger.ONE,
                new AtomicLong(1), 2, Double.NaN, Double.POSITIVE_INFINITY, Float.NaN, "1");
        Map<String, Object> document = Map.of(VALUES, values);
        List<Object> ones = JsonPath.compile("$.values[?@ == 1]").select(document);
        assertEquals(9, ones.size());
        assertFalse(ones.contains("1"));
        assertEquals(List.of(2), JsonPath.compile("$.values[?@ > 1]").select(document));
        assertEquals(List.of(), JsonPath.compile("$.values[?@ < 0.5]").select(document));
        // a double converts through its shortest decimal text
        assertEquals(List.of(0.1), JsonPath.compile("$[?@ == 0.1]").select(List.of(0.1, 0.2)));
        // a Number whose text is not a decimal neither equals nor orders
        assertEquals(List.of(), JsonPath.compile("$[?@ == 1]").select(List.of(new Odd())));
    }

    /** A Number that is not one of the JDK's numeric classes and whose text is not a decimal. */
    private static final class Odd extends Number {
        @Override
        public int intValue() {
            return 1;
        }

        @Override
        public long longValue() {
            return 1;
        }

        @Override
        public float floatValue() {
            return 1;
        }

        @Override
        public double doubleValue() {
            return 1;
        }

        @Override
        public String toString() {
            return "one";
        }
    }

    @Test
    void structuresCompareMemberByMember() {
        Map<String, Object> document = Map.of("a", List.of(1, Map.of("x", 2)), "b", List.of(1, Map.of("x", 2)),
                "c", List.of(1, Map.of("y", 2)), "d", List.of(1), "e", Map.of("x", 2, "y", 3), "f", Map.of("x", 2));
        assertEquals(List.of(document), JsonPath.compile("$[?@.a == @.b]").select(List.of(document)));
        assertEquals(List.of(), JsonPath.compile("$[?@.a == @.c]").select(List.of(document)));
        assertEquals(List.of(), JsonPath.compile("$[?@.a == @.d]").select(List.of(document)));
        assertEquals(List.of(), JsonPath.compile("$[?@.e == @.f]").select(List.of(document)));
        assertEquals(List.of(), JsonPath.compile("$[?@.a < @.b]").select(List.of(document)));
        assertEquals(List.of(document), JsonPath.compile("$[?@.a <= @.b]").select(List.of(document)));
    }

    @Test
    void stringsOrderByCodePoint() {
        // U+FF61 is below U+1F600 by code point, above it by UTF-16 unit
        assertEquals(List.of("｡"), JsonPath.compile("$[?@ < '😀']").select(List.of("｡")));
        assertEquals(List.of("ab"), JsonPath.compile("$[?@ > 'a']").select(List.of("a", "ab")));
        assertEquals(List.of("a"), JsonPath.compile("$[?@ < 'ab']").select(List.of("a", "ab")));
    }

    @Test
    void aLeafOfAnotherTypeIsSelectedButNeverDescendedInto() {
        byte[] bytes = {1, 2};
        Map<Object, Object> document = new HashMap<>();
        document.put("bytes", bytes);
        document.put(7, "seven");
        assertSame(bytes, JsonPath.compile("$.bytes").select(document).getFirst());
        assertEquals(List.of("$['7']"), JsonPath.compile("$[?@ == 'seven']").selectPaths(document));
        assertEquals(List.of(), JsonPath.compile("$.bytes[*]").select(document));
        assertEquals(List.of(document), JsonPath.compile("$[?@.bytes == @.bytes]").select(List.of(document)));
    }

    @Test
    void normalizedPathsEscapeWhatTheStandardEscapes() {
        Map<String, Object> document = new HashMap<>();
        document.put("a'b\\c\b\f\n\r\t\u0001\u001Fé", 1);
        assertEquals(List.of("$['a\\'b\\\\c\\b\\f\\n\\r\\t\\u0001\\u001fé']"),
                JsonPath.compile("$.*").selectPaths(document));
        assertEquals(List.of("$[0][1]"), JsonPath.compile("$[0][1]").selectPaths(List.of(List.of(0, 1))));
        assertEquals(List.of("$"), JsonPath.compile("$").selectPaths(1));
    }

    @Test
    void aDeepDocumentIsWalkedWithoutRecursion() {
        Object deep = 1;
        for (int i = 0; i < 100_000; i++) {
            deep = List.of(deep);
        }
        assertEquals(100_001, JsonPath.compile("$..*").select(List.of(deep)).size());
        assertEquals(List.of(1), JsonPath.compile("$..[?@ == 1]").select(deep));
    }

    @Test
    void functionsTakeTheirArgumentsByType() {
        Map<String, Object> document = Map.of(VALUES, List.of("abc", List.of(1, 2), Map.of("k", 1), 3, true));
        assertEquals(List.of("abc"), JsonPath.compile("$.values[?length(@) == 3]").select(document));
        assertEquals(List.of(List.of(1, 2)), JsonPath.compile("$.values[?count(@.*) == 2]").select(document));
        assertEquals(List.of(Map.of("k", 1)), JsonPath.compile("$.values[?value(@.k) == 1]").select(document));
        assertEquals(List.of(), JsonPath.compile("$.values[?value(@.*) == 2]").select(document));
        assertEquals(List.of(), JsonPath.compile("$.values[?length(true) == 1]").select(document));
        // a literal pattern that is not a string, or not an I-Regexp, never matches
        assertEquals(List.of(), JsonPath.compile("$.values[?match(@, 1)]").select(document));
        assertEquals(List.of(), JsonPath.compile("$.values[?match(@, '[')]").select(document));
        assertEquals(List.of("abc"), JsonPath.compile("$.values[?search(@, 'b') && !match(@, 'b')]").select(document));
        assertEquals(List.of("abc"), JsonPath.compile("$.values[?search(@, 'b') || match(@, 'x')]").select(document));
        // a function argument may itself be a logical expression, which a value parameter refuses
        assertTrue(refusal("$[?length(@.a == 1) == 1]").getMessage().startsWith("length() takes a value"));
        assertTrue(refusal("$[?length(!@.a) == 1]").getMessage().startsWith("length() takes a value"));
        // blank space where the grammar allows it
        assertEquals(List.of("abc"), JsonPath.compile("$.values[? length( @ ) == 3 ]").select(document));
        assertEquals(List.of("abc"), JsonPath.compile("$ .values [ ?match(@ , 'a.c') ]").select(document));
    }

    @Test
    void aCompiledQueryIsSharedAcrossThreads() {
        JsonPath path = JsonPath.compile("$.items[?@.n > 2].n");
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(Map.of("n", i));
        }
        Map<String, Object> document = Map.of("items", items);
        long mismatches = IntStream.range(0, 2_000).parallel()
                .filter(i -> path.select(document).size() != 97).count();
        assertEquals(0, mismatches);
    }

    @Test
    void aTestFunctionAskedForAValueYieldsItsLogicalResult() {
        // the parser never asks: it refuses match() and search() where a value is expected
        Node root = Node.root(List.of());
        var literal = new Operand.Literal("a.c");
        assertEquals(Boolean.TRUE, new FunctionCall(JsonFunction.MATCH, List.of(new Operand.Literal("abc"), literal))
                .value(root, root));
        assertEquals(JsonFunction.Type.LOGICAL, new FunctionCall(JsonFunction.SEARCH,
                List.of(literal, literal)).type());
    }

    @Test
    void theNothingValueNamesItself() {
        assertEquals("Nothing", Values.NOTHING.toString());
        assertEquals(Values.NOTHING, Selector.Index.elementAt("not a list", 0));
        assertEquals(Values.NOTHING, Selector.Index.elementAt(List.of(1), -2));
        assertEquals(1, Selector.Index.elementAt(List.of(1), -1));
    }
}
