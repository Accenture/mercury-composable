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

package org.platformlambda.mini.kafka;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the second-level routing rule grammar: the three matcher modes (exact, wildcard,
 * explicit regex), first-match-wins ordering, case-insensitive header-name lookup, composite body
 * paths, the non-match-never-errors contract, and the compile-time rejections.
 */
class RoutingRuleSetTest {

    /** A byte[] body: every {@code input.body} rule must be a non-match against it. */
    private static final byte[] RAW = "raw-bytes".getBytes(StandardCharsets.UTF_8);
    private static final String CATCH_ALL = "default -> flow://catch-all";

    private static RoutingRuleSet.Target flow(String flowId) {
        return new RoutingRuleSet.Target(false, flowId);
    }

    private static RoutingRuleSet.Target task(String route) {
        return new RoutingRuleSet.Target(true, route);
    }

    private static RoutingRuleSet compile(String... rules) {
        return RoutingRuleSet.compile(List.of(rules));
    }

    @Test
    void firstMatchWinsInDeclarationOrder() {
        RoutingRuleSet rules = compile(
                "input.header.type(order-1) -> flow://specific-flow",
                "input.header.type(order-*) -> flow://wildcard-flow",
                CATCH_ALL);
        // both rules match 'order-1'; the first declared wins
        assertEquals(flow("specific-flow"), rules.select(Map.of("type", "order-1"), RAW));
        assertEquals(flow("wildcard-flow"), rules.select(Map.of("type", "order-2"), RAW));
        assertEquals(flow("catch-all"), rules.select(Map.of("type", "invoice"), RAW));
    }

    @Test
    void headerNameLookupIsCaseInsensitiveButValueStaysCaseSensitive() {
        RoutingRuleSet rules = compile("input.header.type(order) -> flow://order-flow", CATCH_ALL);
        // Kafka preserves the producer's wire casing of the header NAME - the rule must not depend on it
        assertEquals(flow("order-flow"), rules.select(Map.of("Type", "order"), RAW));
        assertEquals(flow("order-flow"), rules.select(Map.of("TYPE", "order"), RAW));
        // the VALUE comparison is exact-mode case-sensitive
        assertEquals(flow("catch-all"), rules.select(Map.of("type", "Order"), RAW));
    }

    @Test
    void wildcardIsAnchoredFullMatch() {
        RoutingRuleSet rules = compile("input.header.type(order-*) -> flow://wildcard-flow", CATCH_ALL);
        assertEquals(flow("wildcard-flow"), rules.select(Map.of("type", "order-42"), RAW));
        // '*' matches an empty run too
        assertEquals(flow("wildcard-flow"), rules.select(Map.of("type", "order-"), RAW));
        // anchored full match: a prefix before the literal segment does not match
        assertEquals(flow("catch-all"), rules.select(Map.of("type", "bulk-order-42"), RAW));
    }

    @Test
    void wildcardTreatsRegexMetacharactersAsLiterals() {
        RoutingRuleSet rules = compile("input.header.path(a.b*) -> flow://dotted-flow", CATCH_ALL);
        assertEquals(flow("dotted-flow"), rules.select(Map.of("path", "a.b.c"), RAW));
        // the '.' in the matcher is a literal dot, not regex "any character"
        assertEquals(flow("catch-all"), rules.select(Map.of("path", "aXb.c"), RAW));
    }

    @Test
    void regexModeIsExplicitAndFullMatch() {
        RoutingRuleSet rules = compile(
                "input.header.type(regex: shipment-(eu|us)) -> flow://shipment-flow", CATCH_ALL);
        assertEquals(flow("shipment-flow"), rules.select(Map.of("type", "shipment-eu"), RAW));
        assertEquals(flow("shipment-flow"), rules.select(Map.of("type", "shipment-us"), RAW));
        // full-match semantics (the topic-pattern precedent): a substring occurrence does not match
        assertEquals(flow("catch-all"), rules.select(Map.of("type", "my-shipment-eu-1"), RAW));
    }

    @Test
    void bodyRuleMatchesCompositePathOnMapBody() {
        RoutingRuleSet rules = compile(
                "input.body.event.kind(refund) -> task://v1.refund.processor", CATCH_ALL);
        assertEquals(task("v1.refund.processor"),
                rules.select(Map.of(), Map.of("event", Map.of("kind", "refund"))));
        assertEquals(flow("catch-all"),
                rules.select(Map.of(), Map.of("event", Map.of("kind", "purchase"))));
    }

    @Test
    void bodyRuleNeverMatchesBytesOrNullOrAMismatchedShape() {
        RoutingRuleSet rules = compile(
                "input.body.event.kind(refund) -> task://v1.refund.processor", CATCH_ALL);
        assertEquals(flow("catch-all"), rules.select(Map.of(), RAW));   // raw byte[]
        assertEquals(flow("catch-all"), rules.select(Map.of(), null));
        // a List body is addressable (see the bracket-path test) but never by a Map-shaped path
        assertEquals(flow("catch-all"), rules.select(Map.of(), List.of("refund")));
    }

