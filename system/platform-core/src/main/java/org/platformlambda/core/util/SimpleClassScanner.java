/*

    Copyright 2018-2025 Accenture Technology

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

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class SimpleClassScanner {
    private static final String WEB_COMPONENT_SCAN = "web.component.scan";
    private static final String PLATFORM_LAMBDA = "org.platformlambda.";
    private static final String ACCENTURE_COM = "com.accenture.";
    private static final String[] BASE_PACKAGE = {PLATFORM_LAMBDA, ACCENTURE_COM};
    private static final String EX_START = "Invalid package path (";
    private static final String EX_END = ")";
    private static final SimpleClassScanner INSTANCE = new SimpleClassScanner();

    private SimpleClassScanner() {
        // singleton
    }

    public static SimpleClassScanner getInstance() {
        return INSTANCE;
    }

    public List<ClassInfo> getAnnotatedClasses(Class<? extends Annotation> type, boolean includeBasePackage) {
        List<ClassInfo> result = new ArrayList<>();
        Set<String> packages = getPackages(includeBasePackage);
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

    public Set<String> getPackages(boolean includeBasePackage) {
        Set<String> result = new HashSet<>();
        if (includeBasePackage) {
            result.addAll(Arrays.asList(BASE_PACKAGE));
        }
        // add user packages from web.component.scan
        result.addAll(getScanComponents());
        return result;
    }

    private Set<String> getScanComponents() {
        Set<String> result = new HashSet<>();
        AppConfigReader reader = AppConfigReader.getInstance();
        List<String> packages = Utility.getInstance().split(reader.getProperty(WEB_COMPONENT_SCAN), ", []");
        for (String p : packages) {
            if (!isBasePackage(p)) {
                if (!p.contains(".")) {
                    throw new IllegalArgumentException(EX_START + p + EX_END);
                } else {
                    result.add(normalizePackage(p));
                }
            }
        }
        return result;
    }

    private String normalizePackage(String text) {
        List<String> parts = Utility.getInstance().split(text, ".");
        StringBuilder sb = new StringBuilder();
        for (String p: parts) {
            sb.append(p);
            sb.append('.');
        }
        return sb.toString();
    }

    private boolean isBasePackage(String namespace) {
        for (String p: BASE_PACKAGE) {
            if (namespace.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
