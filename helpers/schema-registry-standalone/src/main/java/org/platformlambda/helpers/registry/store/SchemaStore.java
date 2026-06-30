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

package org.platformlambda.helpers.registry.store;

import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimalist in-memory schema store mimicking Confluent Schema Registry, persisted to {@code schemas.json}
 * under the configurable {@code schema.registry.data.store} directory (default {@code /tmp/schema-registry}).
 *
 * <p>The store is <b>loaded on boot and persists across restarts</b>, so schema ids stay stable - {@code
 * schemas.json} is a convenient single-file store you can back up / seed (e.g. for a demo with known ids).
 * The default lives under {@code /tmp} (cleared by the OS on reboot); override to a durable directory
 * ({@code -Dschema.registry.data.store=$HOME/schema-registry}) to survive reboots.</p>
 */
public class SchemaStore {
    private static final Logger log = LoggerFactory.getLogger(SchemaStore.class);
    // Where schemas are persisted. Configurable so a user can choose a transient (/tmp) or a durable
    // path without dealing with filesystem-permission issues. Override with the config key
    // 'schema.registry.data.store' or the JVM flag -Dschema.registry.data.store=<dir>.
    private static final String STORE_DIR_KEY = "schema.registry.data.store";
    private static final String DEFAULT_STORE_DIR = "/tmp/schema-registry";

    private static final SchemaStore instance = new SchemaStore();

    public static final String AVRO_TYPE = "AVRO";
    public static final String PROTOBUF_TYPE = "PROTOBUF";

    private final ConcurrentMap<Integer, SchemaEntry> idToSchema = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> hashToId = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private final String storeDir;
    private final String storeFile;
    private final Utility util = Utility.getInstance();
    private final CryptoApi crypto = new CryptoApi();

    public record SchemaEntry(String schema, String schemaType) {}

    private SchemaStore() {
        this.storeDir = AppConfigReader.getInstance().getProperty(STORE_DIR_KEY, DEFAULT_STORE_DIR);
        this.storeFile = storeDir + "/schemas.json";
        log.info("Schema data store at {} (override with -D{}=<dir>)", storeDir, STORE_DIR_KEY);
        loadFromDisk();
    }

    public static SchemaStore getInstance() {
        return instance;
    }

    private String getHash(String typeAndSchema) {
        byte[] hashBytes = crypto.getSHA256(util.getUTF(typeAndSchema));
        return util.bytes2hex(hashBytes);
    }

    public int register(String schema, String schemaType) {
        String type = schemaType == null || schemaType.isEmpty() ? AVRO_TYPE : schemaType;
        String hashKey = getHash(type + ":" + schema);
        
        Integer existingId = hashToId.get(hashKey);
        if (existingId != null) {
            return existingId;
        }

        int newId = idGenerator.getAndIncrement();
        idToSchema.put(newId, new SchemaEntry(schema, type));
        hashToId.put(hashKey, newId);
        
        saveToDisk();
        return newId;
    }

    public SchemaEntry get(int id) {
        return idToSchema.get(id);
    }
    
    private void loadFromDisk() {
        File dir = new File(storeDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Failed to create directory: {}", storeDir);
            return;
        }
        Path filePath = Path.of(storeFile);
        if (!Files.exists(filePath)) {
            return;
        }
        try {
            String json = Files.readString(filePath);
            if (!json.isEmpty()) {
                loadEntries(json);
            }
        } catch (IOException e) {
            log.warn("Failed to load schemas from disk: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadEntries(String json) throws IOException {
        Object parsed = org.platformlambda.core.serializers.SimpleMapper.getInstance()
                .getMapper().readValue(json, Object.class);
        if (!(parsed instanceof Map)) {
            return;
        }
        int maxId = 0;
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) parsed).entrySet()) {
            if (entry.getValue() instanceof Map) {
                int id = util.str2int(entry.getKey());
                loadEntry(id, (Map<String, String>) entry.getValue());
                maxId = Math.max(maxId, id);
            }
        }
        if (maxId > 0) {
            idGenerator.set(maxId + 1);
        }
        log.info("Loaded {} schemas from disk. Next ID will be {}", idToSchema.size(), idGenerator.get());
    }

    private void loadEntry(int id, Map<String, String> schemaMap) {
        String schema = schemaMap.get("schema");
        // accept either "schemaType" (canonical) or a legacy "schema_type"; default to AVRO.
        String type = schemaMap.containsKey("schema_type")
                ? schemaMap.get("schema_type")
                : schemaMap.getOrDefault("schemaType", AVRO_TYPE);
        idToSchema.put(id, new SchemaEntry(schema, type));
        hashToId.put(getHash(type + ":" + schema), id);
    }
    
    private void saveToDisk() {
        try {
            String json = org.platformlambda.core.serializers.SimpleMapper.getInstance().getMapper().writeValueAsString(idToSchema);
            Files.writeString(Path.of(storeFile), json);
        } catch (IOException e) {
            log.warn("Failed to save schemas to disk: {}", e.getMessage());
        }
    }
}
