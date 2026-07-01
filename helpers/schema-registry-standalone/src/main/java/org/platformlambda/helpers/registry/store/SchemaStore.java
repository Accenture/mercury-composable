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

import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimalist in-memory schema store mimicking Confluent Schema Registry, persisted as <b>one file per schema
 * id</b> ({@code <id>.json}, e.g. {@code 1.json}) under the configurable {@code schema.registry.data.store}
 * directory (default {@code /tmp/schema-registry}). Each file holds
 * {@code {"schema": ..., "schemaType": ..., "subject": ..., "version": ...}}.
 *
 * <p>The store is loaded on boot and persists across restarts, so schema ids stay stable - the per-id files
 * are a convenient store you can back up / seed (e.g. a demo with known ids). Because storage is one file per
 * id, a schema <b>dropped into the directory while the server is running</b> (e.g. {@code 10.json}) is picked
 * up <b>on demand</b> by the next {@code GET /schemas/ids/10} - no restart needed (see {@link #get(int)}).</p>
 *
 * <p><b>Subjects &amp; versions.</b> A global schema id is content-addressed (identical content = same id, even
 * across subjects, faithful to Confluent). Versions are per-subject and tracked in an in-memory
 * {@code subject → (version → id)} index, rebuilt on boot from the per-id files (each records the subject and
 * version it was first registered under). This backs {@code GET /subjects/{subject}/versions/{version}} and
 * {@code .../latest}. <b>Mock limitation:</b> a single {@code <id>.json} records one subject/version, so the
 * uncommon case of the same content registered under multiple subjects only round-trips the first across a
 * restart (all subjects still resolve within a running session). A file dropped in after boot is id-resolvable
 * on demand, but subject-resolvable only after a restart (or re-registration).</p>
 *
 * <p>The default lives under {@code /tmp} (cleared by the OS on reboot); override to a durable directory
 * ({@code -Dschema.registry.data.store=$HOME/schema-registry}) to survive reboots.</p>
 */
public class SchemaStore {
    private static final Logger log = LoggerFactory.getLogger(SchemaStore.class);
    // Where schemas are persisted. Configurable so a user can choose a transient (/tmp) or a durable
    // path without dealing with filesystem-permission issues. Override with the config key
    // 'schema.registry.data.store' or the JVM flag -Dschema.registry.data.store=<dir>.
    private static final String STORE_DIR_KEY = "schema.registry.data.store";
    private static final String DEFAULT_STORE_DIR = "/tmp/schema-registry";
    private static final String JSON_SUFFIX = ".json";
    private static final String SCHEMA = "schema";
    private static final String SCHEMA_TYPE = "schemaType";
    private static final String LEGACY_SCHEMA_TYPE = "schema_type";
    private static final String SUBJECT = "subject";
    private static final String VERSION = "version";

    private static final SchemaStore instance = new SchemaStore();

    public static final String AVRO_TYPE = "AVRO";
    public static final String PROTOBUF_TYPE = "PROTOBUF";

    private final ConcurrentMap<Integer, SchemaEntry> idToSchema = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> hashToId = new ConcurrentHashMap<>();
    // subject -> (version -> global schema id); versions are per-subject, ids are global/content-addressed.
    private final ConcurrentMap<String, ConcurrentMap<Integer, Integer>> subjectVersions = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    private final String storeDir;
    private final Utility util = Utility.getInstance();
    private final CryptoApi crypto = new CryptoApi();

    /** A stored schema: the opaque schema string, its type, and the subject/version it was registered under. */
    public record SchemaEntry(String schema, String schemaType, String subject, int version) {}

    private SchemaStore() {
        this.storeDir = AppConfigReader.getInstance().getProperty(STORE_DIR_KEY, DEFAULT_STORE_DIR);
        log.info("Schema data store at {} (one <id>{} per schema; override with -D{}=<dir>)",
                storeDir, JSON_SUFFIX, STORE_DIR_KEY);
        loadAllFromDisk();
    }

    public static SchemaStore getInstance() {
        return instance;
    }

    /**
     * Register a schema under a subject. The global id is content-addressed (identical content returns the
     * same id, even across subjects); a new version is assigned under the subject unless this id is already a
     * version there (idempotent).
     *
     * @param subject    the subject to register under (the per-subject version sequence)
     * @param schema     the opaque schema string
     * @param schemaType {@code AVRO} (default when null/empty), {@code JSON}, or {@code PROTOBUF}
     * @return the global schema id
     */
    public int register(String subject, String schema, String schemaType) {
        String type = schemaType == null || schemaType.isEmpty() ? AVRO_TYPE : schemaType;
        String hashKey = getHash(type + ":" + schema);
        Integer existingId = hashToId.get(hashKey);
        int id = existingId != null ? existingId : nextFreeId();
        int version = assignVersion(subject, id);
        if (existingId == null) {
            // First time this content is seen: store + persist with the registering subject/version.
            SchemaEntry entry = new SchemaEntry(schema, type, subject, version);
            idToSchema.put(id, entry);
            hashToId.put(hashKey, id);
            saveEntry(id, entry);
        }
        return id;
    }

    /** Assign (or find, if idempotent) the per-subject version for {@code id}. Returns 0 when no subject. */
    private int assignVersion(String subject, int id) {
        if (subject == null || subject.isEmpty()) {
            return 0;
        }
        ConcurrentMap<Integer, Integer> versions = subjectVersions.computeIfAbsent(subject, k -> new ConcurrentHashMap<>());
        synchronized (versions) {
            for (Map.Entry<Integer, Integer> e : versions.entrySet()) {
                if (e.getValue().equals(id)) {
                    return e.getKey();
                }
            }
            int version = versions.isEmpty() ? 1 : Collections.max(versions.keySet()) + 1;
            versions.put(version, id);
            return version;
        }
    }

    /**
     * Look up a schema by id. If it is not already in memory, an {@code <id>.json} dropped into the store
     * directory while the server is running is loaded here (on demand) - so a newly added schema is served on
     * the next request without a restart.
     *
     * @param id the global schema id
     * @return the stored entry, or {@code null} if no such id exists in memory or on disk
     */
    public SchemaEntry get(int id) {
        SchemaEntry entry = idToSchema.get(id);
        return entry != null ? entry : loadEntry(id);
    }

    /** @return true if the subject has at least one registered version. */
    public boolean hasSubject(String subject) {
        ConcurrentMap<Integer, Integer> versions = subjectVersions.get(subject);
        return versions != null && !versions.isEmpty();
    }

    /** @return the newest version number for the subject, or {@code null} if the subject is unknown/empty. */
    public Integer latestVersion(String subject) {
        ConcurrentMap<Integer, Integer> versions = subjectVersions.get(subject);
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        synchronized (versions) {
            return Collections.max(versions.keySet());
        }
    }

    /** @return the global schema id for {@code subject} at {@code version}, or {@code null} if not found. */
    public Integer idForVersion(String subject, int version) {
        ConcurrentMap<Integer, Integer> versions = subjectVersions.get(subject);
        return versions == null ? null : versions.get(version);
    }

    private String getHash(String typeAndSchema) {
        return util.bytes2hex(crypto.getSHA256(util.getUTF(typeAndSchema)));
    }

    private File fileFor(int id) {
        return new File(storeDir, id + JSON_SUFFIX);
    }

    /** Next id not already used in memory or on disk, so a dropped {@code <id>.json} is never overwritten. */
    private int nextFreeId() {
        int id;
        do {
            id = idGenerator.getAndIncrement();
        } while (idToSchema.containsKey(id) || fileFor(id).exists());
        return id;
    }

    private void loadAllFromDisk() {
        File dir = new File(storeDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Failed to create directory: {}", storeDir);
            return;
        }
        File[] files = dir.listFiles((d, name) -> name.length() > JSON_SUFFIX.length()
                && name.endsWith(JSON_SUFFIX)
                && util.isDigits(name.substring(0, name.length() - JSON_SUFFIX.length())));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            loadEntry(util.str2int(name.substring(0, name.length() - JSON_SUFFIX.length())));
        }
        log.info("Loaded {} schemas ({} subjects) from disk. Next ID will be {}",
                idToSchema.size(), subjectVersions.size(), idGenerator.get());
    }

    /**
     * Read {@code <id>.json} from disk into memory, index its subject/version, and advance the id generator
     * past it (so a later registration never reuses the id). Returns {@code null} when the file is absent or
     * unreadable.
     */
    @SuppressWarnings("unchecked")
    private SchemaEntry loadEntry(int id) {
        File file = fileFor(id);
        if (!file.exists()) {
            return null;
        }
        try {
            Map<String, Object> map = SimpleMapper.getInstance().getMapper()
                    .readValue(Files.readString(file.toPath()), Map.class);
            Object schemaValue = map.get(SCHEMA);
            if (!(schemaValue instanceof String schema) || schema.isEmpty()) {
                return null;
            }
            // accept either "schemaType" (canonical) or a legacy "schema_type"; default to AVRO.
            Object typeValue = map.containsKey(LEGACY_SCHEMA_TYPE) ? map.get(LEGACY_SCHEMA_TYPE)
                    : map.getOrDefault(SCHEMA_TYPE, AVRO_TYPE);
            String type = typeValue == null ? AVRO_TYPE : String.valueOf(typeValue);
            String subject = map.get(SUBJECT) == null ? null : String.valueOf(map.get(SUBJECT));
            int version = map.get(VERSION) == null ? 0 : util.str2int(String.valueOf(map.get(VERSION)));
            SchemaEntry entry = new SchemaEntry(schema, type, subject, version);
            idToSchema.put(id, entry);
            hashToId.put(getHash(type + ":" + schema), id);
            if (subject != null && !subject.isEmpty() && version > 0) {
                subjectVersions.computeIfAbsent(subject, k -> new ConcurrentHashMap<>()).put(version, id);
            }
            idGenerator.accumulateAndGet(id + 1, Math::max);
            return entry;
        } catch (IOException | RuntimeException e) {
            log.warn("Ignoring unreadable schema file {}: {}", file, e.getMessage());
            return null;
        }
    }

    private void saveEntry(int id, SchemaEntry entry) {
        try {
            Files.writeString(fileFor(id).toPath(),
                    SimpleMapper.getInstance().getMapper().writeValueAsString(entry));
        } catch (IOException e) {
            log.warn("Failed to save schema {}: {}", id, e.getMessage());
        }
    }
}
