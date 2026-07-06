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
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.helpers.registry.store.SchemaStore;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SchemaStore}'s in-memory subject/version logic and on-demand file loading, driven
 * directly against the singleton (no HTTP server). These cover the branches the HTTP-level
 * {@code SchemaRegistryTest} does not reach: null/empty subjects, per-subject versioning + idempotency,
 * unknown lookups, and the {@code <id>.json} parsing variants (legacy type, missing schema, malformed file).
 */
class SchemaStoreTest {

    private static final SchemaStore store = SchemaStore.getInstance();
    private static String storeDir;

    @BeforeAll
    static void ensureStoreDir() {
        storeDir = AppConfigReader.getInstance().getProperty("schema.registry.data.store", "/tmp/schema-registry");
        new File(storeDir).mkdirs();
    }

    @AfterAll
    static void cleanup() {
        if (storeDir.startsWith("/tmp/")) {
            Utility.getInstance().cleanupDir(new File(storeDir));
        }
    }

    @Test
    void registrationWithoutSubjectHasNoVersion() {
        // null and empty subjects both bypass version assignment; null/empty schemaType defaults to AVRO
        int idNull = store.register(null, uniqueSchema("no-subject-null"), null);
        int idEmptySubject = store.register("", uniqueSchema("no-subject-empty"), "AVRO");
        int idEmptyType = store.register("has-subject", uniqueSchema("empty-type"), "");
        assertTrue(idNull > 0 && idEmptySubject > 0 && idEmptyType > 0);
        assertEquals(SchemaStore.AVRO_TYPE, store.get(idNull).schemaType());
        assertEquals(SchemaStore.AVRO_TYPE, store.get(idEmptyType).schemaType());
    }

    @Test
    void contentAddressedIdIsSharedAcrossSubjects() {
        String schema = uniqueSchema("shared");
        int id1 = store.register("subject-a", schema, "AVRO");
        int id2 = store.register("subject-b", schema, "AVRO");   // same content -> same global id
        assertEquals(id1, id2);
        assertTrue(store.hasSubject("subject-a"));
        assertTrue(store.hasSubject("subject-b"));
    }

    @Test
    void perSubjectVersioningAndIdempotency() {
        String s1 = uniqueSchema("v-one");
        String s2 = uniqueSchema("v-two");
        int id1 = store.register("versioned", s1, "AVRO");   // version 1
        int id2 = store.register("versioned", s2, "AVRO");   // version 2 (different content)
        assertEquals(2, store.latestVersion("versioned"));
        assertEquals(id1, store.idForVersion("versioned", 1));
        assertEquals(id2, store.idForVersion("versioned", 2));
        // idempotent: re-registering the same content under the same subject returns the same id/version
        assertEquals(id1, store.register("versioned", s1, "AVRO"));
        assertEquals(2, store.latestVersion("versioned"), "no new version should be created");
    }

    @Test
    void unknownLookupsReturnNullOrFalse() {
        assertNull(store.get(987654));                         // absent id, no file
        assertFalse(store.hasSubject("nope"));
        assertNull(store.latestVersion("nope"));
        assertNull(store.idForVersion("nope", 1));             // unknown subject
        store.register("known-subject", uniqueSchema("k"), "AVRO");
        assertNull(store.idForVersion("known-subject", 999));  // known subject, unknown version
    }

    @Test
    void loadsSchemaFileVariantsOnDemand() throws Exception {
        // legacy "schema_type" key, no subject -> loaded on demand, type honored
        writeSchemaFile(9101, Map.of("schema", uniqueSchema("legacy"), "schema_type", "PROTOBUF"));
        SchemaStore.SchemaEntry legacy = store.get(9101);
        assertNotNull(legacy);
        assertEquals(SchemaStore.PROTOBUF_TYPE, legacy.schemaType());

        // missing "schema" -> ignored (null)
        writeSchemaFile(9102, Map.of("schemaType", "AVRO"));
        assertNull(store.get(9102));

        // malformed JSON -> ignored (null)
        Files.writeString(new File(storeDir, "9103.json").toPath(), "{ this is not json");
        assertNull(store.get(9103));
    }

    private static void writeSchemaFile(int id, Map<String, Object> content) throws Exception {
        Files.writeString(new File(storeDir, id + ".json").toPath(),
                SimpleMapper.getInstance().getCompactGson().toJson(content));
    }

    private static String uniqueSchema(String tag) {
        return SimpleMapper.getInstance().getCompactGson().toJson(Map.of("type", "record", "name", tag));
    }
}
