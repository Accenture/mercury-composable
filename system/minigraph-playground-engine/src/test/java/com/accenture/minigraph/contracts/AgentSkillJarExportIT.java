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

package com.accenture.minigraph.contracts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSkillJarExportIT {
    private static final String VERSION = "4.11.9";
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("!?\\[[^]]*]\\(([^)]+)\\)");
    private static final Pattern URI_SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*:.*");

    @TempDir
    Path temp;

    @Test
    void packagedRuntimeExportsOneCompleteDeterministicSkill() throws Exception {
        var reactor = reactorRoot();
        var contractsJar = reactor.resolve("contracts/target/platform-contracts-" + VERSION + ".jar");
        var firstRoot = Files.createDirectory(temp.resolve("first"));
        var secondRoot = Files.createDirectory(temp.resolve("second"));
        var classpath = packagedRuntimeClasspath(reactor);

        invokeExporter(classpath, firstRoot.toRealPath());
        invokeExporter(classpath, secondRoot.toRealPath());

        var first = readSnapshot(firstRoot.resolve("mercury-platform"));
        var second = readSnapshot(secondRoot.resolve("mercury-platform"));
        assertEquals(first.keySet(), second.keySet());
        first.forEach((path, bytes) -> assertArrayEquals(bytes, second.get(path), path));
        assertEquals(56, first.size());
        verifyManifest(first);
        verifyRuntimeInventory(first);
        verifyOfflineLinks(first);
        assertJarResourceEquals(contractsJar, "mercury/agent-skill/SKILL.md",
                first.get("SKILL.md"));
        assertJarResourceEquals(contractsJar, "mercury/agent-skill/security.json",
                first.get("security.json"));
    }

    private static Path reactorRoot() throws IOException {
        var configured = System.getProperty("mercury.reactor.root");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toRealPath();
        }
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("contracts/pom.xml"))
                    && Files.isRegularFile(current.resolve("system/platform-core/pom.xml"))) {
                return current.toRealPath();
            }
            current = current.getParent();
        }
        throw new IOException("Mercury reactor root is unavailable");
    }

    private static String packagedRuntimeClasspath(Path reactor) {
        var classesToJar = Map.of(
                reactor.resolve("contracts/target/classes").normalize(),
                reactor.resolve("contracts/target/platform-contracts-" + VERSION + ".jar"),
                reactor.resolve("system/platform-core/target/classes").normalize(),
                reactor.resolve("system/platform-core/target/platform-core-" + VERSION + ".jar"),
                reactor.resolve("system/event-script-engine/target/classes").normalize(),
                reactor.resolve("system/event-script-engine/target/event-script-engine-" + VERSION + ".jar"),
                reactor.resolve("system/minigraph-playground-engine/target/classes").normalize(),
                reactor.resolve("system/minigraph-playground-engine/target/minigraph-playground-engine-"
                        + VERSION + ".jar"));
        classesToJar.values().forEach(path -> assertTrue(Files.isRegularFile(path), path.toString()));
        var source = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        var entries = new ArrayList<String>();
        var packagedNames = classesToJar.values().stream()
                .map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
        for (var raw : source.split(Pattern.quote(File.pathSeparator))) {
            if (raw.isBlank()) {
                continue;
            }
            var path = Path.of(raw).toAbsolutePath().normalize();
            if (path.endsWith("target/test-classes") || classesToJar.containsKey(path)
                    || packagedNames.contains(path.getFileName().toString())) {
                continue;
            }
            entries.add(path.toString());
        }
        for (var jar : classesToJar.values()) {
            entries.add(jar.toString());
        }
        return String.join(File.pathSeparator, new TreeSet<>(entries));
    }

    private static void invokeExporter(String classpath, Path root) throws Exception {
        var javaName = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe" : "java";
        var java = Path.of(System.getProperty("java.home"), "bin", javaName);
        var outputFile = root.resolveSibling(root.getFileName() + ".export.log");
        var process = new ProcessBuilder(
                java.toString(), "-cp", classpath,
                "org.platformlambda.contracts.AgentSkillExportCli", root.toString())
                .redirectErrorStream(true).redirectOutput(outputFile.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            throw new AssertionError("exporter process timed out");
        }
        assertTrue(Files.size(outputFile) <= 8192, "exporter output exceeded 8 KiB");
        var output = Files.readString(outputFile, StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertEquals("Mercury platform skill exported", output.strip());
    }

    private static Map<String, byte[]> readSnapshot(Path skill) throws IOException {
        var files = new TreeMap<String, byte[]>();
        try (var paths = Files.walk(skill)) {
            for (var path : paths.toList()) {
                if (path.equals(skill)) {
                    continue;
                }
                assertFalse(Files.isSymbolicLink(path), path.toString());
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    files.put(skill.relativize(path).toString().replace('\\', '/'),
                            Files.readAllBytes(path));
                }
            }
        }
        return files;
    }

    @SuppressWarnings("unchecked")
    private static void verifyManifest(Map<String, byte[]> snapshot) throws IOException {
        var manifest = new String(snapshot.get("manifest.json"), StandardCharsets.UTF_8);
        var parsed = SimpleMapper.getInstance().getMapper().readValue(manifest, Map.class);
        assertEquals(Set.of("schema_version", "provenance", "mercury_version",
                "contract_build", "snapshot_sha256", "lifecycle", "files"), parsed.keySet());
        assertTrue(parsed.get("schema_version") instanceof Number);
        assertEquals(1, ((Number) parsed.get("schema_version")).intValue());
        assertEquals("platform-contracts", parsed.get("provenance"));
        assertEquals(VERSION, parsed.get("mercury_version"));
        assertEquals("platform-contracts/" + VERSION, parsed.get("contract_build"));

        assertTrue(parsed.get("lifecycle") instanceof Map<?, ?>);
        var lifecycle = (Map<String, Object>) parsed.get("lifecycle");
        assertEquals(Set.of("verify", "remove", "re_export"), lifecycle.keySet());
        assertEquals("Recompute every declared file hash and snapshot_sha256 before use.",
                lifecycle.get("verify"));
        assertEquals("After verification, remove only the exact mercury-platform directory.",
                lifecycle.get("remove"));
        assertEquals("Remove the verified directory completely, then export to a clean root.",
                lifecycle.get("re_export"));

        assertTrue(parsed.get("files") instanceof List<?>);
        var entries = (List<Object>) parsed.get("files");
        var declared = new TreeMap<String, String>();
        var declaredOrder = new ArrayList<String>();
        for (var value : entries) {
            assertTrue(value instanceof Map<?, ?>);
            var entry = (Map<String, Object>) value;
            assertEquals(Set.of("path", "sha256"), entry.keySet());
            assertTrue(entry.get("path") instanceof String);
            assertTrue(entry.get("sha256") instanceof String);
            var path = (String) entry.get("path");
            var hash = (String) entry.get("sha256");
            assertTrue(SHA_256.matcher(hash).matches(), path);
            assertFalse(declared.containsKey(path), "duplicate manifest path: " + path);
            declared.put(path, hash);
            declaredOrder.add(path);
        }
        var expected = new TreeSet<>(snapshot.keySet());
        expected.remove("manifest.json");
        assertEquals(expected, declared.keySet());
        assertEquals(new ArrayList<>(expected), declaredOrder);
        declared.forEach((path, hash) -> assertEquals(hash, sha256(snapshot.get(path)), path));

        var snapshotInput = new ByteArrayOutputStream();
        declared.forEach((path, hash) -> {
            snapshotInput.writeBytes(path.getBytes(StandardCharsets.UTF_8));
            snapshotInput.write('\n');
            snapshotInput.writeBytes(hash.getBytes(StandardCharsets.UTF_8));
            snapshotInput.write('\n');
        });
        assertTrue(parsed.get("snapshot_sha256") instanceof String);
        assertEquals(sha256(snapshotInput.toByteArray()), parsed.get("snapshot_sha256"));
    }

    private static void verifyRuntimeInventory(Map<String, byte[]> snapshot) {
        var inventory = new String(snapshot.get("references/runtime-contracts.md"),
                StandardCharsets.UTF_8);
        assertEquals(3, count(inventory, "## Provider `"));
        assertEquals(4, count(inventory, "### `"));
        for (var value : new String[]{
                "platform-core-provider", "event-script-provider", "minigraph-provider",
                "### `platform-core`", "### `rest-automation`", "### `event-script`",
                "### `minigraph`", "org.platformlambda.core.system.AppStarter",
                "org.platformlambda.core.system.PostOffice",
                "org.platformlambda.core.models.EventEnvelope",
                "org.platformlambda.automation.config.RoutingEntry",
                "com.accenture.automation.CompileFlows",
                "com.accenture.minigraph.services.GraphCommandService",
                "com.accenture.minigraph.start.CompileGraph",
                "com.accenture.minigraph.common.GraphModelValidator"}) {
            assertTrue(inventory.contains(value), value);
        }
    }

    private static void verifyOfflineLinks(Map<String, byte[]> snapshot) {
        for (var entry : snapshot.entrySet()) {
            if (!entry.getKey().endsWith(".md")) {
                continue;
            }
            var text = new String(entry.getValue(), StandardCharsets.UTF_8);
            assertFalse(text.contains("--8<--"), entry.getKey());
            var links = MARKDOWN_LINK.matcher(text);
            var parent = Path.of(entry.getKey()).getParent();
            while (links.find()) {
                var raw = links.group(1).trim();
                if (raw.isEmpty() || raw.startsWith("#") || raw.startsWith("//")
                        || URI_SCHEME.matcher(raw).matches()) {
                    continue;
                }
                var local = raw.split("#", 2)[0].split("\\?", 2)[0];
                var target = (parent == null ? Path.of(local) : parent.resolve(local)).normalize();
                assertFalse(target.isAbsolute() || target.startsWith(".."), entry.getKey());
                assertTrue(snapshot.containsKey(target.toString().replace('\\', '/')),
                        entry.getKey() + " -> " + target);
            }
        }
    }

    private static void assertJarResourceEquals(Path jar, String resource, byte[] exported)
            throws IOException {
        try (var archive = new JarFile(jar.toFile())) {
            var entry = archive.getJarEntry(resource);
            assertTrue(entry != null, resource);
            try (var in = archive.getInputStream(entry)) {
                assertArrayEquals(in.readAllBytes(), exported, resource);
            }
        }
    }

    private static int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
