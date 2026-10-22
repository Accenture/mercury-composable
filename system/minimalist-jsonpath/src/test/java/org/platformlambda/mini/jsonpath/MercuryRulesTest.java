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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mercury's two rules beside the standard, pinned by the cases the Rust engine pins them with (crate event-script,
 * mlm.rs: json_path_is_the_complex_query_escape_hatch, json_path_result_shape_follows_jayway,
 * path_shape_reads_brackets_quotes_and_filters, jsonpath_tolerates_hyphenated_member_names_like_jayway) and by the
 * shared fixture unit-test-jsonpath-1, so both engines answer the same.
 */
class MercuryRulesTest {
    private static final Object NOT_FOUND = new Object();
    private static final String PRICE = "price";
    private static final String DETAILS = "account_details";
    private static final Map<String, Object> SHOP = Map.of("shop", Map.of(
            "items", List.of(Map.of(PRICE, 10), Map.of(PRICE, 25), Map.of(PRICE, 5)),
            "single", List.of("only")));

    private static Object read(String query) {
        return JsonPath.compileLenient(query).read(SHOP, NOT_FOUND);
    }

    @Test
    void aDefinitePathYieldsTheValueAndAnIndefiniteOneAList() {
        assertEquals(25, read("$.shop.items[1].price"));
        assertEquals(List.of(10, 25, 5), read("$.shop.items[*].price"));
        assertEquals(List.of(10, 25), read("$.shop.items[?(@.price > 8)].price"));
        assertSame(NOT_FOUND, read("$.shop.missing"));
    }

    @Test
    void theKindOfPathDecidesNeverTheNumberOfMatches() {
        assertEquals(List.of(25), read("$.shop.items[?(@.price > 20)].price"));
        assertEquals(List.of(), read("$.shop.items[?(@.price > 100)].price"));
        assertEquals(List.of("only"), read("$.shop.single[*]"));
        assertEquals(List.of(10), read("$.shop.items[0:1].price"));
        assertEquals(List.of(10, 25), read("$.shop.items[0,1].price"));
        assertEquals(List.of(10, 25, 5), read("$..price"));
    }

    @Test
    void afterTheFirstIndefiniteStepAMissingMemberOnlyEmptiesTheList() {
        assertEquals(List.of(), read("$.shop.items[*].missing"));
        assertEquals(List.of(), read("$..missing"));
        // a missing index before it is not an error either
        assertEquals(List.of(), read("$.shop.items[9][*]"));
        assertEquals(List.of(), read("$.shop.single[0][*]"));
    }

    @Test
    void aMissingMemberNameBeforeTheFirstIndefiniteStepIsNotFound() {
        assertSame(NOT_FOUND, read("$.shop.missing[*]"));
        assertSame(NOT_FOUND, read("$.missing..price"));
        assertSame(NOT_FOUND, read("$.shop.items[1].missing[*]"));
        // as is a name applied to a value that is not an object
        assertSame(NOT_FOUND, read("$.shop.single.x[*]"));
    }

    @Test
    void definitePathsWithBracketedNamesAndNegativeIndexes() {
        assertEquals(5, read("$['shop']['items'][-1].price"));
        assertSame(NOT_FOUND, read("$.shop.items[7].price"));
        assertTrue(JsonPath.compile("$.a.b[0]").isSingular());
        assertTrue(JsonPath.compile("$['a.b'][\"c]d\"][-2]").isSingular());
        assertFalse(JsonPath.compile("$.a['b','c']").isSingular());
        assertFalse(JsonPath.compile("$..a").isSingular());
    }

    @Test
    void aJsonNullIsAValueNotAnAbsence() {
        Map<String, Object> document = new java.util.HashMap<>();
        document.put("a", null);
        assertNull(JsonPath.compile("$.a").read(document, NOT_FOUND));
        assertSame(NOT_FOUND, JsonPath.compile("$.a.b[*]").read(document, NOT_FOUND));
    }

