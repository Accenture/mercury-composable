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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

public class ManifestReader {
    private static final Logger log = LoggerFactory.getLogger(ManifestReader.class);
    private static final String META_INF_MANIFEST = "META-INF/MANIFEST.MF";
    private static final String APP_VERSION = "info.app.version";
    private static final String DEFAULT_APP_VERSION = "1.0.0";

    public static String getVersionFromManifest() {
        // Manifest information available from compiled JAR or bundle only
        try (InputStream in = ManifestReader.class.getClassLoader().getResourceAsStream(META_INF_MANIFEST)) {
            if (in != null) {
                var manifest = new Manifest(in);
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
}
