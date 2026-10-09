package com.accenture.minigraph.playground;

import com.accenture.minigraph.common.GraphModelGate;
import com.accenture.minigraph.contract.GraphSchemaValidator;
import com.accenture.minigraph.models.CompiledGraphs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Input validation at the root of a deployed graph (RFC-0007, WP2): a root {@code schema} turns the
 * assumed validation step on, a bad request is refused with every violation in one message before
 * anything runs, the root's {@code exception=} handler takes over a failed validation when declared,
 * the gate refuses a schema outside the vocabulary (so the graph answers 404), and the validator
 * function's own contract holds for a direct caller.
 */
class GraphSchemaValidationTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;
    private static final String TENANT = "acme-west-1";
    private static String target;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", config.getProperty("server.port", "8085"));
        target = "http://127.0.0.1:" + port;
    }

    private EventEnvelope post(String graphId, Map<String, Object> body, Map<String, String> headers)
            throws ExecutionException, InterruptedException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target).setUrl("/api/graph/" + graphId)
                .setHeader("Content-Type", "application/json").setHeader("Accept", "application/json").setBody(body);
        headers.forEach(request::setHeader);
        var po = PostOffice.trackable("unit.test", "schema-" + graphId, "TEST /api/graph/" + graphId);
        return po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap()), TIMEOUT).get();
    }

    @SuppressWarnings("unchecked")
    private static MultiLevelMap body(EventEnvelope response) {
        assertInstanceOf(Map.class, response.getBody(), String.valueOf(response.getBody()));
        return new MultiLevelMap((Map<String, Object>) response.getBody());
    }

    @Test
    void aValidRequestRunsTheGraph() throws ExecutionException, InterruptedException {
        var response = post("unit-test-schema-1", Map.of("amount", 12.5, "currency", "USD"), Map.of("X-Tenant", TENANT));
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        var mm = body(response);
        assertEquals(12.5, mm.getElement("charged"));
        assertEquals("USD", mm.getElement("currency"));
        assertEquals("ok", mm.getElement("status"));
    }

    @Test
    void aBadRequestIsRefusedAtTheRootWithEveryViolation() throws ExecutionException, InterruptedException {
        var response = post("unit-test-schema-1",
                Map.of("amount", "12.5", "currency", "GBP", "note", "x".repeat(41)), Map.of());
        assertEquals(400, response.getStatus(), String.valueOf(response.getBody()));
        var mm = body(response);
        assertEquals("error", mm.getElement("type"));
        assertEquals("Input validation failed - input.body.amount: expected number, got string; " +
                "input.body.currency: must be one of USD, EUR; input.body.note: must be at most 40 characters; " +
                "input.header.X-Tenant: required", mm.getElement("message"));
        // a header is text: the parsed value is validated, and the name matches case-insensitively
        response = post("unit-test-schema-1", Map.of("amount", 1, "currency", "EUR"),
                Map.of("x-tenant", TENANT, "X-RETRY", "many"));
        assertEquals(400, response.getStatus(), String.valueOf(response.getBody()));
        assertEquals("Input validation failed - input.header.X-Retry: expected integer", body(response).getElement("message"));
    }

    @Test
    void aRootExceptionHandlerTakesOverAFailedValidation() throws ExecutionException, InterruptedException {
        var response = post("unit-test-schema-2", Map.of("amount", -1, "currency", "USD"), Map.of("X-Tenant", TENANT));
        assertEquals(422, response.getStatus(), String.valueOf(response.getBody()));
        var mm = body(response);
        assertEquals("Input validation failed - input.body.amount: must be more than 0", mm.getElement("reason"));
        assertEquals("root", mm.getElement("source"));
        assertEquals(400, mm.getElement("code"));
        // the same graph runs when the request passes
        response = post("unit-test-schema-2", Map.of("amount", 5, "currency", "USD"), Map.of("X-Tenant", TENANT));
        assertEquals(200, response.getStatus(), String.valueOf(response.getBody()));
        assertEquals("ok", body(response).getElement("status"));
    }

    @Test
    void theGateRefusesASchemaOutsideTheVocabulary() throws Exception {
        assertTrue(CompiledGraphs.graphExists("unit-test-schema-1"));
        assertTrue(CompiledGraphs.graphExists("unit-test-schema-2"));
        for (var id : List.of("unit-test-schema-err1", "unit-test-schema-err2")) {
            assertFalse(CompiledGraphs.graphExists(id), id + " must be rejected by the quality gate");
            assertEquals(404, post(id, Map.of("amount", 1), Map.of()).getStatus());
        }
        var err1 = new ConfigReader("classpath:/graph/unit-test-schema-err1.json").getMap();
        var ex = assertThrows(IllegalArgumentException.class, () -> GraphModelGate.validate("unit-test-schema-err1", err1));
        assertEquals("node root - schema.body.properties.amount: unknown keyword 'min' - the vocabulary is type, " +
                "properties, required, items, enum, minimum, maximum, exclusiveMinimum, exclusiveMaximum, minLength, " +
                "maxLength, pattern, minItems, maxItems, nullable and additionalProperties (title, description, " +
                "example and format are documentary)", ex.getMessage());
        var err2 = new ConfigReader("classpath:/graph/unit-test-schema-err2.json").getMap();
        ex = assertThrows(IllegalArgumentException.class, () -> GraphModelGate.validate("unit-test-schema-err2", err2));
        assertEquals("node root - schema.header.properties.X-Count: 'pattern' does not apply to type integer",
                ex.getMessage());
    }

    @Test
    void theValidatorFunctionContract() throws ExecutionException, InterruptedException {
        var po = PostOffice.trackable("unit.test", "schema-function", "TEST graph.schema.validator");
        Map<String, Object> schema = Map.of("body", Map.of("type", "object", "required", List.of("a"),
                "properties", Map.of("a", Map.of("type", "integer"))));
        var passed = po.request(new EventEnvelope().setTo(GraphSchemaValidator.ROUTE)
                .setBody(Map.of("schema", schema, "body", Map.of("a", 1), "header", Map.of())), TIMEOUT).get();
        assertEquals(200, passed.getStatus(), String.valueOf(passed.getBody()));
        assertEquals(Map.of("valid", true), passed.getBody());
        var failed = po.request(new EventEnvelope().setTo(GraphSchemaValidator.ROUTE)
                .setBody(Map.of("schema", schema, "body", Map.of("a", "1"))), TIMEOUT).get();
        assertEquals(400, failed.getStatus());
        assertEquals("Input validation failed - input.body.a: expected integer, got string", failed.getError());
        var refused = po.request(new EventEnvelope().setTo(GraphSchemaValidator.ROUTE)
                .setBody(Map.of("schema", Map.of("body", Map.of("type", "object", "min", 1)), "body", Map.of())), TIMEOUT).get();
        assertEquals(400, refused.getStatus());
        assertTrue(String.valueOf(refused.getError()).startsWith("Invalid schema - schema.body: unknown keyword 'min' - "),
                String.valueOf(refused.getError()));
        var missing = po.request(new EventEnvelope().setTo(GraphSchemaValidator.ROUTE)
                .setBody(Map.of("body", Map.of())), TIMEOUT).get();
        assertEquals(400, missing.getStatus());
        assertEquals("Invalid schema - schema: must be an object with body and/or header", missing.getError());
    }
}