    @Test
    void bodyRulesAddressListBodiesWithBracketPaths() {
        RoutingRuleSet rules = compile(
                "input.body[0].type(order) -> flow://order-processing",
                "input.body.items[1].kind(refund) -> task://v1.refund.processor",
                CATCH_ALL);
        // a TOP-LEVEL JSON array body: the first element's type drives the routing
        assertEquals(flow("order-processing"),
                rules.select(Map.of(), List.of(Map.of("type", "order"))));
        // a nested list inside a Map body
        assertEquals(task("v1.refund.processor"), rules.select(Map.of(),
                Map.of("items", List.of(Map.of("kind", "sale"), Map.of("kind", "refund")))));
        // an out-of-range index is a non-match, never an error
        assertEquals(flow("catch-all"), rules.select(Map.of(), List.of()));
    }

    @Test
    void dollarPrefixedBodyKeysAreLiteralSegmentsNeverJsonPath() {
        // the body is evaluated under a synthetic root, so MultiLevelMap's '$'-JsonPath escape hatch
        // (whose parser can throw at record time) is structurally unreachable - a '$' prefixed key is
        // an ordinary literal segment, and a JsonPath-looking key is a plain non-match, never an error
        RoutingRuleSet literal = compile("input.body.$kind(refund) -> flow://refund-flow", CATCH_ALL);
        assertEquals(flow("refund-flow"), literal.select(Map.of(), Map.of("$kind", "refund")));
        RoutingRuleSet jsonPathLike = compile("input.body.$[(refund) -> flow://never-selected", CATCH_ALL);
        assertEquals(flow("catch-all"), jsonPathLike.select(Map.of(), Map.of("kind", "refund")));
    }

    @Test
    void nonStringValueIsANonMatch() {
        RoutingRuleSet rules = compile("input.body.priority(1) -> flow://urgent-flow", CATCH_ALL);
        // a numeric 1 is a non-match by design; only the String "1" matches
        assertEquals(flow("catch-all"), rules.select(Map.of(), Map.of("priority", 1)));
        assertEquals(flow("urgent-flow"), rules.select(Map.of(), Map.of("priority", "1")));
    }

    @Test
    void missingHeaderOrKeyIsANonMatchNotAnError() {
        RoutingRuleSet rules = compile(
                "input.header.type(order) -> flow://order-flow",
                "input.body.kind(refund) -> flow://refund-flow",
                CATCH_ALL);
        assertEquals(flow("catch-all"), rules.select(Map.of(), Map.of()));
    }

    @Test
    void defaultMayTargetATask() {
        RoutingRuleSet rules = compile("default -> task://raw.handler");
        assertEquals(task("raw.handler"), rules.select(Map.of("any", "thing"), RAW));
        assertEquals(0, rules.size());
    }

    @Test
    void allTargetsIncludesEveryRuleAndTheDefault() {
        RoutingRuleSet rules = compile(
                "input.header.type(order) -> flow://order-flow",
                "input.body.kind(refund) -> task://v1.refund.processor",
                CATCH_ALL);
        assertEquals(2, rules.size());
        assertEquals(List.of(flow("order-flow"), task("v1.refund.processor"), flow("catch-all")),
                rules.allTargets());
    }

    @Test
    void targetLabelNamesTheTargetKind() {
        assertEquals("flow 'order-flow'", flow("order-flow").label());
        assertEquals("task 'v1.refund.processor'", task("v1.refund.processor").label());
    }

    @Test
    void rejectsRuleWithoutArrow() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.type(order) flow://order-flow", CATCH_ALL));
    }

    @Test
    void rejectsSelectorWithoutParentheses() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.type -> flow://order-flow", CATCH_ALL));
    }

    @Test
    void rejectsEmptyMatcher() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.type() -> flow://order-flow", CATCH_ALL));
    }

    @Test
    void rejectsMissingHeaderName() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.(order) -> flow://order-flow", CATCH_ALL));
    }

    @Test
    void rejectsUnknownSelectorNamespace() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.metadata.topic(orders) -> flow://order-flow", CATCH_ALL));
    }

    @Test
    void rejectsUnknownTargetScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.type(order) -> route://order-flow", CATCH_ALL));
        assertThrows(IllegalArgumentException.class, () -> compile("default -> catch-all"));
    }

    @Test
    void rejectsEmptyTargetDestination() {
        assertThrows(IllegalArgumentException.class, () -> compile("default -> flow://"));
        assertThrows(IllegalArgumentException.class, () -> compile("default -> task://"));
    }

    @Test
    void rejectsDuplicateDefault() {
        assertThrows(IllegalArgumentException.class,
                () -> compile(CATCH_ALL, "default -> flow://another"));
    }

    @Test
    void requiresADefaultRule() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.type(order) -> flow://order-flow"));
    }

    @Test
    void wildcardMatchesAcrossLineBreaks() {
        // '*' matches ANY run of characters - including newlines inside a free-text body value (DOTALL)
        RoutingRuleSet rules = compile("input.body.note(urgent*) -> flow://urgent-flow", CATCH_ALL);
        assertEquals(flow("urgent-flow"),
                rules.select(Map.of(), Map.of("note", "urgent\nsecond line")));
    }

    @Test
    void rejectsInvalidOrEmptyRegex() {
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.type(regex: [a-) -> flow://order-flow", CATCH_ALL));
        assertThrows(IllegalArgumentException.class,
                () -> compile("input.header.type(regex: ) -> flow://order-flow", CATCH_ALL));
    }

    @Test
    void rejectsEmptyOrNullRuleList() {
        List<String> empty = List.of();
        assertThrows(IllegalArgumentException.class, () -> RoutingRuleSet.compile(empty));
        assertThrows(IllegalArgumentException.class, () -> RoutingRuleSet.compile(null));
    }
}
