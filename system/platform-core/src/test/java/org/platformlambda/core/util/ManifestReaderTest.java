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

package org.platformlambda.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The application's version comes from its own jar, even when an agent's jar is searched first.
 */
class ManifestReaderTest {
    private static final String TITLE = "Implementation-Title";
    private static final String VERSION = "Implementation-Version";

    @TempDir
    Path folder;

    @Test
    void theApplicationJarsManifestWinsOverAnAgents() throws IOException {
        File app = jar("app.jar", "demo-app", "9.9.9");
        File agent = jar("agent.jar", "oneagentjava.17", "1.347.64");

        Manifest manifest = ManifestReader.applicationManifest(app + File.pathSeparator + agent);

        assertNotNull(manifest);
        assertEquals("demo-app", manifest.getMainAttributes().getValue(TITLE));
        assertEquals("9.9.9", manifest.getMainAttributes().getValue(VERSION));
    }

    @Test
    void onlyTheFirstClassPathEntryIsTheApplication() throws IOException {
        File app = jar("app.jar", "demo-app", "9.9.9");

        assertEquals(app, ManifestReader.applicationJar(app.getPath()));
        // an IDE or a test run starts with a folder: the jar after it is a library
        assertNull(ManifestReader.applicationJar(folder + File.pathSeparator + app));
        assertNull(ManifestReader.applicationJar(folder.resolve("missing.jar").toString()));
        assertNull(ManifestReader.applicationJar(""));
        assertNull(ManifestReader.applicationJar(null));
    }

    @Test
    void withoutAnApplicationJarTheClassLoadersManifestIsUsed() throws IOException {
        File plain = folder.resolve("plain.jar").toFile();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(plain))) {
            out.flush();
        }

        // a jar with no manifest, and a class path that starts with a folder, both fall back as before
        Manifest fromPlainJar = ManifestReader.applicationManifest(plain.getPath());
        Manifest fromFolder = ManifestReader.applicationManifest(folder.toString());
        assertEquals(fromFolder, fromPlainJar);
        assertNotNull(ManifestReader.getVersionFromManifest());
    }

    private File jar(String name, String title, String version) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue(TITLE, title);
        manifest.getMainAttributes().putValue(VERSION, version);
        File file = folder.resolve(name).toFile();
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(file), manifest)) {
            out.flush();
        }
        return file;
    }
}
