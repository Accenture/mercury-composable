package com.accenture.minigraph.contract;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.SimpleMapper;

import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The graph contract's schema vocabulary (RFC-0007, WP2) pinned by the shared vector file
 * {@code graph-schema-vectors.json}: what the gate accepts and refuses, with the exact refusal, and
 * what the validator reports for a given input, every violation in order, plus the one-line message
 * with its cap. The same file, byte-identical, drives the Rust engine's test.
 */
class GraphSchemaTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> vectors() throws Exception {
        try (var in = GraphSchemaTest.class.getResourceAsStream("/graph-schema-vectors.json")) {
            assertNotNull(in, "the vector file");
            return SimpleMapper.getInstance().getMapper().readValue(new String(in.readAllBytes(), UTF_8), Map.class);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void theGateAcceptsTheVocabularyAndRefusesTheRest() throws Exception {
        var cases = (List<Map<String, Object>>) vectors().get("compile");
        assertFalse(cases.isEmpty());
        for (var c : cases) {
            var name = String.valueOf(c.get("name"));
            var part = String.valueOf(c.get("part"));
            var schema = c.get("schema");
            Runnable compile = switch (part) {
                case "contract" -> () -> GraphSchema.compileContract(schema);
                case "header" -> () -> GraphSchema.compileHeaderPart("schema.header", schema);
                default -> () -> GraphSchema.compile("schema.body", schema);
            };
            if (Boolean.TRUE.equals(c.get("ok"))) {
                assertDoesNotThrow(compile::run, name);
            } else {
                var ex = assertThrows(IllegalArgumentException.class, compile::run, name);
                assertEquals(c.get("error"), ex.getMessage(), name);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void theValidatorReportsEveryViolationInOrder() throws Exception {
        var cases = (List<Map<String, Object>>) vectors().get("validate");
        assertFalse(cases.isEmpty());
        for (var c : cases) {
            var name = String.valueOf(c.get("name"));
            var contract = GraphSchema.compileContract(c.get("schema"));
            var violations = contract.check(c.get("body"), c.get("header"));
            assertEquals(c.get("violations"), violations, name);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void theMessageCarriesTheViolationsUpToTheCap() throws Exception {
        var vectors = vectors();
        assertEquals(GraphSchema.VIOLATION_CAP, ((Number) vectors.get("cap")).intValue());
        var cases = (List<Map<String, Object>>) vectors.get("report");
        assertFalse(cases.isEmpty());
        for (var c : cases) {
            var name = String.valueOf(c.get("name"));
            assertEquals(c.get("message"), GraphSchema.report((List<String>) c.get("violations")), name);
        }
    }
}
