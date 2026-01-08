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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class SimpleClassScanner {
    private static final Logger log = LoggerFactory.getLogger(SimpleClassScanner.class);
    private static final String WEB_COMPONENT_SCAN = "web.component.scan";
    private static final String PLATFORM_LAMBDA = "org.platformlambda.";
    private static final String ACCENTURE_COM = "com.accenture.";
    private static final String[] BASE_PACKAGE = {PLATFORM_LAMBDA, ACCENTURE_COM};
    private static final String EX_START = "Invalid package path (";
    private static final String EX_END = ")";
    private static final Set<String> scanPackages = new HashSet<>();
    private static final AtomicBoolean loaded = new AtomicBoolean(false);
    private static final SimpleClassScanner INSTANCE = new SimpleClassScanner();

    private SimpleClassScanner() {
        // singleton
    }

    public static SimpleClassScanner getInstance() {
        return INSTANCE;
    }

    public List<ClassInfo> getAnnotatedClasses(Class<? extends Annotation> type) {
        List<ClassInfo> result = new ArrayList<>();
        Set<String> packages = getPackages();
        for (String p : packages) {
            result.addAll(getAnnotatedClasses(p, type));
        }
        return result;
    }

    public List<ClassInfo> getAnnotatedClasses(String scanPath, Class<? extends Annotation> type) {
        if (!scanPath.contains(".")) {
            throw new IllegalArgumentException(EX_START + scanPath + EX_END);
        }
        try (ScanResult sr = new ClassGraph().enableAllInfo().acceptPackages(scanPath).scan()) {
            return new ArrayList<>(sr.getClassesWithAnnotation(type));
        }
    }

    public Set<String> getPackages() {
        if (!loaded.get() && scanPackages.isEmpty()) {
            loaded.set(true);
            AppConfigReader reader = AppConfigReader.getInstance();
            List<String> packages = Utility.getInstance().split(reader.getProperty(WEB_COMPONENT_SCAN), ", []");
            for (String p : packages) {
                if (p.contains(".")) {
                    var userPackage = normalizePackagePath(p);
                    if (!isCovered(userPackage)) {
                        scanPackages.add(userPackage);
                    }
                } else {
                    throw new IllegalArgumentException(EX_START + p + EX_END);
                }
            }
            // guarantee that scan package includes the base packages
            scanPackages.addAll(Arrays.asList(BASE_PACKAGE));
        }
        return scanPackages;
    }

    /**
     * Check if the user package covered by the base package
     *
     * @param packageName in web.component.scan
     * @return true if covered
     */
    private boolean isCovered(String packageName) {
        for (String p : BASE_PACKAGE) {
            if (packageName.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normalize package path with dot format
     *
     * @param packageName in web.component.scan
     * @return formatted path
     */
    private String normalizePackagePath(String packageName) {
        List<String> parts = Utility.getInstance().split(packageName, ".");
        StringBuilder sb = new StringBuilder();
        for (String p: parts) {
            sb.append(p);
            sb.append('.');
        }
        return sb.toString();
    }
}
