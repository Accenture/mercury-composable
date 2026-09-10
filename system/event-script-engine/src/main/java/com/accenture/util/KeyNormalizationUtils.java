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

package com.accenture.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Key normalization for the f:camelCase and f:snakeCase simple plugins.
 * <p>
 * Legacy systems (often XML-to-JSON transformations) deliver maps whose key formats vary
 * per source - MyExampleKey, My_Example_key, my_example_Key. Both plugins segmentize each
 * key and re-case the segments, recursively through nested maps and lists (values are
 * never touched; only keys).
 * <p>
 * Segmentation: underscore, hyphen and dot are separators; a lower-case-or-digit to
 * upper-case transition starts a new segment; an upper-case run followed by a lower-case
 * letter splits before its last upper-case letter (the acronym rule: XMLKey becomes
 * XML + Key, so myXMLKey normalizes to myXmlKey / my_xml_key). Digits ride with their
 * segment (address1Line stays address1 + Line). Empty segments are dropped; a key with
 * no segments at all (e.g. "___") is kept as-is.
 * <p>
 * Two distinct source keys can normalize to the same target (MyKey and my_key both
 * become myKey) - the later entry in map order wins, deterministically, at the first
 * key's position. Normalization is idempotent. The Rust engine ships the identical
 * algorithm and error messages (portable-flow contract).
 */
public class KeyNormalizationUtils {

    private KeyNormalizationUtils() {}

    /**
     * Validate the plugin argument and normalize all keys recursively.
     *
     * @param input the plugin's argument list - exactly one Map or List
     * @param plugin the plugin name (camelCase or snakeCase), for error messages
     * @param keyMapper the per-key normalizer
     * @return a new Map or List with normalized keys at every depth
     */
    public static Object normalize(Object[] input, String plugin, UnaryOperator<String> keyMapper) {
        if (input == null || input.length != 1) {
            throw new IllegalArgumentException("One input is required for " + plugin + " key normalization");
        }
        var value = input[0];
        if (value == null) {
            throw new IllegalArgumentException("Input cannot be null for " + plugin + " key normalization");
        }
        if (!(value instanceof Map) && !(value instanceof List)) {
            throw new IllegalArgumentException("Input must be a map or a list for " + plugin + " key normalization");
        }
        return normalizeKeys(value, keyMapper);
    }

    private static Object normalizeKeys(Object value, UnaryOperator<String> keyMapper) {
        if (value instanceof Map<?, ?> map) {
            var normalized = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                normalized.put(keyMapper.apply(String.valueOf(entry.getKey())),
                        normalizeKeys(entry.getValue(), keyMapper));
            }
            return normalized;
        }
        if (value instanceof List<?> list) {
            var normalized = new ArrayList<Object>(list.size());
            for (Object item : list) {
                normalized.add(normalizeKeys(item, keyMapper));
            }
            return normalized;
        }
        return value;
    }

    /**
     * Normalize one key to camelCase.
     *
     * @param key the source key in any format
     * @return the camelCase form, or the key unchanged when it has no segments
     */
    public static String toCamelCase(String key) {
        var segments = segments(key);
        if (segments.isEmpty()) {
            return key;
        }
        var sb = new StringBuilder(segments.getFirst().toLowerCase(Locale.ROOT));
        for (int i = 1; i < segments.size(); i++) {
            var segment = segments.get(i).toLowerCase(Locale.ROOT);
            sb.append(Character.toUpperCase(segment.charAt(0))).append(segment, 1, segment.length());
        }
        return sb.toString();
    }

    /**
     * Normalize one key to snake_case.
     *
     * @param key the source key in any format
     * @return the snake_case form, or the key unchanged when it has no segments
     */
    public static String toSnakeCase(String key) {
        var segments = segments(key);
        if (segments.isEmpty()) {
            return key;
        }
        return String.join("_", segments.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList());
    }

    private static List<String> segments(String key) {
        var segments = new ArrayList<String>();
        var current = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
                flush(segments, current);
                continue;
            }
            if (!current.isEmpty()) {
                char previous = current.charAt(current.length() - 1);
                boolean lowerToUpper = (Character.isLowerCase(previous) || Character.isDigit(previous))
                        && Character.isUpperCase(c);
                // an upper-case run followed by a lower-case letter belongs to the next
                // segment from its last upper-case letter (XMLKey -> XML + Key)
                boolean acronymEnd = Character.isUpperCase(previous) && Character.isUpperCase(c)
                        && i + 1 < key.length() && Character.isLowerCase(key.charAt(i + 1));
                if (lowerToUpper || acronymEnd) {
                    flush(segments, current);
                }
            }
            current.append(c);
        }
        flush(segments, current);
        return segments;
    }

    private static void flush(List<String> segments, StringBuilder current) {
        if (!current.isEmpty()) {
            segments.add(current.toString());
            current.setLength(0);
        }
    }
}
