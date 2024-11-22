/*

    Copyright 2018-2024 Accenture Technology

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

import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.common.ConfigBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConfigReader implements ConfigBase {
    private static final Logger log = LoggerFactory.getLogger(ConfigReader.class);

    private static final ConcurrentMap<String, List<String>> loopDetection = new ConcurrentHashMap<>();
    private static final String CLASSPATH = "classpath:";
    private static final String FILEPATH = "file:";
    private static final String JSON = ".json";
    private static final String YML = ".yml";
    private static final String YAML = ".yaml";
    private static final String DOT_PROPERTIES = ".properties";

    private static AppConfigReader baseConfig;
    private MultiLevelMap config = new MultiLevelMap();
    private final Map<String, Object> cachedFlatMap = new HashMap<>();

    /**
     * Set the base configuration reader (AppConfigReader)
     * Note that this is done automatically when your application starts.
     * You only need to set this when you are running unit tests for the
     * config reader without starting the platform module.
     *
     * @param config is the singleton AppConfigReader class
     */
    public static void setBaseConfig(AppConfigReader config) {
        if (ConfigReader.baseConfig == null) {
            ConfigReader.baseConfig = config;
        }
    }

    /**
     * Retrieve a parameter value by key
     * (Note that a parameter may be substituted by a system property,
     * an environment variable or another configuration parameter key-value
     * using the standard dot-bracket syntax)
     *
     * @param key of a configuration parameter
     * @return parameter value
     */
    @Override
    public Object get(String key) {
        return get(key, null);
    }

    private String getSystemProperty(String key) {
        if (key.isEmpty()) {
            return null;
        }
        return System.getProperty(key);
    }

    /**
     * Retrieve a parameter value by key, given a default value
     * <p>
     * 1. a parameter may be substituted by a system property,
     * an environment variable or another configuration parameter key-value
     * using the standard dot-bracket syntax
     * <p>
     * 2. the optional "loop" parameter should have zero or one element.
     * <p>
     * @param key of a configuration parameter
     * @param defaultValue if key does not exist
     * @param loop reserved for internal use to detect configuration loops
     * @return parameter value
     */
    @Override
    public Object get(String key, Object defaultValue, String... loop) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String systemProperty = getSystemProperty(key);
        if (systemProperty != null) {
            return systemProperty;
        }
        Object value = config.getElement(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String result && baseConfig != null) {
            // start parsing environment variables if it has the signature
            if (result.lastIndexOf("${") != -1) {
                List<EnvVarSegment> segments = extractSegments(result);
                // restore to original order since the text parsing is in reverse order
                Collections.reverse(segments);
                int start = 0;
                StringBuilder sb = new StringBuilder();
                for (EnvVarSegment s: segments) {
                    String middle = result.substring(s.start+2, s.end-1).trim();
                    String evaluated = performEnvVarSubstitution(key, middle, defaultValue, loop);
                    String heading = result.substring(start, s.start);
                    if (!heading.isEmpty()) {
                        sb.append(heading);
                    }
                    if (evaluated != null) {
                        sb.append(evaluated);
                    }
                    start = s.end;
                }
                String lastSegment = result.substring(start);
                if (!lastSegment.isEmpty()) {
                    sb.append(lastSegment);
                }
                return sb.isEmpty()? null : sb.toString();
            }
        }
        return value;
    }

    /**
     * Extract all the segments that contain environment variable references
     * @param original text of the key-value
     * @return list of segment pointers in reversed order
     */
    private List<EnvVarSegment> extractSegments(String original) {
        List<EnvVarSegment> result = new ArrayList<>();
        String text = original;
        while (true) {
            int bracketStart = text.lastIndexOf("${");
            int bracketEnd = text.lastIndexOf("}");
            if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {
                result.add(new EnvVarSegment(bracketStart, bracketEnd+1));
                text = original.substring(0, bracketStart);
            } else if (bracketStart != -1) {
                text = original.substring(0, bracketStart);
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Resolve environment variable for a key-value
     *
     * @param key of the property
     * @param text containing environment variable reference
     * @param defaultValue when property is not found
     * @param loop contains 0 or 1 elements
     * @return value with environment variable substitution
     */
    private String performEnvVarSubstitution(String key, String text, Object defaultValue, String... loop) {
        if (!text.isEmpty()) {
            String middleDefault = null;
            String loopId = loop.length == 1 && !loop[0].isEmpty() ? loop[0] : Utility.getInstance().getUuid();
            int colon = text.indexOf(':');
            if (colon > 0) {
                middleDefault = text.substring(colon+1);
                text = text.substring(0, colon);
            }
            String property = System.getenv(text);
            if (property != null) {
                text = property;
            } else {
                List<String> refs = loopDetection.getOrDefault(loopId, new ArrayList<>());
                if (refs.contains(text)) {
                    log.warn("Config loop for '{}' detected", key);
                    text = "";
                } else {
                    refs.add(text);
                    loopDetection.put(loopId, refs);
                    Object mid = baseConfig.get(text, defaultValue, loopId);
                    text = mid != null? String.valueOf(mid) : null;
                }
            }
            // this guarantees cleaning up temporary loop detection reference to avoid memory leak
            loopDetection.remove(loopId);
            return text != null ? text : middleDefault;
        } else {
            return defaultValue != null? String.valueOf(defaultValue) : null;
        }
    }

    /**
     * Retrieve a parameter value by key with return value enforced as a string
     *
     * @param key of a configuration parameter
     * @return parameter value as a string
     */
    @Override
    public String getProperty(String key) {
        Object o = get(key);
        return o != null? String.valueOf(o) : null;
    }

    /**
     * Retrieve a parameter value by key with return value enforced as a string, given a default value
     *
     * @param key of a configuration parameter
     * @param defaultValue if key does not exist
     * @return parameter value as a string
     */
    @Override
    public String getProperty(String key, String defaultValue) {
        String s = getProperty(key);
        return s != null? s : defaultValue;
    }

    /**
     * Retrieve the underlying map
     * (Note that this returns a raw map without value substitution)
     *
     * @return map of key-values
     */
    @Override
    public Map<String, Object> getMap() {
        return config.getMap();
    }

    /**
     * Retrieve a flat map of composite key-values
     * (Value substitution is automatically applied)
     *
     * @return flat map
     */
    @Override
    public Map<String, Object> getCompositeKeyValues() {
        if (cachedFlatMap.isEmpty()) {
            Map<String, Object> map = Utility.getInstance().getFlatMap(config.getMap());
            for (String key : map.keySet()) {
                cachedFlatMap.put(key, this.get(key));
            }
        }
        return cachedFlatMap;
    }

    /**
     * Check if a key exists
     *
     * @param key of a configuration parameter
     * @return true if key exists
     */
    @Override
    public boolean exists(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return config.exists(key);
    }

    /**
     * Check if the configuration file is empty
     *
     * @return true if empty
     */
    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    /**
     * Load a configuration file into a config reader
     *
     * @param path of the configuration file prefix with "classpath:/" or "file:/"
     * @throws IOException if file not found
     */
    @SuppressWarnings("unchecked")
    public void load(String path) throws IOException {
        boolean isYaml = path.endsWith(YML) || path.endsWith(YAML);
        // ".yaml" and ".yml" can be used interchangeably
        String alternativePath = null;
        if (isYaml) {
            String pathWithoutExt = path.substring(0, path.lastIndexOf('.'));
            alternativePath = path.endsWith(YML)? pathWithoutExt + YAML : pathWithoutExt + YML;
        }
        InputStream in = null;
        if (path.startsWith(FILEPATH)) {
            String filePath = path.substring(FILEPATH.length());
            File f = new File(filePath);
            try {
                if (f.exists()) {
                    in = Files.newInputStream(Paths.get(filePath));
                } else {
                    if (alternativePath != null) {
                        in = Files.newInputStream(Paths.get(alternativePath.substring(FILEPATH.length())));
                    }
                }
            } catch (IOException e) {
                // ok to ignore
            }
        } else {
            String resourcePath = path.startsWith(CLASSPATH)? path.substring(CLASSPATH.length()) : path;
            if (alternativePath != null && alternativePath.startsWith(CLASSPATH)) {
                alternativePath = alternativePath.substring(CLASSPATH.length());
            }
            in = ConfigReader.class.getResourceAsStream(resourcePath);
            if (in == null && alternativePath != null) {
                in = ConfigReader.class.getResourceAsStream(alternativePath);
            }
        }
        if (in == null) {
            throw new IOException(path + " not found");
        }
        try {
            if (isYaml) {
                Utility util = Utility.getInstance();
                Yaml yaml = new Yaml();
                String data = util.getUTF(util.stream2bytes(in, false));
                Map<String, Object> m = yaml.load(data.contains("\t")? data.replace("\t", "  ") : data);
                config = getMultiLevelMap(m);
            } else if (path.endsWith(JSON)) {
                Map<String, Object> m = SimpleMapper.getInstance().getMapper().readValue(in, Map.class);
                config = getMultiLevelMap(m);
            } else if (path.endsWith(DOT_PROPERTIES)) {
                config = new MultiLevelMap();
                Properties p = new Properties();
                p.load(in);
                Map<String, Object> map = new HashMap<>();
                p.forEach((k,v) -> map.put(String.valueOf(k), v));
                List<String> keys = new ArrayList<>(map.keySet());
                Collections.sort(keys);
                keys.forEach(k -> config.setElement(k, map.get(k)));
            }
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                //
            }
        }
    }

    /**
     * Normalized a raw map where the input map may be null
     *
     * @param raw input map
     * @return multi level map
     */
    private MultiLevelMap getMultiLevelMap(Map<String, Object> raw) {
        if (raw == null) {
            return new MultiLevelMap();
        } else {
            enforceKeysAsText(raw);
            return new MultiLevelMap(normalizeMap(raw));
        }
    }

    /**
     * Load a configuration file into a config reader
     *
     * @param map of key-values
     */
    public void load(Map<String, Object> map) {
        enforceKeysAsText(map);
        config = new MultiLevelMap(normalizeMap(map));
    }

    private Map<String, Object> normalizeMap(Map<String, Object> map) {
        Map<String, Object> flat = Utility.getInstance().getFlatMap(map);
        List<String> keys = new ArrayList<>(flat.keySet());
        Collections.sort(keys);
        MultiLevelMap multiMap = new MultiLevelMap();
        keys.forEach(k -> multiMap.setElement(k, flat.get(k)));
        return multiMap.getMap();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void enforceKeysAsText(Map raw) {
        Set keys = new HashSet(raw.keySet());
        for (Object k: keys) {
            Object v = raw.get(k);
            // key is assumed to be string
            if (!(k instanceof String)) {
                raw.remove(k);
                raw.put(String.valueOf(k), v);
            }
            if (v instanceof Map map) {
                enforceKeysAsText(map);
            }
        }
    }

    private record EnvVarSegment(int start, int end) { }
}
