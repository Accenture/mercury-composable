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
 * Minimalist in-memory schema store mimicking Confluent Schema Registry.
 * Persists schemas to /tmp/mini-schema-registry/schemas.json to survive server restarts.
 */
public class SchemaStore {
    private static final Logger log = LoggerFactory.getLogger(SchemaStore.class);
    private static final String STORE_DIR = "/tmp/mini-schema-registry";
    private static final String STORE_FILE = STORE_DIR + "/schemas.json";
    
    private static final SchemaStore instance = new SchemaStore();
    
    public static final String AVRO_TYPE = "AVRO";
    public static final String JSON_TYPE = "JSON";
    public static final String PROTOBUF_TYPE = "PROTOBUF";

    private final ConcurrentMap<Integer, SchemaEntry> idToSchema = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> hashToId = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    
    private final Utility util = Utility.getInstance();
    private final CryptoApi crypto = new CryptoApi();

    public record SchemaEntry(String schema, String schemaType) {}

    private SchemaStore() {
        loadFromDisk();
    }

    public static SchemaStore getInstance() {
        return instance;
    }
    
    /**
     * Clear the store (mostly for testing).
     */
    public void clear() {
        idToSchema.clear();
        hashToId.clear();
        idGenerator.set(1);
        saveToDisk();
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
    
    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        File dir = new File(STORE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Failed to create directory: {}", STORE_DIR);
            return;
        }
        
        Path filePath = Path.of(STORE_FILE);
        if (!Files.exists(filePath)) {
            return;
        }
        
        try {
            String json = Files.readString(filePath);
            if (json.isEmpty()) {
                return;
            }
            
            Object parsed = org.platformlambda.core.serializers.SimpleMapper.getInstance().getMapper().readValue(json, Object.class);
            if (parsed instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) parsed;
                int maxId = 0;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    int id = util.str2int(entry.getKey());
                    if (entry.getValue() instanceof Map) {
                        Map<String, String> schemaMap = (Map<String, String>) entry.getValue();
                        String schema = schemaMap.get("schema");
                        String type = schemaMap.getOrDefault("schemaType", AVRO_TYPE);
                        if (schemaMap.containsKey("schema_type")) {
                             type = schemaMap.get("schema_type");
                        }
                        
                        idToSchema.put(id, new SchemaEntry(schema, type));
                        hashToId.put(getHash(type + ":" + schema), id);
                        if (id > maxId) {
                            maxId = id;
                        }
                    }
                }
                if (maxId > 0) {
                    idGenerator.set(maxId + 1);
                }
                log.info("Loaded {} schemas from disk. Next ID will be {}", idToSchema.size(), idGenerator.get());
            }
        } catch (IOException e) {
            log.warn("Failed to load schemas from disk: {}", e.getMessage());
        }
    }
    
    private void saveToDisk() {
        try {
            String json = org.platformlambda.core.serializers.SimpleMapper.getInstance().getMapper().writeValueAsString(idToSchema);
            Files.writeString(Path.of(STORE_FILE), json);
        } catch (IOException e) {
            log.warn("Failed to save schemas to disk: {}", e.getMessage());
        }
    }
}
