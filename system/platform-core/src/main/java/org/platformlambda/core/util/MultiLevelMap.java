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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiLevelMap {
    private static final NotFound NOT_FOUND = NotFound.of();
    private final Map<String, Object> multiLevels = new HashMap<>();

    /**
     * Create an empty multi-level map
     */
    public MultiLevelMap() { }

    /**
     * Create a multi-level map from a hashmap
     *
     * @param map of key-values
     */
    public MultiLevelMap(Map<String, Object> map) {
        this.multiLevels.putAll(map);
    }

    /**
     * Reload key-values from a different map
     *
     * @param map of key-values
     */
    public void reload(Map<String, Object> map) {
        this.multiLevels.clear();
        this.multiLevels.putAll(map);
    }

    /**
     * Retrieve the underlying map of key-values
     *
     * @return map
     */
    public Map<String, Object> getMap() {
        return multiLevels;
    }

    /**
     * Check if map is empty
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return multiLevels.isEmpty();
    }

    /**
     * Check if a key-value exists
     *
     * @param compositePath in dot-bracket format
     *
     * @return true if exists
     */
    public boolean exists(String compositePath) {
        Object element = getElement(compositePath, multiLevels);
        return element != null && !(element instanceof NotFound);
    }

    /**
     * Check if a key exists where the value can be empty
     *
     * @param compositePath in dot-bracket format
     *
     * @return true if exists
     */
    public boolean keyExists(String compositePath) {
        Object element = getElement(compositePath, multiLevels);
        return !(element instanceof NotFound);
    }

    /**
     * Retrieve an element from a map using a composite path
     * (Nested array is supported)
     * e.g. "some.key", "some.array[3]", "hello.world[2][10][1]"
     *
     * @param compositePath using dot-bracket convention
     * @return element
     */
    public Object getElement(String compositePath) {
        Object element = getElement(compositePath, multiLevels);
        return element instanceof NotFound? null : element;
    }

    /**
     * Retrieve an element from a map using a composite path, given a default value
     * <p>
     * (Nested array is supported)
     * e.g. "some.key", "some.array[3]", "hello.world[2][10][1]"
     *
     * @param compositePath using dot-bracket convention
     * @param defaultValue if key does not exist
     * @return element
     */
    public Object getElement(String compositePath, Object defaultValue) {
        Object element = getElement(compositePath);
        return element == null? defaultValue : element;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getElements(String compositePath) {
        if (compositePath == null || compositePath.isEmpty()) {
            return new ArrayList<>();
        }

        validateCompositePathSyntax(compositePath);

        int wildcardIndex = compositePath.indexOf("[]");
        if (wildcardIndex == -1) {
            // if there is no [] in the path, error out
            throw new IllegalArgumentException("Invalid composite path - missing index segment");
        }

        // Split the path using [] as the separator
        String basePath = compositePath.substring(0, wildcardIndex);
        String remainingPath = compositePath.substring(wildcardIndex + 2); // skip "[]"
        if (remainingPath.startsWith(".")) {
            remainingPath = remainingPath.substring(1);
        }

        Object baseArray = getElement(basePath);
        if (!(baseArray instanceof List)) {
            return new ArrayList<>();
        }

        List<Object> array = (List<Object>) baseArray;
        List<Object> result = new ArrayList<>();

        // iterate through the list
        for (int i = 0; i < array.size(); i++) {
            String elementPath;
            if (remainingPath.isEmpty()) {
                // If there is no remaining path, build the element path directly from the base path and index.
                elementPath = basePath + "[" + i + "]";
            } else {
                // Otherwise build the element path by concatenating the base path, index, and remaining path.
                elementPath = basePath + "[" + i + "]." + remainingPath;
            }

            Object element = getElement(elementPath);
            if (element != null) {
                result.add(element);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Object getListElement(List<Integer> indexes, List<Object> data) {
        List<Object> current = data;
        int n = 0;
        int len = indexes.size();
        for (Integer i: indexes) {
            n++;
            if (i < 0 || i >= current.size()) {
                return NOT_FOUND;
            }
            Object o = current.get(i);
            if (n == len) {
                return o;
            }
            if (o instanceof List) {
                current = (List<Object>) o;
            } else {
                return NOT_FOUND;
            }
        }
        return NOT_FOUND;
    }

    private Object getElement(String path, Map<String, Object> map) {
        if (path == null || map == null || map.isEmpty()) return NOT_FOUND;
        if (map.containsKey(path)) {
            return map.get(path);
        }
        if (!isComposite(path)) {
            return NOT_FOUND;
        }
        Utility util = Utility.getInstance();
        List<String> list = util.split(path, ".");
        CurrentMap current = new CurrentMap(map);
        int len = list.size();
        int n = 0;
        for (String p: list) {
            n++;
            var element = isListElement(p)? getListElementFromCurrent(p, current, n, len) :
                                            getNonListElementFromCurrent(p, current, n, len);
            if (!(element instanceof ToBeContinued)) {
                return element;
            }
        }
        return NOT_FOUND;
    }

    @SuppressWarnings("unchecked")
    private Object getListElementFromCurrent(String p, CurrentMap current, int n, int len) {
        var util = Utility.getInstance();
        int start = p.indexOf('[');
        int end = p.indexOf(']', start);
        if (end == -1) return NOT_FOUND;
        String key = p.substring(0, start);
        String index = p.substring(start+1, end).trim();
        if (index.isEmpty() || !util.isDigits(index)) return NOT_FOUND;
        if (current.map.containsKey(key)) {
            Object nextList = current.map.get(key);
            if (nextList instanceof List) {
                List<Integer> indexes = getIndexes(p.substring(start));
                Object next = getListElement(indexes, (List<Object>) nextList);
                if (n == len) {
                    return next;
                }
                if (next instanceof Map) {
                    current.map = (Map<String, Object>) next;
                    return ToBeContinued.of();
                }
            }
        }
        return NOT_FOUND;
    }

    @SuppressWarnings("unchecked")
    private Object getNonListElementFromCurrent(String p, CurrentMap current, int n, int len) {
        if (current.map.containsKey(p)) {
            Object next = current.map.get(p);
            if (n == len) {
                return next;
            } else if (next instanceof Map) {
                current.map = (Map<String, Object>) next;
                return ToBeContinued.of();
            }
        }
        return NOT_FOUND;
    }

    /**
     * Set a key-value
     * <p>
     * Note: you may append a new list element using the empty index syntax.
     *  e.g. this will set element "hello.world[0]" automatically:
     * <p>
     *          setElement("hello.world[]", "test");
     *
     * @param compositePath using dot-bracket format
     * @param value to be inserted
     * @return this
     */
    public MultiLevelMap setElement(String compositePath, Object value) {
        validateCompositePathSyntax(compositePath);
        var normalizedPath = compositePath.contains("[]")? appendIndex(compositePath) : compositePath;
        setElement(normalizedPath, value, multiLevels, false);
        return this;
    }

    private String appendIndex(String compositePath) {
        int emptyIndex = compositePath.indexOf("[]");
        if (emptyIndex != -1) {
            String parent = compositePath.substring(0, emptyIndex);
            var result = compositePath.substring(0, emptyIndex) + "[" + findLastIndex(parent) + "]" +
                            compositePath.substring(emptyIndex+2);
            return appendIndex(result);
        }
        return compositePath;
    }

    @SuppressWarnings("unchecked")
    private int findLastIndex(String key) {
        if (keyExists(key + "[0]")) {
            return ((List<Object>) getElement(key)).size();
        } else {
            return 0;
        }
    }

    /**
     * Remove a key-value
     *
     * @param compositePath using dot-bracket format
     * @return this
     */
    public MultiLevelMap removeElement(String compositePath) {
        if (keyExists(compositePath)) {
            setElement(compositePath, null, multiLevels, true);
        }
        return this;
    }

    private void setElement(String path, Object value, Map<String, Object> map, boolean delete) {
        Utility util = Utility.getInstance();
        List<String> segments = util.split(path, ".");
        if (segments.isEmpty()) {
            return;
        }
        CurrentMap current = new CurrentMap(map);
        int len = segments.size();
        int n = 0;
        // reconstruct the composite as we walk the path segments
        StringBuilder composite = new StringBuilder();
        for (String p : segments) {
            n++;
            if (isListElement(p)) {
                int sep = p.indexOf('[');
                List<Integer> indexes = getIndexes(p.substring(sep));
                String element = p.substring(0, sep);
                Object parent = getElement(composite+element, map);
                if (n == len) {
                    setCurrentElement(element, value, current, parent, indexes);
                    return;
                } else {
                    walkOneElement(composite+p, element, current, parent, indexes, map);
                }
            } else {
                if (walkNonListElement(p, value, current, n, len, delete)) {
                    return;
                }
            }
            composite.append(p).append('.');
        }
    }

    @SuppressWarnings("unchecked")
    private void setCurrentElement(String element, Object value, CurrentMap current, Object parent, List<Integer> indexes) {
        if (parent instanceof List) {
            setListElement(indexes, (List<Object>) parent, value);
        } else {
            List<Object> newList = new ArrayList<>();
            setListElement(indexes, newList, value);
            current.map.put(element, newList);
        }
    }

    @SuppressWarnings("unchecked")
    private void walkOneElement(String compositePath, String element, CurrentMap current, Object parent,
                                List<Integer> indexes, Map<String, Object> map) {
        if (parent instanceof List) {
            Object next = getElement(compositePath, map);
            if (next instanceof Map) {
                current.map = (Map<String, Object>) next;
            } else {
                Map<String, Object> m = new HashMap<>();
                setListElement(indexes, (List<Object>) parent, m);
                current.map = m;
            }
        } else {
            Map<String, Object> nextMap = new HashMap<>();
            List<Object> newList = new ArrayList<>();
            setListElement(indexes, newList, nextMap);
            current.map.put(element, newList);
            current.map = nextMap;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean walkNonListElement(String p, Object value, CurrentMap current, int n, int len, boolean delete) {
        if (n == len) {
            if (value == null && delete) {
                current.map.remove(p);
            } else {
                current.map.put(p, value);
            }
            return true;
        } else {
            Object next = current.map.get(p);
            if (next instanceof Map) {
                current.map = (Map<String, Object>) next;
            } else {
                Map<String, Object> nextMap = new HashMap<>();
                current.map.put(p, nextMap);
                current.map = nextMap;
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void setListElement(List<Integer> indexes, List<Object> dataset, Object value) {
        List<Object> current = expandList(indexes, dataset);
        int len = indexes.size();
        for (int i=0; i < len; i++) {
            int idx = indexes.get(i);
            if (i == len - 1) {
                current.set(idx, value);
            } else {
                Object o = current.get(idx);
                if (o instanceof List) {
                    current = (List<Object>) o;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> expandList(List<Integer> indexes, List<Object> dataset) {
        List<Object> current = dataset;
        int len = indexes.size();
        for (int i=0; i < len; i++) {
            int idx = indexes.get(i);
            if (idx >= current.size()) {
                int diff = idx - current.size();
                while (diff-- >= 0) {
                    current.add(null);
                }
            }
            if (i == len - 1) {
                break;
            }
            Object o = current.get(idx);
            if (o instanceof List) {
                current = (List<Object>) o;
            } else {
                List<Object> newList = new ArrayList<>();
                current.set(idx, newList);
                current = newList;
            }
        }
        return dataset;
    }

    private boolean isComposite(String item) {
        return item.contains(".") || item.contains("[") || item.contains("]");
    }

    private boolean isListElement(String item) {
        return (item.contains("[") && item.endsWith("]") && !item.startsWith("["));
    }

    private List<Integer> getIndexes(String indexSegment) {
        Utility util = Utility.getInstance();
        List<String> indexes = util.split(indexSegment, "[]");
        List<Integer> result = new ArrayList<>();
        for (String s: indexes) {
            result.add(util.str2int(s));
        }
        return result;
    }

    /**
     * Check if the composite path is in proper dot-bracket syntax
     *
     * @param path of the key
     * @throws IllegalArgumentException if invalid format
     */
    public void validateCompositePathSyntax(String path) {
        Utility util = Utility.getInstance();
        List<String> segments = util.split(path, ".");
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("Missing composite path");
        }
        if (segments.getFirst().trim().startsWith("[")) {
            throw new IllegalArgumentException("Invalid composite path - missing first element");
        }
        for (String s: segments) {
            if (s.contains("[") || s.contains("]")) {
                if (!s.contains("[")) {
                    throw new IllegalArgumentException("Invalid composite path - missing start bracket");
                }
                if (!s.endsWith("]")) {
                    throw new IllegalArgumentException("Invalid composite path - missing end bracket");
                }
                // check start-end pair
                int sep1 = s.indexOf('[');
                int sep2 = s.indexOf(']');
                if (sep2 < sep1) {
                    throw new IllegalArgumentException("Invalid composite path - missing start bracket");
                }
                validateOneSegmentOfPath(s, sep1);
            }
        }
    }

    private void validateOneSegmentOfPath(String s, int sep) {
        boolean start = false;
        for (char c: s.substring(sep).toCharArray()) {
            int n = validateCorrectBracketPair(c, start);
            if (n == 1) {
                start = true;
            } else if (n == 2) {
                start = false;
            } else if (start) {
                if (c < '0' || c > '9') {
                    throw new IllegalArgumentException("Invalid composite path - indexes must be digits");
                }
            } else {
                throw new IllegalArgumentException("Invalid composite path - invalid indexes");
            }
        }
    }

    private int validateCorrectBracketPair(char c, boolean start) {
        if (c == '[') {
            if (start) {
                throw new IllegalArgumentException("Invalid composite path - missing end bracket");
            } else {
                return 1;
            }
        } else if (c == ']') {
            if (!start) {
                throw new IllegalArgumentException("Invalid composite path - duplicated end bracket");
            } else {
                return 2;
            }
        }
        return 0;
    }

    private static class NotFound {
        private NotFound() { }

        private static NotFound of() {
            return new NotFound();
        }
    }

    private static class ToBeContinued {
        private ToBeContinued() { }

        private static ToBeContinued of() {
            return new ToBeContinued();
        }
    }

    private static class CurrentMap {
        Map<String, Object> map;

        public CurrentMap(Map<String, Object> map) {
            this.map = map;
        }
    }
}
