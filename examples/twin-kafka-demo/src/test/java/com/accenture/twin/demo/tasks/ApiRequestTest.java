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
import org.platformlambda.core.exception.AppException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * v1.api.request is pure request validation and message shaping - directly testable.
 */
class ApiRequestTest {

    private final ApiRequest api = new ApiRequest();

    @SuppressWarnings("unchecked")
    @Test
    void upsertBuildsRequestAndAck() throws AppException {
        Map<String, Object> profile = Map.of(
                "id", 100, "name", "Peter Parker",
                "address", "20 Ingram Street, Queens", "telephone", "212-555-1212");
        Map<String, Object> result = api.handleEvent(
                Map.of("command", "UPSERT"), Map.of("profile", profile), 1);
        Map<String, Object> request = (Map<String, Object>) result.get("request");
        assertEquals("UPSERT", request.get("command"));
        assertEquals(100, request.get("id"));
        assertEquals(profile, request.get("profile"));
        Map<String, Object> ack = (Map<String, Object>) result.get("ack");
        assertEquals("HTTP_REQUEST", ack.get("originator"));
        assertEquals("Request accepted for processing", ack.get("message"));
        assertEquals(100, ack.get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void readAndDeleteCarryTheIdOnly() throws AppException {
        for (String command : new String[]{"READ", "DELETE"}) {
            Map<String, Object> result = api.handleEvent(
                    Map.of("command", command), Map.of("id", "100"), 1);
            Map<String, Object> request = (Map<String, Object>) result.get("request");
            assertEquals(command, request.get("command"));
            assertEquals(100, request.get("id"));
            assertFalse(request.containsKey("profile"));
        }
    }

    @Test
    void invalidRequestsAreRejected() {
        // missing command header = a broken flow definition
        AppException noCommand = assertThrows(AppException.class,
                () -> api.handleEvent(Map.of(), Map.of("id", "1"), 1));
        assertEquals(500, noCommand.getStatus());
        // UPSERT without a profile body
        AppException noProfile = assertThrows(AppException.class,
                () -> api.handleEvent(Map.of("command", "UPSERT"), new HashMap<>(), 1));
        assertEquals(400, noProfile.getStatus());
        // non-positive or non-numeric id
        AppException badId = assertThrows(AppException.class,
                () -> api.handleEvent(Map.of("command", "READ"), Map.of("id", "zero"), 1));
        assertEquals(400, badId.getStatus());
        AppException negative = assertThrows(AppException.class,
                () -> api.handleEvent(Map.of("command", "READ"), Map.of("id", "-5"), 1));
        assertEquals(400, negative.getStatus());
    }
}