    @Test
    void theSharedFixtureShapes() {
        // unit-test-jsonpath-1, whose mapping sources read the request body
        var people = List.of(Map.of("name", "Peter", "team", "blue"), Map.of("name", "Paul", "team", "red"),
                Map.of("name", "Mary", "team", "blue"));
        Map<String, Object> state = Map.of("input", Map.of("body", Map.of("people", people)));
        assertEquals("Paul", JsonPath.compileLenient("$.input.body.people[1].name").read(state, NOT_FOUND));
        assertEquals(List.of("Paul"), lenient("$.input.body.people[?(@.team == 'red')].name", state));
        assertEquals(List.of(), lenient("$.input.body.people[?(@.team == 'green')].name", state));
        assertEquals(List.of("Peter", "Paul", "Mary"), lenient("$.input.body.people[*].name", state));
        assertEquals(List.of("Peter"), lenient("$.input.body.people[0:1].name", state));
        assertEquals(List.of("blue", "red", "blue"), lenient("$.input.body..team", state));
        assertEquals(List.of(), lenient("$.input.body.people[*].missing", state));
        assertEquals(List.of(), lenient("$.input.body.people[9].team[*]", state));
        assertSame(NOT_FOUND, lenient("$.input.body.missing[?(@.team == 'red')].name", state));
        assertSame(NOT_FOUND, lenient("$.input.body.people[1].missing", state));
    }

    private static Object lenient(String query, Object document) {
        return JsonPath.compileLenient(query).read(document, NOT_FOUND);
    }

    @Test
    void aHyphenatedMemberNameIsAcceptedLikeTheRustEngine() {
        Map<String, Object> state = Map.of("fetcher-ext", Map.of("result",
                List.of(Map.of(DETAILS, "a"), Map.of(DETAILS, "b"))));
        assertEquals(List.of("a", "b"), lenient("$.fetcher-ext.result[*].account_details", state));
        assertEquals(List.of(List.of(Map.of(DETAILS, "a"), Map.of(DETAILS, "b"))),
                lenient("$..fetcher-ext.result", state));
        // RFC 9535 itself refuses the shorthand, which is what the compliance suite checks
        assertThrows(JsonPathException.class, () -> JsonPath.compile("$.fetcher-ext.result"));
    }

    @Test
    void theRewriteIsTheRustOne() {
        assertEquals("$['fetcher-ext'].result", HyphenNames.bracketize("$.fetcher-ext.result"));
        assertEquals("$..['node-a'].b", HyphenNames.bracketize("$..node-a.b"));
        assertEquals("$.a.b[0]", HyphenNames.bracketize("$.a.b[0]"));
        // nothing inside brackets or quotes is touched
        assertEquals("$['x.y-z'][?(@.a-b)]", HyphenNames.bracketize("$['x.y-z'][?(@.a-b)]"));
        assertEquals("$[\"q.r-s\"]", HyphenNames.bracketize("$[\"q.r-s\"]"));
        // a wildcard and an empty name pass through
        assertEquals("$.*", HyphenNames.bracketize("$.*"));
        assertEquals("$..*", HyphenNames.bracketize("$..*"));
        // the Rust rewrite counts letters and numbers of every script, and stops at anything else
        assertEquals("$['é-٣'].x", HyphenNames.bracketize("$.é-٣.x"));
        assertEquals("$['a-'] ", HyphenNames.bracketize("$.a- "));
        assertEquals("$]['a-b']", HyphenNames.bracketize("$].a-b"));
    }

    @Test
    void aQueryTheStandardAcceptsIsNeverRewritten() {
        JsonPath path = JsonPath.compileLenient("$['a-b']");
        assertEquals("$['a-b']", path.toString());
        assertEquals(List.of(1), path.select(Map.of("a-b", 1)));
    }

    @Test
    void aRefusedRewriteReportsTheStrictError() {
        JsonPathException e = assertThrows(JsonPathException.class, () -> JsonPath.compileLenient("$.a-b["));
        JsonPathException strict = assertThrows(JsonPathException.class, () -> JsonPath.compile("$.a-b["));
        assertEquals(strict.getMessage(), e.getMessage());
        // and a query with no hyphenated name reports the strict error unchanged
        JsonPathException plain = assertThrows(JsonPathException.class, () -> JsonPath.compileLenient("$.a["));
        assertEquals(4, plain.getPosition());
        // the lenient query keeps its own text
        assertEquals("$.node-b", JsonPath.compileLenient("$.node-b").toString());
    }
}
