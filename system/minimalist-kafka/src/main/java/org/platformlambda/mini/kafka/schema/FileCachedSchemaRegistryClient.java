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

package org.platformlambda.mini.kafka.schema;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.SchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.platformlambda.core.serializers.SimpleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * A {@link CachedSchemaRegistryClient} that adds a <b>file-backed</b> cache for {@code getSchemaById} - the
 * hot path used by both serialization-by-id and deserialization. Each schema is persisted as
 * {@code <dir>/<id>.json} ({@code {"schemaType": ..., "schema": ...}}); a cache entry is honored while the
 * file's age is within the configured TTL, after which it is refreshed from the registry. This survives
 * JVM restarts (warm cache on boot) and reduces registry round-trips.
 *
 * <p>The directory + TTL come from {@code application.properties} ({@code schema.registry.data.store}-style:
 * {@code schema.registry.cache.dir}, {@code schema.registry.cache.ttl}). The Confluent serdes call
 * {@code getSchemaById} through this client, so they transparently benefit. This phase reconstructs
 * cached JSON schemas only; other types simply fall through to the registry (still correct).</p>
 */
public class FileCachedSchemaRegistryClient extends CachedSchemaRegistryClient {
    private static final Logger log = LoggerFactory.getLogger(FileCachedSchemaRegistryClient.class);
    private static final String SCHEMA_TYPE = "schemaType";
    private static final String SCHEMA = "schema";

    private final File cacheDir;
    private final long ttlMillis;

    public FileCachedSchemaRegistryClient(List<String> urls, int identityMapCapacity,
                                          List<SchemaProvider> providers, Map<String, ?> configs,
                                          File cacheDir, long ttlMillis) {
        super(urls, identityMapCapacity, providers, configs);
        this.cacheDir = cacheDir;
        this.ttlMillis = ttlMillis;
        if (ttlMillis > 0 && !cacheDir.exists() && !cacheDir.mkdirs()) {
            log.warn("Unable to create schema cache dir {}; file caching disabled", cacheDir);
        }
    }

    @Override
    public ParsedSchema getSchemaById(int id) throws IOException, RestClientException {
        if (ttlMillis <= 0) {
            return super.getSchemaById(id);   // file caching disabled
        }
        ParsedSchema cached = readFromFile(id);
        if (cached != null) {
            return cached;
        }
        ParsedSchema schema = super.getSchemaById(id);
        writeToFile(id, schema);
        return schema;
    }

    private ParsedSchema readFromFile(int id) {
        Path file = new File(cacheDir, id + ".json").toPath();
        try {
            if (!Files.exists(file)) {
                return null;
            }
            long age = System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis();
            if (age > ttlMillis) {
                return null;   // expired -> refresh from registry
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = SimpleMapper.getInstance().getMapper()
                    .readValue(Files.readString(file), Map.class);
            String type = String.valueOf(entry.get(SCHEMA_TYPE));
            String schema = String.valueOf(entry.get(SCHEMA));
            // This phase reconstructs JSON schemas only; other types fall through to the registry.
            if (SchemaType.JSON.name().equals(type)) {
                return new JsonSchema(schema);
            }
            return null;
        } catch (IOException | RuntimeException e) {
            log.warn("Ignoring unreadable schema cache entry for id {}: {}", id, e.getMessage());
            return null;
        }
    }

    private void writeToFile(int id, ParsedSchema schema) {
        if (schema == null) {
            return;
        }
        try {
            String json = SimpleMapper.getInstance().getCompactGson()
                    .toJson(Map.of(SCHEMA_TYPE, schema.schemaType(), SCHEMA, schema.canonicalString()));
            Files.writeString(new File(cacheDir, id + ".json").toPath(), json);
        } catch (IOException | RuntimeException e) {
            log.warn("Unable to cache schema id {} to {}: {}", id, cacheDir, e.getMessage());
        }
    }
}
