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

package com.accenture.twin.demo.tasks;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * v1.profile.store parses the plain JSON request bytes (the cloud cluster carries no schema)
 * and applies the command to the temp store - directly testable without Kafka.
 */
class ProfileStoreTest {

    private static final int TEST_ID = 987654;

    private final ProfileStore store = new ProfileStore();

    private Map<String, Object> apply(Map<String, Object> request) throws IOException {
        byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsString(request)
                .getBytes(StandardCharsets.UTF_8);
        return store.handleEvent(Map.of(), payload, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void fullLifecycle() throws IOException {
        Map<String, Object> profile = Map.of(
                "id", TEST_ID, "name", "Mary Jane", "address", "Queens", "telephone", "555-0000");
        // READ before create - errors travel as data
        Map<String, Object> missing = apply(Map.of("command", "READ", "id", TEST_ID));
        assertEquals("SYSTEM_OF_RECORDS", missing.get("originator"));
        assertEquals("Profile " + TEST_ID + " not found", missing.get("message"));
        // UPSERT echoes the saved profile
        Map<String, Object> saved = apply(Map.of("command", "UPSERT", "id", TEST_ID, "profile", profile));
        assertEquals("Profile " + TEST_ID + " saved", saved.get("message"));
        assertEquals(profile.get("name"), ((Map<String, Object>) saved.get("profile")).get("name"));
        // READ returns the stored profile
        Map<String, Object> found = apply(Map.of("command", "READ", "id", TEST_ID));
        assertEquals("Profile " + TEST_ID + " found", found.get("message"));
        assertEquals("Mary Jane", ((Map<String, Object>) found.get("profile")).get("name"));
        // DELETE removes it; a second DELETE reports not found
        Map<String, Object> deleted = apply(Map.of("command", "DELETE", "id", TEST_ID));
        assertEquals("Profile " + TEST_ID + " deleted", deleted.get("message"));
        Map<String, Object> gone = apply(Map.of("command", "DELETE", "id", TEST_ID));
        assertEquals("Profile " + TEST_ID + " not found", gone.get("message"));
    }

    @Test
    void unknownCommandAndBadIdTravelAsData() throws IOException {
        Map<String, Object> unknown = apply(Map.of("command", "PURGE", "id", TEST_ID));
        assertEquals("Unknown command PURGE", unknown.get("message"));
        // a non-numeric id degrades to -1 rather than failing the flow
        Map<String, Object> badId = apply(Map.of("command", "READ", "id", "not-a-number"));
        assertEquals(-1, badId.get("id"));
    }
}
