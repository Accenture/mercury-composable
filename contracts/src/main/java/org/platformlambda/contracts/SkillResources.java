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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

/** Access to the canonical skill bytes packaged in {@code platform-contracts}. */
public final class SkillResources {
    public static final String ROOT = "mercury/agent-skill/";
    private static final String FILES = ROOT + "files.list";

    private SkillResources() {
        // utility class
    }

    public static List<String> inventory() {
        var text = readClasspath(FILES);
        var paths = new ArrayList<String>();
        for (var line : text.split("\\R")) {
            var path = line.trim();
            if (!path.isEmpty()) {
                validateRelativePath(path);
                if (paths.contains(path)) {
                    throw new ContractException(ContractError.EXPORT_INTEGRITY_FAILED);
                }
                paths.add(path);
            }
        }
        Collections.sort(paths);
        return Collections.unmodifiableList(paths);
    }

    public static byte[] read(String relativePath) {
        validateRelativePath(relativePath);
        return readClasspathBytes(ROOT + relativePath);
    }

    public static String readText(String relativePath) {
        return new String(read(relativePath), StandardCharsets.UTF_8);
    }

    static void validateRelativePath(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.startsWith(".")
                || path.contains("..") || path.contains("\\") || path.contains(":")) {
            throw new ContractException(ContractError.EXPORT_INTEGRITY_FAILED);
        }
    }

    private static String readClasspath(String name) {
        return new String(readClasspathBytes(name), StandardCharsets.UTF_8);
    }

    private static byte[] readClasspathBytes(String name) {
        try {
            var codeSource = SkillResources.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null
                    || !"file".equals(codeSource.getLocation().getProtocol())) {
                throw new ContractException(ContractError.EXPORT_INTEGRITY_FAILED);
            }
            var location = Path.of(codeSource.getLocation().toURI()).toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (Files.isDirectory(location, LinkOption.NOFOLLOW_LINKS)) {
                var resource = location.resolve(name).normalize();
                if (!resource.startsWith(location) || !Files.isRegularFile(resource, LinkOption.NOFOLLOW_LINKS)
                        || !resource.toRealPath().startsWith(location)) {
                    throw new ContractException(ContractError.EXPORT_INTEGRITY_FAILED);
                }
                return Files.readAllBytes(resource);
            }
            if (!Files.isRegularFile(location, LinkOption.NOFOLLOW_LINKS)) {
                throw new ContractException(ContractError.EXPORT_INTEGRITY_FAILED);
            }
            try (var jar = new JarFile(location.toFile(), true)) {
                var entry = jar.getJarEntry(name);
                if (entry == null || entry.isDirectory()) {
                    throw new ContractException(ContractError.EXPORT_INTEGRITY_FAILED);
                }
                try (var in = jar.getInputStream(entry)) {
                    return in.readAllBytes();
                }
            }
        } catch (IOException | URISyntaxException | SecurityException e) {
            throw new ContractException(ContractError.EXPORT_IO_FAILED);
        }
    }
}
