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

package org.platformlambda.discovery.export;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.discovery.services.SkillSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the offline mercury-platform Agent Skill: exactly the rendered snapshot the REST
 * endpoints serve, plus manifest.json written LAST as the completion marker (a directory
 * without manifest.json is an incomplete export). Safety properties: the target directory
 * must not pre-exist, every file is created with CREATE_NEW (never overwrites), all content
 * is verified by re-reading before the manifest is written, and a failed export cleans up
 * the partial directory - but never one that already carries a manifest.
 */
public class OfflineSkillWriter {
    private static final Logger log = LoggerFactory.getLogger(OfflineSkillWriter.class);

    public static final String SKILL_DIRECTORY = "mercury-platform";

    public Map<String, Object> export(String directory) {
        if (directory == null || directory.isBlank()) {
            throw new AppException(400, "Missing export directory");
        }
        var root = Path.of(directory).toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) {
            throw new AppException(400, "Export root must be an existing directory");
        }
        var target = root.resolve(SKILL_DIRECTORY);
        if (Files.exists(target)) {
            throw new AppException(409, "Snapshot already exists - remove " + SKILL_DIRECTORY
                    + " from the export root first");
        }
        var snapshot = SkillSnapshot.getInstance();
        try {
            Files.createDirectory(target);
            writeAndVerify(target, snapshot.getFiles());
            var manifest = SimpleMapper.getInstance().getMapper()
                    .writeValueAsString(snapshot.getManifest());
            write(target.resolve(SkillSnapshot.MANIFEST), manifest.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            cleanupIncomplete(target);
            if (e instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(500, "Skill export to " + target + " failed - " + e.getMessage(), e);
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("skill_directory", target.toString());
        result.put("files", snapshot.getFiles().size() + 1);
        result.put("snapshot_sha256", snapshot.getManifest().get("snapshot_sha256"));
        return result;
    }

    private static void writeAndVerify(Path target, Map<String, byte[]> files) throws IOException {
        for (var entry : files.entrySet()) {
            var destination = target.resolve(entry.getKey()).normalize();
            if (!destination.startsWith(target)) {
                throw new AppException(500, "Invalid snapshot path: " + entry.getKey());
            }
            Files.createDirectories(destination.getParent());
            write(destination, entry.getValue());
        }
        for (var entry : files.entrySet()) {
            if (!Arrays.equals(entry.getValue(), Files.readAllBytes(target.resolve(entry.getKey())))) {
                throw new AppException(500, "Verification failed for " + entry.getKey());
            }
        }
    }

    private static void write(Path destination, byte[] content) throws IOException {
        try (var out = Files.newOutputStream(destination,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            out.write(content);
        }
    }

    private static void cleanupIncomplete(Path target) {
        try {
            if (!Files.isDirectory(target)
                    || Files.exists(target.resolve(SkillSnapshot.MANIFEST))) {
                return;
            }
            try (var paths = Files.walk(target)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException e) {
            log.warn("Unable to clean up incomplete export at {} - {}", target, e.getMessage());
        }
    }
}
