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

package org.platformlambda.helpers.registry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the Schema Registry over its real HTTP endpoints (wired in rest.yaml), going through the
 * built-in reactive HTTP server exactly as a Confluent client would — not by invoking the composable
 * functions directly. An {@link AsyncHttpRequest} sent to the {@code async.http.request} route reaches
 * the REST endpoint over HTTP.
 * <p>
 * The schema definitions are authored as immutable Maps for readability, then serialized to the escaped
 * "schema" string that the Confluent API carries on the wire (the registry treats a schema as an opaque
 * string). Immutable Maps are safe here because the event system deep-copies map payloads.
 */
class SchemaRegistryTest {

    private static int restPort;

    private static final Map<String, Object> AVRO_SCHEMA = Map.of(
            "type", "record",
            "name", "User",
            "fields", List.of(Map.of("name", "name", "type", "string")));

    private static final Map<String, Object> JSON_SCHEMA = Map.of(
            "$schema", "http://json-schema.org/draft-07/schema#",
            "type", "object",
            "properties", Map.of("age", Map.of("type", "integer")));

    @BeforeAll
    static void setup() {
        // The store defaults to the transient /tmp/schema-registry; wipe it before boot so each run
        // starts clean. (The test shares the default store path, so it must not leave data behind.)
        wipeTransientStore();
        // AutoStart is idempotent - it boots the platform-core runtime + REST automation HTTP server once.
        AutoStart.main(new String[0]);
        restPort = Utility.getInstance().str2int(
                AppConfigReader.getInstance().getProperty("rest.server.port", "8383"));
    }

    @AfterAll
    static void cleanup() {
        // Clean up the transient store so test schemas don't linger in the default /tmp/schema-registry
        // and get loaded by a later standalone-server run (e.g. the sync-over-async-demo).
        wipeTransientStore();
    }

    private static String storeDir() {
        return AppConfigReader.getInstance().getProperty("schema.registry.data.store", "/tmp/schema-registry");
    }

    /** Remove the configured store dir, but only when it is a transient /tmp path (never a durable one). */
    private static void wipeTransientStore() {
        String storeDir = storeDir();
        if (storeDir.startsWith("/tmp/")) {
            Utility.getInstance().cleanupDir(new File(storeDir));
        }
    }

    @Test
    void healthCheckReturnsEmptyObject() throws Exception {
        EventEnvelope res = http("GET", "/", null);
        assertEquals(200, res.getStatus());
        assertInstanceOf(Map.class, res.getBody());
        assertTrue(((Map<?, ?>) res.getBody()).isEmpty());
    }

    @Test
    void registerAndGetAvroSchema() throws Exception {
        String schema = asSchemaString(AVRO_SCHEMA);
        // schemaType omitted -> defaults to AVRO
        EventEnvelope res = http("POST", "/subjects/test-avro/versions", registerBody(schema, null));
        assertEquals(200, res.getStatus());
        assertInstanceOf(Map.class, res.getBody());
        int id = (Integer) ((Map<?, ?>) res.getBody()).get("id");
        assertTrue(id > 0);

        EventEnvelope getRes = http("GET", "/schemas/ids/" + id, null);
        assertEquals(200, getRes.getStatus());
        Map<?, ?> body = (Map<?, ?>) getRes.getBody();
        assertEquals(schema, body.get("schema"));
        assertFalse(body.containsKey("schemaType"), "AVRO schemaType should be omitted in the response");
    }

    @Test
    void registerAndGetJsonSchema() throws Exception {
        String schema = asSchemaString(JSON_SCHEMA);
        EventEnvelope res = http("POST", "/subjects/test-json/versions", registerBody(schema, "JSON"));
        assertEquals(200, res.getStatus());
        int id = (Integer) ((Map<?, ?>) res.getBody()).get("id");

        EventEnvelope getRes = http("GET", "/schemas/ids/" + id, null);
        assertEquals(200, getRes.getStatus());
        Map<?, ?> body = (Map<?, ?>) getRes.getBody();
        assertEquals(schema, body.get("schema"));
        assertEquals("JSON", body.get("schemaType"));
    }

    @Test
    void registrationIsIdempotent() throws Exception {
        String schema = asSchemaString(Map.of("type", "string"));
        EventEnvelope res1 = http("POST", "/subjects/test-idem/versions", registerBody(schema, null));
        int id1 = (Integer) ((Map<?, ?>) res1.getBody()).get("id");

        EventEnvelope res2 = http("POST", "/subjects/test-idem/versions", registerBody(schema, null));
        int id2 = (Integer) ((Map<?, ?>) res2.getBody()).get("id");

        assertEquals(id1, id2, "registering the same schema should return the same ID");
    }

    @Test
    void unknownSchemaIdReturns404() throws Exception {
        EventEnvelope res = http("GET", "/schemas/ids/999999", null);
        assertEquals(404, res.getStatus());
        // Confluent error body: {"error_code": 40403, "message": "..."}
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(40403, body.get("error_code"));
    }

    @Test
    void picksUpSchemaFileDroppedWhileRunning() throws Exception {
        // On-demand load: an <id>.json dropped into the store dir after boot is served on the next GET,
        // without a restart. (Confirms the per-id-file storage; the prior single-file store needed a reboot.)
        int id = 4242;
        String schema = asSchemaString(Map.of("type", "object", "title", "DroppedIn"));
        Files.writeString(new File(storeDir(), id + ".json").toPath(),
                SimpleMapper.getInstance().getCompactGson().toJson(Map.of("schema", schema, "schemaType", "JSON")));

        EventEnvelope res = http("GET", "/schemas/ids/" + id, null);
        assertEquals(200, res.getStatus());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(schema, body.get("schema"));
        assertEquals("JSON", body.get("schemaType"));
    }

    @Test
    void missingSchemaReturns422() throws Exception {
        // Missing the required "schema" string -> Confluent error_code 42201, HTTP 422
        EventEnvelope res = http("POST", "/subjects/test-bad/versions", Map.of("schemaType", "AVRO"));
        assertEquals(422, res.getStatus());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(42201, body.get("error_code"));
    }

    @Test
    void malformedSchemaReturns422() throws Exception {
        // A schema string that is not well-formed JSON -> Confluent error_code 42201, HTTP 422
        EventEnvelope res = http("POST", "/subjects/test-malformed/versions", registerBody("{not valid", null));
        assertEquals(422, res.getStatus());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals(42201, body.get("error_code"));
    }

    /** Serialize a schema document to the compact (non-pretty) escaped string the Confluent API carries. */
    private static String asSchemaString(Map<String, Object> schema) {
        return SimpleMapper.getInstance().getCompactGson().toJson(schema);
    }

    private static Map<String, Object> registerBody(String schemaString, String schemaType) {
        Map<String, Object> body = new HashMap<>();
        body.put("schema", schemaString);
        if (schemaType != null) {
            body.put("schemaType", schemaType);
        }
        return body;
    }

    /** Send a real HTTP request to the running Schema Registry through the AsyncHttpClient. */
    private static EventEnvelope http(String method, String uri, Map<String, Object> body) throws Exception {
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setTargetHost("http://127.0.0.1:" + restPort).setUrl(uri).setMethod(method)
                .setHeader("accept", "application/json");
        if (body != null) {
            req.setHeader("content-type", "application/json").setBody(body);
        }
        PostOffice po = PostOffice.trackable("unit.test", Utility.getInstance().getUuid(), method + " " + uri);
        return po.request(new EventEnvelope().setTo("async.http.request").setBody(req.toMap()), 10000).get();
    }
}
