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

package org.platformlambda.contracts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSkillExporterTest {
    private static final Pattern FILE_ENTRY = Pattern.compile(
            "\\{\\\"path\\\": \\\"([^\\\"]+)\\\", \\\"sha256\\\": \\\"([0-9a-f]{64})\\\"}");
    private static final Pattern SNAPSHOT = Pattern.compile(
            "\\\"snapshot_sha256\\\": \\\"([0-9a-f]{64})\\\"");

    @TempDir
    Path temp;

    @Test
    void rendersACompleteDeterministicOfflineSkill() {
        var exporter = exporter();
        var first = exporter.render();
        var second = exporter.render();

        assertEquals(first.keySet(), second.keySet());
        first.forEach((path, bytes) -> assertArrayEquals(bytes, second.get(path), path));
        assertTrue(first.containsKey("SKILL.md"));
        assertTrue(first.containsKey("security.json"));
        assertTrue(first.containsKey("references/runtime-contracts.md"));
        assertTrue(first.containsKey("references/fixtures/rest-bindings.yaml"));
        assertTrue(first.containsKey("references/index.md"));
        assertTrue(first.containsKey("references/arch-decisions/ADR.md"));
        assertTrue(first.containsKey("references/test-reports/event-over-http-interop.md"));
        assertTrue(first.containsKey("references/guides/getting-started.md"));
        assertTrue(first.containsKey("references/guides/knowledge-graph/minigraph-commands.json"));
        assertFalse(first.containsKey("llms.txt"));
        assertTrue(first.keySet().stream().noneMatch(path -> path.endsWith("/llms.txt")));

        var skill = new String(first.get("SKILL.md"), StandardCharsets.UTF_8);
        assertTrue(skill.startsWith("---\nname: mercury-platform\n"));
        assertTrue(skill.contains("both `service: http.flow.adapter` and `flow: <flow-id>`"));
        for (var entry : first.entrySet()) {
            if (entry.getKey().endsWith(".md")) {
                assertFalse(new String(entry.getValue(), StandardCharsets.UTF_8).contains("--8<--"),
                        entry.getKey());
                assertLocalLinksResolve(first, entry.getKey());
            }
        }
        verifyManifest(first);
    }

    @Test
    void exportsOnlyToTheFixedNewChild() throws IOException {
        var root = temp.toRealPath();
        var target = exporter().export(root);
        assertEquals(root.resolve("mercury-platform"), target);
        assertTrue(Files.isRegularFile(target.resolve("SKILL.md")));
        assertTrue(Files.isRegularFile(target.resolve("manifest.json")));
        try (var children = Files.list(root)) {
            assertTrue(children.map(path -> path.getFileName().toString())
                    .noneMatch(name -> name.endsWith(".pending")));
        }

        var error = assertThrows(ContractException.class, () -> exporter().export(root));
        assertEquals(ContractError.EXPORT_EXISTS, error.getError());
        assertNull(error.getCause());
        assertEquals(0, error.getSuppressed().length);
    }

    @Test
    void neverWritesIntoAPreexistingReservedTarget() throws IOException {
        var root = Files.createDirectory(temp.resolve("hostile-root")).toRealPath();
        var target = Files.createDirectory(root.resolve("mercury-platform"));
        var sentinel = target.resolve("sentinel.txt");
        Files.writeString(sentinel, "do-not-replace", StandardCharsets.UTF_8);

        var error = assertThrows(ContractException.class, () -> exporter().export(root));

        assertEquals(ContractError.EXPORT_EXISTS, error.getError());
        assertEquals("do-not-replace", Files.readString(sentinel, StandardCharsets.UTF_8));
        try (var children = Files.list(target)) {
            assertEquals(List.of("sentinel.txt"), children
                    .map(path -> path.getFileName().toString()).sorted().toList());
        }
    }

    @Test
    void concurrentExportersCannotReplaceEachOther() throws Exception {
        var root = temp.toRealPath();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var task = (Callable<Object>) () -> {
                ready.countDown();
                start.await();
                try {
                    return exporter().export(root);
                } catch (ContractException e) {
                    return e;
                }
            };
            var first = pool.submit(task);
            var second = pool.submit(task);
            ready.await();
            start.countDown();
            var results = List.of(first.get(), second.get());

            assertEquals(1, results.stream().filter(Path.class::isInstance).count());
            assertEquals(1, results.stream().filter(ContractException.class::isInstance).count());
            var failure = results.stream().filter(ContractException.class::isInstance)
                    .map(ContractException.class::cast).findFirst().orElseThrow();
            assertEquals(ContractError.EXPORT_EXISTS, failure.getError());
            assertTrue(Files.isRegularFile(root.resolve("mercury-platform/manifest.json")));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void failedPublicationRemovesItsReservedDirectory() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), new AgentSkillExporter.ExportHook() {
            @Override
            public void beforeManifest(Path ignoredRoot, Path ignoredTarget) throws IOException {
                throw new IOException("injected before manifest publication");
            }
        });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertNull(error.getCause());
        assertEquals(0, error.getSuppressed().length);
        assertFalse(Files.exists(root.resolve("mercury-platform")));
    }

    @Test
    void cleanupFailureIsBoundedAndDoesNotReplaceThePrimaryFailure() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), new AgentSkillExporter.ExportHook() {
            @Override
            public void beforeManifest(Path ignoredRoot, Path target) throws IOException {
                Files.writeString(target.resolve("manifest.json"), "injected", StandardCharsets.UTF_8);
                throw new IOException("injected before real manifest publication");
            }
        });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertNull(error.getCause());
        assertEquals(1, error.getSuppressed().length);
        var cleanup = (ContractException) error.getSuppressed()[0];
        assertEquals(ContractError.EXPORT_CLEANUP_FAILED, cleanup.getError());
        assertNull(cleanup.getCause());
        assertEquals(ContractError.EXPORT_CLEANUP_FAILED.message(), cleanup.getMessage());
    }

    @Test
    void neverReplacesAManifestCreatedDuringPublication() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), new AgentSkillExporter.ExportHook() {
            @Override
            public void beforePublication(Path ignoredRoot, Path target) throws IOException {
                Files.writeString(target.resolve("manifest.json"), "do-not-replace",
                        StandardCharsets.UTF_8);
            }
        });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertEquals("do-not-replace", Files.readString(
                root.resolve("mercury-platform/manifest.json"), StandardCharsets.UTF_8));
        assertEquals(1, error.getSuppressed().length);
        assertEquals(ContractError.EXPORT_CLEANUP_FAILED,
                ((ContractException) error.getSuppressed()[0]).getError());
    }

    @Test
    void atomicPublicationFailureCleansTheReservedDirectory() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), AgentSkillExporter.ExportHook.NO_OP,
                (manifest, staged) -> {
                    throw new IOException("injected hard-link failure");
                });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertFalse(Files.exists(root.resolve("mercury-platform")));
        try (var children = Files.list(root)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void unsupportedAtomicPublicationCleansTheReservedDirectory() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), AgentSkillExporter.ExportHook.NO_OP,
                (manifest, staged) -> {
                    throw new UnsupportedOperationException("hard links unsupported");
                });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertNull(error.getCause());
        assertFalse(Files.exists(root.resolve("mercury-platform")));
        try (var children = Files.list(root)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void partialStagingWriteFailureRemovesOwnedStagingAndReservation() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), AgentSkillExporter.ExportHook.NO_OP,
                Files::createLink, Files::delete, (output, content) -> {
                    output.write(content, 0, Math.min(8, content.length));
                    throw new IOException("injected partial staging write");
                });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertNull(error.getCause());
        assertEquals(0, error.getSuppressed().length);
        assertFalse(Files.exists(root.resolve("mercury-platform")));
        try (var children = Files.list(root)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void ownershipReadAndCleanupFailuresRemainBoundedAndAttached() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), AgentSkillExporter.ExportHook.NO_OP,
                Files::createLink,
                staged -> {
                    throw new IOException("injected staging cleanup failure");
                },
                (output, content) -> output.write(content),
                staged -> {
                    throw new IOException("injected staging identity failure");
                });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertNull(error.getCause());
        assertEquals(1, error.getSuppressed().length);
        assertEquals(ContractError.EXPORT_CLEANUP_FAILED,
                ((ContractException) error.getSuppressed()[0]).getError());
        assertFalse(Files.exists(root.resolve("mercury-platform")));
        try (var children = Files.list(root)) {
            assertEquals(1, children.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".pending")).count());
        }
    }

    @Test
    void stagingCleanupFailureDoesNotInvalidatePublishedSnapshot() throws IOException {
        var root = temp.toRealPath();
        var exporter = new AgentSkillExporter(registry(), AgentSkillExporter.ExportHook.NO_OP,
                Files::createLink, staged -> {
                    throw new IOException("injected post-publication cleanup failure");
                });

        var target = exporter.export(root);

        assertTrue(Files.isRegularFile(target.resolve("manifest.json")));
        verifyManifest(readSnapshot(target));
        try (var children = Files.list(root)) {
            assertEquals(1, children.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".pending")).count());
        }
        var retry = assertThrows(ContractException.class, () -> exporter().export(root));
        assertEquals(ContractError.EXPORT_EXISTS, retry.getError());
    }

    @Test
    void neverDeletesAReplacementAtTheStagingPath() throws IOException {
        var root = temp.toRealPath();
        var parked = root.resolve("owned-staging-manifest");
        var exporter = new AgentSkillExporter(registry(), new AgentSkillExporter.ExportHook() {
            @Override
            public void afterStaging(Path stagedManifest) throws IOException {
                Files.move(stagedManifest, parked);
                Files.writeString(stagedManifest, "replacement", StandardCharsets.UTF_8);
            }
        });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_INTEGRITY_FAILED, error.getError());
        assertEquals(1, error.getSuppressed().length);
        assertEquals(ContractError.EXPORT_CLEANUP_FAILED,
                ((ContractException) error.getSuppressed()[0]).getError());
        assertTrue(Files.isRegularFile(parked));
        try (var children = Files.list(root)) {
            var replacement = children.filter(path -> path.getFileName().toString()
                    .endsWith(".pending")).findFirst().orElseThrow();
            assertEquals("replacement", Files.readString(replacement, StandardCharsets.UTF_8));
        }
        assertFalse(Files.exists(root.resolve("mercury-platform")));
    }

    @Test
    void targetSwapCannotRedirectWritesOrDeleteTheReplacement() throws IOException {
        var root = temp.toRealPath();
        var parked = root.resolve("reserved-by-exporter");
        var exporter = new AgentSkillExporter(registry(), new AgentSkillExporter.ExportHook() {
            @Override
            public void afterReservation(Path ignoredRoot, Path target) throws IOException {
                Files.move(target, parked);
                Files.createDirectory(target);
            }
        });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_INTEGRITY_FAILED, error.getError());
        assertTrue(Files.isDirectory(parked));
        assertTrue(Files.isDirectory(root.resolve("mercury-platform")));
        try (var replacement = Files.list(root.resolve("mercury-platform"));
             var original = Files.list(parked)) {
            assertEquals(0, replacement.count());
            assertEquals(0, original.count());
        }
        assertEquals(1, error.getSuppressed().length);
        assertEquals(ContractError.EXPORT_CLEANUP_FAILED,
                ((ContractException) error.getSuppressed()[0]).getError());
    }

    @Test
    void trustedRootSwapCannotRedirectWritesOrDeleteTheReservation() throws IOException {
        var parent = Files.createDirectory(temp.resolve("parent")).toRealPath();
        var root = Files.createDirectory(parent.resolve("root")).toRealPath();
        var parkedRoot = parent.resolve("original-root");
        var exporter = new AgentSkillExporter(registry(), new AgentSkillExporter.ExportHook() {
            @Override
            public void afterReservation(Path trustedRoot, Path ignoredTarget) throws IOException {
                Files.move(trustedRoot, parkedRoot);
                Files.createDirectory(trustedRoot);
            }
        });

        var error = assertThrows(ContractException.class, () -> exporter.export(root));

        assertEquals(ContractError.EXPORT_IO_FAILED, error.getError());
        assertTrue(Files.isDirectory(parkedRoot.resolve("mercury-platform")));
        assertFalse(Files.exists(root.resolve("mercury-platform")));
        assertEquals(1, error.getSuppressed().length);
        assertEquals(ContractError.EXPORT_CLEANUP_FAILED,
                ((ContractException) error.getSuppressed()[0]).getError());
    }

    @Test
    void rejectsASymbolicLinkRoot() throws IOException {
        var root = temp.toRealPath();
        var actual = Files.createDirectory(root.resolve("actual"));
        var link = root.resolve("linked");
        try {
            Files.createSymbolicLink(link, actual);
        } catch (UnsupportedOperationException e) {
            return;
        }
        var error = assertThrows(ContractException.class, () -> exporter().export(link));
        assertEquals(ContractError.INVALID_EXPORT_ROOT, error.getError());
    }

    @Test
    void rejectsAProviderReferenceMissingFromTheSnapshot() {
        var missing = new MercuryContract(
                "platform-core",
                "test-module",
                "Test contract",
                List.of(String.class),
                List.of("references/not-packaged.md"));
        var exporter = new AgentSkillExporter(ContractRegistry.of(List.of(
                ContractRegistryTest.provider("missing-reference-provider", missing))));

        var error = assertThrows(ContractException.class, exporter::render);
        assertEquals(ContractError.EXPORT_INTEGRITY_FAILED, error.getError());
    }

    @Test
    void rejectsBrokenOfflineLinksAndMutatedSecurityMetadata() {
        var brokenLink = new TreeMap<>(exporter().render());
        brokenLink.put("references/contract-index.md",
                "[missing](not-packaged.md)\n".getBytes(StandardCharsets.UTF_8));
        var linkError = assertThrows(ContractException.class,
                () -> AgentSkillExporter.validateSnapshot(brokenLink));
        assertEquals(ContractError.EXPORT_INTEGRITY_FAILED, linkError.getError());

        var changedSecurity = new TreeMap<>(exporter().render());
        changedSecurity.put("security.json", "{}\n".getBytes(StandardCharsets.UTF_8));
        var securityError = assertThrows(ContractException.class,
                () -> AgentSkillExporter.validateSnapshot(changedSecurity));
        assertEquals(ContractError.EXPORT_INTEGRITY_FAILED, securityError.getError());
    }

    @Test
    void packagedResourcesIgnoreTheThreadContextClassLoader() {
        var prior = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new ClassLoader(null) {
                // deliberately has no Mercury resources
            });
            assertTrue(SkillResources.readText("SKILL.md").startsWith("---\nname: mercury-platform\n"));
        } finally {
            Thread.currentThread().setContextClassLoader(prior);
        }
    }

    private static AgentSkillExporter exporter() {
        return new AgentSkillExporter(registry());
    }

    private static ContractRegistry registry() {
        return ContractRegistry.of(List.of(
                ContractRegistryTest.provider("platform-core-provider",
                        ContractRegistryTest.contract("platform-core"))));
    }

    private static Map<String, byte[]> readSnapshot(Path skill) throws IOException {
        var files = new TreeMap<String, byte[]>();
        try (var paths = Files.walk(skill)) {
            for (var path : paths.toList()) {
                if (Files.isRegularFile(path)) {
                    files.put(skill.relativize(path).toString().replace('\\', '/'),
                            Files.readAllBytes(path));
                }
            }
        }
        return files;
    }

    private static void verifyManifest(Map<String, byte[]> rendered) {
        var manifest = new String(rendered.get("manifest.json"), StandardCharsets.UTF_8);
        var matcher = FILE_ENTRY.matcher(manifest);
        var declared = new TreeMap<String, String>();
        while (matcher.find()) {
            declared.put(matcher.group(1), matcher.group(2));
        }
        var expectedPaths = new java.util.TreeSet<>(rendered.keySet());
        expectedPaths.remove("manifest.json");
        assertEquals(expectedPaths, declared.keySet());
        declared.forEach((path, hash) -> assertEquals(hash, sha256(rendered.get(path)), path));

        var snapshotInput = new ByteArrayOutputStream();
        declared.forEach((path, hash) -> {
            snapshotInput.writeBytes(path.getBytes(StandardCharsets.UTF_8));
            snapshotInput.write('\n');
            snapshotInput.writeBytes(hash.getBytes(StandardCharsets.UTF_8));
            snapshotInput.write('\n');
        });
        var snapshotMatcher = SNAPSHOT.matcher(manifest);
        assertTrue(snapshotMatcher.find());
        assertEquals(sha256(snapshotInput.toByteArray()), snapshotMatcher.group(1));
        assertTrue(manifest.contains("\"verify\": \"Recompute every declared file hash "
                + "and snapshot_sha256 before use.\""));
        assertTrue(manifest.contains("\"remove\": \"After verification, remove only the exact "
                + "mercury-platform directory.\""));
        assertTrue(manifest.contains("\"re_export\": \"Remove the verified directory completely, "
                + "then export to a clean root.\""));
    }

    private static void assertLocalLinksResolve(Map<String, byte[]> rendered, String source) {
        var text = new String(rendered.get(source), StandardCharsets.UTF_8);
        var links = Pattern.compile("\\[[^]]*]\\(([^)]+)\\)").matcher(text);
        var parent = Path.of(source).getParent();
        while (links.find()) {
            var raw = links.group(1).trim();
            if (raw.startsWith("#") || raw.startsWith("//")
                    || raw.matches("[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
                continue;
            }
            var local = raw.split("#", 2)[0].split("\\?", 2)[0];
            var target = parent == null ? Path.of(local) : parent.resolve(local);
            var normalized = target.normalize().toString().replace('\\', '/');
            assertFalse(normalized.startsWith("../"), source + " link escapes the snapshot");
            assertTrue(rendered.containsKey(normalized), source + " -> " + normalized);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
