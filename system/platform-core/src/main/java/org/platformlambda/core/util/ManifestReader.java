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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Reads the application's version from its own jar manifest.
 * <p>
 * The manifest is the one in the jar that {@code java.class.path} names first, which for
 * {@code java -jar app.jar} is the application's jar. The class loader's first {@code META-INF/MANIFEST.MF}
 * is not reliable: parent delegation searches the boot class path first, and an agent that puts its jar
 * there, such as Dynatrace OneAgent, would report its own name and version. When the class path does not
 * start with a jar (an IDE or a test run), the class loader's first manifest is used, as before.
 */
public class ManifestReader {
    private static final Logger log = LoggerFactory.getLogger(ManifestReader.class);
    private static final String META_INF_MANIFEST = "META-INF/MANIFEST.MF";
    private static final String APP_VERSION = "info.app.version";
    private static final String DEFAULT_APP_VERSION = "1.0.0";

    private ManifestReader() {}

    public static String getVersionFromManifest() {
        // Manifest information available from compiled JAR or bundle only
        try {
            Manifest manifest = applicationManifest(System.getProperty("java.class.path"));
            if (manifest != null) {
                var title = manifest.getMainAttributes().getValue("Implementation-Title");
                var version = manifest.getMainAttributes().getValue("Implementation-Version");
                if (title != null) {
                    log.info("Application name: {}", title);
                }
                if (version != null) {
                    return version;
                } else {
                    log.info("Version information not available");
                    AppConfigReader reader = AppConfigReader.getInstance();
                    return reader.getProperty(APP_VERSION, DEFAULT_APP_VERSION);
                }
            }
        } catch (IOException e) {
            log.error("Unable to load MANIFEST.MF", e);
        }
        return DEFAULT_APP_VERSION;
    }

    /**
     * The application's manifest: the manifest of the class path's first entry when it is a jar, else the
     * class loader's first manifest. Visible for testing.
     *
     * @param classPath the class path, as in {@code java.class.path}
     * @return the manifest, or null when there is none
     * @throws IOException when the manifest cannot be read
     */
    static Manifest applicationManifest(String classPath) throws IOException {
        File jar = applicationJar(classPath);
        if (jar != null) {
            try (JarFile file = new JarFile(jar)) {
                Manifest manifest = file.getManifest();
                if (manifest != null) {
                    return manifest;
                }
            }
        }
        try (InputStream in = ManifestReader.class.getClassLoader().getResourceAsStream(META_INF_MANIFEST)) {
            return in == null? null : new Manifest(in);
        }
    }

    /**
     * The class path's first entry when it is a jar file. Only the first entry is the application's: the
     * jars after it are libraries or agents.
     */
    static File applicationJar(String classPath) {
        if (classPath == null || classPath.isEmpty()) {
            return null;
        }
        int separator = classPath.indexOf(File.pathSeparatorChar);
        File first = new File(separator < 0? classPath : classPath.substring(0, separator));
        return first.getName().endsWith(".jar") && first.isFile()? first : null;
    }
}
