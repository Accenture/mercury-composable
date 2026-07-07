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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import org.platformlambda.helpers.registry.store.SchemaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Faithful mock of Confluent's {@code GET /subjects/{subject}/versions/{version}} (fetch the schema metadata
 * for a subject at a version). {@code version} is a positive integer or the alias {@code latest}.
 * <p>
 * The REST endpoint is wired directly to this function in rest.yaml (no flow), so the input is the raw
 * {@link AsyncHttpRequest} and {@code subject}/{@code version} come from the path. The response mirrors
 * Confluent: {@code {"subject", "id", "version", "schema"}} with {@code "schemaType"} included only for
 * non-AVRO schemas (Confluent omits it for AVRO). Unknown subject ⇒ 404 / 40401; unknown version ⇒ 404 / 40402.
 * This is what {@code CachedSchemaRegistryClient.getSchemaMetadata(subject, version)} and
 * {@code getLatestSchemaMetadata(subject)} call to resolve a subject/version to a global schema id.
 */
@PreLoad(route = "schema.registry.get.version", instances = 10)
public class GetVersionFunction implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final Logger log = LoggerFactory.getLogger(GetVersionFunction.class);

    private static final String LATEST = "latest";
    private static final String NOT_FOUND_SUFFIX = "' not found";

    private final SchemaStore store = SchemaStore.getInstance();
    private final Utility util = Utility.getInstance();

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        String subject = input.getPathParameter("subject");
        String versionParam = input.getPathParameter("version");
        if (subject == null || subject.isEmpty() || !store.hasSubject(subject)) {
            log.warn("GET /subjects/{}/versions/{} -> 404 (subject not found)", subject, versionParam);
            return ApiError.of(404, ApiError.SUBJECT_NOT_FOUND, "Subject '" + subject + NOT_FOUND_SUFFIX);
        }
        Integer version = resolveVersion(subject, versionParam);
        if (version == null) {
            log.warn("GET /subjects/{}/versions/{} -> 404 (version not found)", subject, versionParam);
            return ApiError.of(404, ApiError.VERSION_NOT_FOUND, "Version '" + versionParam + NOT_FOUND_SUFFIX);
        }
        Integer id = store.idForVersion(subject, version);
        SchemaStore.SchemaEntry entry = id == null ? null : store.get(id);
        if (entry == null) {
            log.warn("GET /subjects/{}/versions/{} -> 404 (version not found)", subject, versionParam);
            return ApiError.of(404, ApiError.VERSION_NOT_FOUND, "Version '" + versionParam + NOT_FOUND_SUFFIX);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("subject", subject);
        response.put("id", id);
        response.put("version", version);
        response.put("schema", entry.schema());
        // Confluent omits schemaType for AVRO (the default) and includes it otherwise.
        if (!SchemaStore.AVRO_TYPE.equals(entry.schemaType())) {
            response.put("schemaType", entry.schemaType());
        }
        log.info("GET /subjects/{}/versions/{} -> 200 (id={}, version={}, schemaType={})",
                subject, versionParam, id, version, entry.schemaType());
        return new EventEnvelope().setStatus(200).setBody(response);
    }

    /** Resolve {@code latest} to the newest version, or parse a positive integer; null if not resolvable. */
    private Integer resolveVersion(String subject, String versionParam) {
        if (versionParam == null || versionParam.isEmpty()) {
            return null;
        }
        if (LATEST.equalsIgnoreCase(versionParam)) {
            return store.latestVersion(subject);
        }
        return util.isDigits(versionParam) ? util.str2int(versionParam) : null;
    }
}
