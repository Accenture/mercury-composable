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

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The specs documents this application offers by name: the files of the specs folder named by
 * the {@code api.playground.specs} property and the sample document
 * {@code resources/sample/yaml/demo.yaml}. A folder file wins over the sample document of the same
 * name, so a deployed {@code demo.yaml} replaces it. Only YAML and JSON files are offered
 * ({@code .yaml}, {@code .yml} or {@code .json}), and a name must be a plain file name (letters,
 * digits, dot, dash, underscore). Deploying from the page writes the current document into the
 * folder through {@link #write}.
 */
public final class SpecFiles {

    private static final SpecFiles INSTANCE = new SpecFiles();
    private static final String EXAMPLE = "demo.yaml";
    private static final String EXAMPLE_RESOURCE = "/sample/yaml/" + EXAMPLE;
    private static final String SPECS_FOLDER = "api.playground.specs";
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final String YAML = "application/yaml";
    private static final String JSON = "application/json";

    private SpecFiles() {}

    public static SpecFiles getInstance() {
        return INSTANCE;
    }

    /** The specs document names, sorted: the folder's files and the sample document. */
    public SortedSet<String> list() {
        TreeSet<String> names = new TreeSet<>();
        names.add(EXAMPLE);
        File folder = specsFolder();
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

    /** The specs document's text, or null when no such document is offered. */
    public String read(String filename) throws IOException {
        if (!offered(filename)) {
            return null;
        }
        File folder = specsFolder();
        if (folder != null) {
            File f = new File(folder, filename);
            if (f.isFile()) {
                return Files.readString(f.toPath(), StandardCharsets.UTF_8);
            }
        }
        if (EXAMPLE.equals(filename)) {
            try (InputStream in = this.getClass().getResourceAsStream(EXAMPLE_RESOURCE)) {
                return in == null ? null : Utility.getInstance().stream2str(in);
            }
        }
        return null;
    }

    /**
     * Write a document into the specs folder.
     *
     * @param filename a plain file name ending with {@code .yaml}, {@code .yml} or {@code .json}
     * @param content the text of the document
     * @param overwrite true to replace an existing file of that name
     * @return true when an existing file was replaced
     * @throws AppException 400 for a bad name or empty content, 409 when the file exists and
     *                      overwrite is false, 503 when no specs folder is configured
     */
    public boolean write(String filename, String content, boolean overwrite) throws AppException, IOException {
        if (!offered(filename)) {
            throw new AppException(400, "File name must be a plain name ending with .yaml, .yml or .json");
        }
        if (content == null || content.isBlank()) {
            throw new AppException(400, "Nothing to deploy - the document is empty");
        }
        String path = AppConfigReader.getInstance().getProperty(SPECS_FOLDER);
        if (path == null || path.isBlank()) {
            throw new AppException(503, "No specs folder - set " + SPECS_FOLDER + " in application.properties");
        }
        File folder = new File(path.trim());
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new AppException(503, "Cannot create the specs folder " + folder);
        }
        File target = new File(folder, filename);
        boolean exists = target.isFile();
        if (exists && !overwrite) {
            throw new AppException(409, filename + " exists in the specs folder - confirm to overwrite");
        }
        Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
        return exists;
    }

    /** The configured folder path, for messages; null when not configured. */
    public String folderPath() {
        String path = AppConfigReader.getInstance().getProperty(SPECS_FOLDER);
        return path == null || path.isBlank() ? null : path.trim();
    }

    public String contentType(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".json") ? JSON : YAML;
    }

    private static boolean offered(String name) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".json");
    }

    private static File specsFolder() {
        String path = AppConfigReader.getInstance().getProperty(SPECS_FOLDER);
        if (path == null || path.isBlank()) {
            return null;
        }
        File folder = new File(path.trim());
        return folder.isDirectory() ? folder : null;
    }
}
