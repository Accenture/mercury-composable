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

package org.platformlambda.core.models;

import java.util.HashMap;
import java.util.Map;

public abstract class GraphProperties {
    private final Map<String, Object> properties = new HashMap<>();

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void addProperty(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        validateName(key);
        properties.put(key, value);
    }

    public void removeProperty(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be empty");
        }
        properties.remove(key);
    }

    protected void validateName(String name) {
        if (!validFormat(name)) {
            throw new IllegalArgumentException("Invalid syntax for " + name +
                    " - please use 0-9, A-Z and/or a-z. Optionally with period and/or underline inside.");
        }
    }

    private boolean validFormat(String str) {
        if (str == null || str.isEmpty()) return false;
        if (str.startsWith(".") || str.startsWith("_") ||
                str.contains("..") || str.endsWith(".") || str.endsWith("_")) return false;
        for (int i=0; i < str.length(); i++) {
            if (!((str.charAt(i) >= '0' && str.charAt(i) <= '9') ||
                    (str.charAt(i) >= 'a' && str.charAt(i) <= 'z') ||
                    (str.charAt(i) >= 'A' && str.charAt(i) <= 'Z') ||
                    (str.charAt(i) == '.' || str.charAt(i) == '_'))) {
                return false;
            }
        }
        return true;
    }
}
