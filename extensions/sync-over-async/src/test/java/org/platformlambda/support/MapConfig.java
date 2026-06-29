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

package org.platformlambda.support;

import org.platformlambda.core.util.common.ConfigBase;

import java.util.Map;

/**
 * Minimal in-memory {@link ConfigBase} backed by a flat key→value map, for unit-testing the
 * {@code from(ConfigBase)} loaders without booting the real {@code AppConfigReader} (whose dotted-key
 * traversal does not resolve flat map keys).
 */
class MapConfig implements ConfigBase {

    private final Map<String, String> values;

    MapConfig(Map<String, String> values) {
        this.values = values;
    }

    @Override
    public String getProperty(String key) {
        return values.get(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = values.get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public Object get(String key) {
        return values.get(key);
    }

    @Override
    public Object get(String key, Object defaultValue, String... loop) {
        Object value = values.get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean exists(String key) {
        return values.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean isBaseConfig() {
        return false;
    }

    @Override
    public Map<String, Object> getMap() {
        return Map.of();
    }

    @Override
    public Map<String, Object> getCompositeKeyValues() {
        return Map.of();
    }
}
