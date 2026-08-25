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

package org.platformlambda.discovery.services;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.discovery.models.ContractEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * The version-matched skill snapshot: the packaged guide closure (files.list) plus the
 * generated installed-contracts.md, rendered once and cached. The REST endpoints serve this
 * snapshot and the offline exporter writes exactly the same bytes, so "what the runtime says"
 * and "what the export contains" cannot differ.
 * <p>
 * All resources are read through the classloader (getResourceAsStream), which works under
 * every packaging - classes directory, plain jar, and Spring Boot nested jar alike.
 */
public class SkillSnapshot {
    public static final String MANIFEST = "manifest.json";
    public static final String INSTALLED_CONTRACTS = "references/installed-contracts.md";
    private static final String SKILL_ROOT = "/skill/";
    private static final String FILES_LIST = SKILL_ROOT + "files.list";
    private static final String PLATFORM_CORE_POM_PROPERTIES =
            "/META-INF/maven/org.platformlambda/platform-core/pom.properties";
    private static final String EVENT_SCRIPT_POM_PROPERTIES =
            "/META-INF/maven/org.platformlambda/event-script-engine/pom.properties";
    // the one mkdocs snippet include used by the guides - expanded at render time
    private static final String FIXTURE_INCLUDE =
            "--8<-- \"system/platform-core/src/test/resources/guide-fixtures/rest-bindings.yaml\"";
    private static final String FIXTURE_PATH = "references/fixtures/rest-bindings.yaml";
    private static final String INCLUDE_MARKER = "--8<--";
    private static final String LLMS_LINK = "[`llms.txt`](llms.txt)";
    private static final String LLMS_NOTE = "`llms.txt` (public-site discovery map; not part of this offline snapshot)";
    private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[[^]]*]\\(([^)]+)\\)");
    private static final Pattern URI_SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*:.*");

    private static final SkillSnapshot INSTANCE = new SkillSnapshot();

    private final Map<String, byte[]> rendered;
    private final Map<String, Object> snapshotManifest;
    private final String mercuryVersion;

    private SkillSnapshot() {
        this.mercuryVersion = dependencyVersion(PLATFORM_CORE_POM_PROPERTIES);
        this.rendered = render();
        this.snapshotManifest = buildManifest(rendered);
    }

    public static SkillSnapshot getInstance() {
        return INSTANCE;
    }

    /** Mercury framework version, read from the platform-core dependency itself. */
    public String getMercuryVersion() {
        return mercuryVersion;
    }

    /** Event Script engine version - compared with platform-core's at startup (fail closed). */
    public String getEventScriptVersion() {
        return dependencyVersion(EVENT_SCRIPT_POM_PROPERTIES);
    }

    /** Every file of the snapshot except the manifest, path -> content. */
    public Map<String, byte[]> getFiles() {
        return rendered;
    }

    /** Deterministic manifest: per-file SHA-256 and a whole-snapshot hash. */
    public Map<String, Object> getManifest() {
        return snapshotManifest;
    }

    /** Read one snapshot file as text, or throw HTTP-404 for anything else. */
    public Map<String, Object> readFile(String path) {
        byte[] content = path == null ? null : rendered.get(path);
        if (content == null) {
            throw new AppException(404, "Reference " + path + " is not in this snapshot");
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("content", new String(content, StandardCharsets.UTF_8));
        result.put("type", contentType(path));
        return result;
    }

    private Map<String, byte[]> render() {
        var files = new TreeMap<String, byte[]>();
        for (String path : inventory()) {
            files.put(path, readClasspath(SKILL_ROOT + path));
        }
        var fixture = files.get(FIXTURE_PATH);
        if (fixture == null) {
            throw new IllegalArgumentException("Packaged snapshot is missing " + FIXTURE_PATH);
        }
        var fixtureText = new String(fixture, StandardCharsets.UTF_8).stripTrailing();
        for (var entry : new ArrayList<>(files.entrySet())) {
            if (entry.getKey().endsWith(".md")) {
                var text = new String(entry.getValue(), StandardCharsets.UTF_8)
                        .replace(FIXTURE_INCLUDE, fixtureText)
                        .replace(LLMS_LINK, LLMS_NOTE);
                if (text.contains(INCLUDE_MARKER)) {
                    throw new IllegalArgumentException("Unexpanded mkdocs include in " + entry.getKey());
                }
                files.put(entry.getKey(), text.getBytes(StandardCharsets.UTF_8));
            }
        }
        files.put(INSTALLED_CONTRACTS, installedContracts().getBytes(StandardCharsets.UTF_8));
        for (var contract : ContractCatalog.getInstance().getContracts()) {
            for (String reference : contract.references()) {
                if (!files.containsKey(reference)) {
                    throw new IllegalArgumentException("Contract " + contract.id()
                            + " references a file missing from the snapshot: " + reference);
                }
            }
        }
        validateLinks(files);
        return Map.copyOf(files);
    }

    public List<String> inventory() {
        var text = new String(readClasspath(FILES_LIST), StandardCharsets.UTF_8);
        var paths = new ArrayList<String>();
        for (String line : text.split("\\R")) {
            var path = line.trim();
            if (!path.isEmpty()) {
                if (path.startsWith("/") || path.startsWith(".") || path.contains("..")
                        || path.contains("\\") || path.contains(":") || paths.contains(path)) {
                    throw new IllegalArgumentException("Invalid files.list entry: " + path);
                }
                paths.add(path);
            }
        }
        paths.sort(String::compareTo);
        return paths;
    }

    private String installedContracts() {
        var out = new StringBuilder();
        out.append("# Installed Mercury contracts\n\n")
                .append("- Mercury version: `").append(mercuryVersion).append("`\n\n");
        for (ContractEntry contract : ContractCatalog.getInstance().getContracts()) {
            out.append("## `").append(contract.id()).append("`\n\n")
                    .append(contract.summary()).append(" (module `")
                    .append(contract.module()).append("`)\n\n")
                    .append("Behavior anchors:\n");
            contract.anchors().forEach(anchor -> out.append("- `").append(anchor).append("`\n"));
            out.append("\nReferences:\n");
            // installed-contracts.md lives under references/, so links are relative to it
            contract.references().forEach(reference -> out.append("- [").append(reference)
                    .append("](").append(reference.substring("references/".length())).append(")\n"));
            out.append('\n');
        }
        return out.toString();
    }

    private static void validateLinks(Map<String, byte[]> files) {
        for (var entry : files.entrySet()) {
            if (entry.getKey().endsWith(".md")) {
                var links = MARKDOWN_LINK.matcher(new String(entry.getValue(), StandardCharsets.UTF_8));
                while (links.find()) {
                    validateLink(files, entry.getKey(), links.group(1).trim());
                }
            }
        }
    }

    private static void validateLink(Map<String, byte[]> files, String source, String target) {
        if (target.isEmpty() || target.startsWith("#") || target.startsWith("//")
                || URI_SCHEME.matcher(target).matches()) {
            return;
        }
        var local = target.split("#", 2)[0].split("\\?", 2)[0];
        var parent = Path.of(source).getParent();
        var resolved = (parent == null ? Path.of(local) : parent.resolve(local)).normalize();
        if (resolved.isAbsolute() || resolved.startsWith("..")
                || !files.containsKey(resolved.toString().replace('\\', '/'))) {
            throw new IllegalArgumentException("Broken relative link in " + source + " -> " + target);
        }
    }

    private Map<String, Object> buildManifest(Map<String, byte[]> files) {
        var hashes = new TreeMap<String, String>();
        files.forEach((path, bytes) -> hashes.put(path, sha256(bytes)));
        var snapshotInput = new StringBuilder();
        var entries = new ArrayList<Map<String, Object>>();
        hashes.forEach((path, hash) -> {
            snapshotInput.append(path).append('\n').append(hash).append('\n');
            var item = new LinkedHashMap<String, Object>();
            item.put("path", path);
            item.put("sha256", hash);
            entries.add(item);
        });
        var result = new LinkedHashMap<String, Object>();
        result.put("type", "mercury-platform-skill");
        result.put("mercury_version", mercuryVersion);
        result.put("snapshot_sha256",
                sha256(snapshotInput.toString().getBytes(StandardCharsets.UTF_8)));
        result.put("files", entries);
        return result;
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".md")) {
            return "text/markdown";
        }
        return path.endsWith(".json") ? "application/json" : "text/plain";
    }

    private static String dependencyVersion(String pomProperties) {
        try (InputStream in = SkillSnapshot.class.getResourceAsStream(pomProperties)) {
            if (in != null) {
                var props = new Properties();
                props.load(in);
                var version = props.getProperty("version");
                if (version != null && !version.isBlank()) {
                    return version.trim();
                }
            }
        } catch (IOException e) {
            // fall through to the loud failure below
        }
        throw new IllegalStateException("Unable to read " + pomProperties
                + " - the Mercury dependency assembly is incomplete");
    }

    private static byte[] readClasspath(String name) {
        try (InputStream in = SkillSnapshot.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing packaged resource: " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read packaged resource: " + name, e);
        }
    }
}
