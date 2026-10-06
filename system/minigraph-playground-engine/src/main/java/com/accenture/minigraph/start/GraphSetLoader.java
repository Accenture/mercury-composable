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

package com.accenture.minigraph.start;

import com.accenture.minigraph.common.GraphModelGate;
import com.accenture.minigraph.common.GraphSet;
import com.accenture.minigraph.models.CompiledGraphs;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deploys the graph sets a deployment manifest lists (ADR-0027). Beside its loose 'graphs', a manifest may name
 * packaged sets in 'sets' and a folder in 'unpack':
 * <ul>
 * <li>each set is read from '{location}/{set}.pack' with the strict read, and its names are checked before any path
 *     is built;</li>
 * <li>its graphs are unpacked into '{unpack}/{graph-id}.json' and pass the deployment gate the way a loose graph does,
 *     read back with their ${...} references resolved;</li>
 * <li>a set registers all of its graphs or none.</li>
 * </ul>
 * A generated manifest, '{unpack}/graphs.yaml', records what was deployed and which files the loader wrote, so the
 * next start removes a graph that a new version of a set no longer holds. The loader never deletes a file it did
 * not write.
 * <p>
 * Precedence follows the manifest list: a manifest's sets compile right after its loose graphs and before the next
 * manifest, and a later graph owns a duplicate id. A duplicate that involves a set is logged as an error.
 */
final class GraphSetLoader {
    private static final Logger log = LoggerFactory.getLogger(GraphSetLoader.class);
    private static final Utility util = Utility.getInstance();
    static final String SETS = "sets";
    static final String UNPACK = "unpack";
    static final String GENERATED_MANIFEST = "graphs.yaml";
    private static final String GENERATED = "generated";
    private static final String VERSION = "version";
    private static final String FILE = "file:";
    private static final String FILE_PREFIX = "file:/";
    private static final String CLASSPATH = "classpath:";
    private static final String JSON_EXT = ".json";
    private static final String DEFAULT_TEMP_DIR = "/tmp/graph";
    private static final String NOT_DEPLOYED = "Set {} not deployed - {}";
    private static final Set<Path> UNPACK_FOLDERS = ConcurrentHashMap.newKeySet();

    private GraphSetLoader() {
        // utility class
    }

    /**
     * A new start: no unpack folder is in use yet.
     */
    static void reset() {
        UNPACK_FOLDERS.clear();
    }

    /**
     * Deploy the sets a manifest lists, after the manifest's loose graphs.
     *
     * @param manifest the manifest's path, for the log
     * @param reader the manifest
     * @param location the manifest's 'location', where the packages are read from
     */
    static void deploy(String manifest, ConfigReader reader, String location) {
        var sets = setNames(reader);
        if (sets.isEmpty()) {
            return;
        }
        var unpack = reader.getProperty(UNPACK, "").trim();
        var refusal = checkUnpack(unpack);
        if (refusal != null) {
            log.error("Graph sets in {} not deployed - {}", manifest, refusal);
            return;
        }
        // the unpacked graphs are deployed graphs: 'list graphs' and 'import graph from' search this folder too
        CompiledGraphs.addDeployedLocation(unpack);
        var folder = folderOf(unpack);
        removePreviousFiles(folder);
        var record = new Record();
        for (var setName : sets) {
            deploySet(location, unpack, folder, setName, record);
        }
        var generatedManifest = folder.resolve(GENERATED_MANIFEST);
        writeGeneratedManifest(generatedManifest, manifest, unpack, record);
        log.info("Graph sets in {} unpacked into {} - generated manifest {}", manifest, unpack, generatedManifest);
    }

    /**
     * What one manifest's sets left behind: the graphs that deployed, the files written per set (all of them, a
     * refused set's included, for the next start to remove) and one provenance line per set.
     */
    private static final class Record {
        final List<String> deployed = new ArrayList<>();
        final Map<String, List<String>> generated = new LinkedHashMap<>();
        final List<String> provenance = new ArrayList<>();
    }

