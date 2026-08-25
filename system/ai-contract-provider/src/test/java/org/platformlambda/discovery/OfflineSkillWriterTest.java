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

package org.platformlambda.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.discovery.export.OfflineSkillWriter;
import org.platformlambda.discovery.services.SkillSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OfflineSkillWriterTest {

    @TempDir
    Path temp;

    @Test
    void exportsAVerifiableSnapshotAndTwoExportsAreByteIdentical() throws IOException {
        var writer = new OfflineSkillWriter();
        var first = Files.createDirectory(temp.resolve("first"));
        var second = Files.createDirectory(temp.resolve("second"));
        var result = writer.export(first.toString());
        writer.export(second.toString());

        var firstTree = readTree(first.resolve(OfflineSkillWriter.SKILL_DIRECTORY));
        var secondTree = readTree(second.resolve(OfflineSkillWriter.SKILL_DIRECTORY));
        assertEquals(firstTree.keySet(), secondTree.keySet());
        firstTree.forEach((path, bytes) -> assertArrayEquals(bytes, secondTree.get(path), path));
        assertEquals(SkillSnapshot.getInstance().getFiles().size() + 1, firstTree.size(),
                "snapshot files plus manifest.json");
        assertEquals(firstTree.size(), result.get("files"));

        // the manifest must verify against the exported bytes, independently recomputed
        var manifest = SimpleMapper.getInstance().getMapper().readValue(
                new String(firstTree.get(SkillSnapshot.MANIFEST)), Map.class);
        assertEquals(result.get("snapshot_sha256"), manifest.get("snapshot_sha256"));
        @SuppressWarnings("unchecked")
        var entries = (List<Map<String, Object>>) manifest.get("files");
        assertEquals(firstTree.size() - 1, entries.size());
        for (Map<String, Object> entry : entries) {
            var path = (String) entry.get("path");
            assertEquals(SkillSnapshot.sha256(firstTree.get(path)), entry.get("sha256"), path);
        }
    }

    @Test
    void refusesAnExistingSnapshotAndNeverOverwrites() throws IOException {
        var writer = new OfflineSkillWriter();
        var exportRoot = temp.toString();
        writer.export(exportRoot);
        var marker = temp.resolve(OfflineSkillWriter.SKILL_DIRECTORY).resolve("SKILL.md");
        var original = Files.readAllBytes(marker);
        var e = assertThrows(AppException.class, () -> writer.export(exportRoot));
        assertEquals(409, e.getStatus());
        assertArrayEquals(original, Files.readAllBytes(marker), "existing snapshot untouched");
    }

    @Test
    void rejectsAMissingOrInvalidExportRoot() {
        var writer = new OfflineSkillWriter();
        for (String root : new String[]{null, "", temp.resolve("absent").toString()}) {
            var e = assertThrows(AppException.class, () -> writer.export(root));
            assertEquals(400, e.getStatus());
        }
    }

    private static Map<String, byte[]> readTree(Path skill) throws IOException {
        var files = new TreeMap<String, byte[]>();
        try (var paths = Files.walk(skill)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                files.put(skill.relativize(path).toString().replace('\\', '/'),
                        Files.readAllBytes(path));
            }
        }
        return files;
    }
}
