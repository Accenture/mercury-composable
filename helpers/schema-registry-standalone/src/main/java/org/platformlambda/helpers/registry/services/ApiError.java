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

package org.platformlambda.helpers.registry.services;

import org.platformlambda.core.models.EventEnvelope;

import java.util.Map;

/**
 * Builds a Confluent Schema Registry style error response.
 * <p>
 * Confluent returns errors as {@code {"error_code": <int>, "message": <string>}} with an HTTP status,
 * where {@code error_code} is a registry-specific sub-code (not the HTTP status). The well-known codes:
 * <ul>
 *   <li>40401 - Subject not found (HTTP 404)</li>
 *   <li>40402 - Version not found (HTTP 404)</li>
 *   <li>40403 - Schema not found (HTTP 404)</li>
 *   <li>42201 - Invalid schema (HTTP 422)</li>
 * </ul>
 */
final class ApiError {

    static final int SUBJECT_NOT_FOUND = 40401;
    static final int VERSION_NOT_FOUND = 40402;
    static final int SCHEMA_NOT_FOUND = 40403;
    static final int INVALID_SCHEMA = 42201;

    private ApiError() { }

    static EventEnvelope of(int httpStatus, int errorCode, String message) {
        return new EventEnvelope().setStatus(httpStatus)
                .setBody(Map.of("error_code", errorCode, "message", message));
    }
}