    private static List<String> setNames(ConfigReader reader) {
        List<String> result = new ArrayList<>();
        if (reader.get(SETS) instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                result.add(reader.getProperty(SETS + "[" + i + "]", "").trim());
            }
        }
        return result;
    }

    private static String checkUnpack(String unpack) {
        if (unpack.isEmpty()) {
            return "'unpack' names no folder";
        }
        if (!unpack.startsWith(FILE_PREFIX)) {
            return "'unpack' must be a file:/ folder the application can write, not " + unpack;
        }
        var folder = folderOf(unpack);
        var temp = folderOf(AppConfigReader.getInstance().getProperty("location.graph.temp", DEFAULT_TEMP_DIR));
        if (folder.startsWith(temp)) {
            return "'unpack' must not be the Playground's temporary folder (location.graph.temp) or inside it";
        }
        if (!UNPACK_FOLDERS.add(folder)) {
            return "'unpack' " + unpack + " is used by another manifest";
        }
        var problem = writable(folder);
        return problem == null ? null : "cannot write in " + unpack + " - " + problem;
    }

    private static Path folderOf(String location) {
        var path = location.startsWith(FILE) ? location.substring(FILE.length()) : location;
        return Path.of(path).toAbsolutePath().normalize();
    }

    private static String writable(Path folder) {
        try {
            Files.createDirectories(folder);
            var probe = Files.createTempFile(folder, ".probe-", ".tmp");
            Files.delete(probe);
            return null;
        } catch (IOException | SecurityException e) {
            return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
    }

    /**
     * Remove the files the previous start unpacked here, as its generated manifest records them, so a graph that a
     * new version of a set no longer holds does not linger. Nothing else in the folder is touched.
     */
    private static void removePreviousFiles(Path folder) {
        var previous = folder.resolve(GENERATED_MANIFEST);
        if (!Files.isRegularFile(previous)) {
            return;
        }
        try {
            var reader = new ConfigReader(FILE + previous);
            if (reader.get(GENERATED) instanceof Map<?, ?> sets) {
                for (var ids : sets.values()) {
                    if (ids instanceof List<?> list) {
                        for (var id : list) {
                            var name = String.valueOf(id);
                            // a name becomes a path only when it is a valid graph id
                            if (GraphModelGate.isValidGraphId(name)) {
                                Files.deleteIfExists(folder.resolve(name + JSON_EXT));
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | IOException e) {
            log.warn("Unable to clean up with the previous generated manifest {} - {}", previous, e.getMessage());
        }
    }

    private static void deploySet(String location, String unpack, Path folder, String setName, Record record) {
        if (!GraphModelGate.isValidGraphId(setName)) {
            log.error(NOT_DEPLOYED, setName, "a set name uses letters, digits, '_' and '-' only");
            record.provenance.add("set " + setName + " not deployed - invalid set name");
            return;
        }
        var source = normalizedPath(location, setName + GraphSet.EXTENSION);
        byte[] bytes;
        GraphSet.Contents contents;
        try {
            bytes = readPackage(source);
            contents = GraphSet.read(bytes);
        } catch (GraphSet.RefusedException e) {
            log.error(NOT_DEPLOYED, setName, String.join("; ", e.getReasons()));
            record.provenance.add("set " + setName + " not deployed - its names break the set rules");
            return;
        } catch (IOException e) {
            log.error(NOT_DEPLOYED, setName, e.getMessage());
            record.provenance.add("set " + setName + " not deployed - " + oneLine(e.getMessage()));
            return;
        }
        var ids = new ArrayList<>(contents.graphs().keySet());
        // the files are written before the gate runs, and stay when it refuses the set, for the operator to inspect
        record.generated.put(setName, ids);
        try {
            for (var graph : contents.graphs().entrySet()) {
                Files.writeString(folder.resolve(graph.getKey() + JSON_EXT), GraphSet.toJson(graph.getValue()));
            }
        } catch (IOException e) {
            log.error(NOT_DEPLOYED, setName, "unable to unpack into " + unpack + " - " + e.getMessage());
            record.provenance.add(provenance(setName, source, bytes, contents) + " - not deployed");
            return;
        }
        // the deployment gate, all or none: every graph is checked before any is registered
        Map<String, Map<String, Object>> models = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        for (var id : ids) {
            try {
                var model = new ConfigReader(normalizedPath(unpack, id + JSON_EXT)).getMap();
                GraphModelGate.validate(id, model);
                models.put(id, model);
            } catch (IllegalArgumentException e) {
                failures.add(id + ": " + e.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            log.error("Set {} rejected - {} of {} failed: {}", setName, failures.size(), count(ids.size()),
                    String.join("; ", failures));
            record.provenance.add(provenance(setName, source, bytes, contents) + " - rejected");
            return;
        }
        var version = contents.manifest().getOrDefault(VERSION, "");
        var set = new CompiledGraphs.DeployedSet(setName, version);
        for (var model : models.entrySet()) {
            var id = model.getKey();
            var previous = CompiledGraphs.getGraphLocation(id);
            if (previous != null) {
                log.error("Graph {} from set {} ({}) replaces the copy from {}", id, setName, unpack,
                        describe(previous, CompiledGraphs.getGraphSet(id)));
                CompiledGraphs.removeGraph(id);
            }
            CompiledGraphs.addGraph(id, model.getValue(), unpack, set);
        }
        record.deployed.addAll(ids);
        record.provenance.add(provenance(setName, source, bytes, contents) + " - deployed");
        if (version.isEmpty()) {
            log.info("Deployed set {} from {} - {} into {}", setName, source, count(ids.size()), unpack);
        } else {
            log.info("Deployed set {} (version {}) from {} - {} into {}", setName, version, source,
                    count(ids.size()), unpack);
        }
    }

    /**
     * Where a compiled graph came from, for a replacement log: a set and its folder, or a manifest's location.
     *
     * @param location the deployed location of the compiled copy
     * @param set the set it came from, or null
     * @return the description
     */
    static String describe(String location, CompiledGraphs.DeployedSet set) {
        return set == null ? location : "set " + set.name() + " (" + location + ")";
    }

    private static String count(int n) {
        return n + (n == 1 ? " graph" : " graphs");
    }

    private static byte[] readPackage(String source) throws IOException {
        if (source.startsWith(CLASSPATH)) {
            try (var in = GraphSetLoader.class.getResourceAsStream(source.substring(CLASSPATH.length()))) {
                if (in == null) {
                    throw new FileNotFoundException(source + " not found");
                }
                return in.readAllBytes();
            }
        }
        var file = Path.of(source.substring(FILE.length()));
        if (!Files.isRegularFile(file)) {
            throw new FileNotFoundException(source + " not found");
        }
        return Files.readAllBytes(file);
    }

    private static String normalizedPath(String folder, String filename) {
        var sb = new StringBuilder();
        for (String part : util.split(folder, "/")) {
            sb.append('/').append(part);
        }
        sb.append('/').append(filename);
        return sb.substring(1);
    }

    private static String provenance(String setName, String source, byte[] bytes, GraphSet.Contents contents) {
        var sb = new StringBuilder("set ").append(setName).append(": ").append(source).append(", SHA-256 ")
                .append(sha256(bytes));
        contents.manifest().forEach((key, value) -> {
            if (!GraphSet.SET.equals(key) && !"format".equals(key) && !"format_version".equals(key)) {
                sb.append(", ").append(key).append('=').append(oneLine(value));
            }
        });
        return sb.toString();
    }

    private static String oneLine(String text) {
        return text == null ? "" : text.replace('\r', ' ').replace('\n', ' ');
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static void writeGeneratedManifest(Path file, String manifest, String unpack, Record record) {
        var sb = new StringBuilder();
        sb.append("# Generated by the graph-set loader from ").append(manifest)
                .append(" - rewritten at every start; do not edit\n");
        sb.append("# Unpacked ").append(Instant.now()).append('\n');
        record.provenance.forEach(line -> sb.append("# ").append(line).append('\n'));
        sb.append("graphs:").append(record.deployed.isEmpty() ? " []\n" : "\n");
        record.deployed.forEach(id -> sb.append("  - '").append(id).append("'\n"));
        sb.append("location: '").append(unpack).append("'\n");
        // the files each set's graphs were unpacked into, which the next start removes before it unpacks again
        sb.append(GENERATED).append(':').append(record.generated.isEmpty() ? " {}\n" : "\n");
        record.generated.forEach((set, ids) -> {
            sb.append("  '").append(set).append("':\n");
            ids.forEach(id -> sb.append("    - '").append(id).append("'\n"));
        });
        try {
            Files.writeString(file, sb.toString());
        } catch (IOException e) {
            log.error("Unable to write the generated manifest {} - {}", file, e.getMessage());
        }
    }
}
