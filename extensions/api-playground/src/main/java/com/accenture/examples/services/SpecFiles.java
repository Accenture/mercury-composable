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

package com.accenture.examples.services;

import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The OpenAPI documents this application offers by name: the bundled example
 * {@code resources/sample/yaml/demo.yaml} and the files of the optional folder named by the
 * {@code api.playground.apps} property, so a developer can add documents without rebuilding.
 * The example wins over a folder file of the same name. Only yaml, yml and json files are
 * offered, and a requested name must be a plain file name (no path separators).
 */
public final class SpecFiles {

    private static final SpecFiles INSTANCE = new SpecFiles();
    private static final String EXAMPLE = "demo.yaml";
    private static final String EXAMPLE_RESOURCE = "/sample/yaml/" + EXAMPLE;
    private static final String APPS_FOLDER = "api.playground.apps";
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final String YAML = "application/yaml";
    private static final String JSON = "application/json";

    private SpecFiles() {}

    public static SpecFiles getInstance() {
        return INSTANCE;
    }

    /** The document names, sorted: the example and the folder's files. */
    public TreeSet<String> list() {
        TreeSet<String> names = new TreeSet<>();
        names.add(EXAMPLE);
        File folder = appsFolder();
        File[] files = folder == null ? null : folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && offered(f.getName())) {
                    names.add(f.getName());
                }
            }
        }
        return names;
    }

    /** The document's text, or null when no such document is offered. */
    public String read(String filename) throws IOException {
        if (!offered(filename)) {
            return null;
        }
        if (EXAMPLE.equals(filename)) {
            try (InputStream in = this.getClass().getResourceAsStream(EXAMPLE_RESOURCE)) {
                return in == null ? null : Utility.getInstance().stream2str(in);
            }
        }
        File folder = appsFolder();
        if (folder != null) {
            File f = new File(folder, filename);
            if (f.isFile()) {
                return Files.readString(f.toPath(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public String contentType(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".json") ? JSON : YAML;
    }

    private static boolean offered(String name) {
        if (!SAFE_NAME.matcher(name).matches()) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json");
    }

    private static File appsFolder() {
        String path = AppConfigReader.getInstance().getProperty(APPS_FOLDER);
        if (path == null || path.isBlank()) {
            return null;
        }
        File folder = new File(path.trim());
        return folder.isDirectory() ? folder : null;
    }
}
