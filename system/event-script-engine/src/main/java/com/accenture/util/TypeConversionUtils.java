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

import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TypeConversionUtils {
    private static final Utility util = Utility.getInstance();

    public static String getUUID(){
        return util.getUuid4();
    }

    public static Boolean isBoolean(Object input){
        if(input == null){
            return false;
        }
        return switch (input) {
            case Boolean i -> true;
            case String s -> s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");
            default -> false;
        };
    }

    public static Boolean convertBoolean(Object input){
        return switch (input) {
            case Boolean i -> i;
            case String s -> Boolean.parseBoolean(s);
            default -> throw new IllegalArgumentException("Cannot convert input to boolean: " + input);
        };
    }

    public static boolean getBooleanValue(Object value, String command) {
        List<String> parts = util.split(command, ",=");
        List<String> filtered = new ArrayList<>();
        parts.forEach(d -> {
            var txt = d.trim();
            if (!txt.isEmpty()) {
                filtered.add(txt);
            }
        });
        if (!filtered.isEmpty() && filtered.size() < 3) {
            // Enforce value to a text string where null value will become "null".
            // Therefore, null value or "null" string in the command is treated as the same.
            String str = String.valueOf(value);
            boolean condition = filtered.size() == 1 || "True".equalsIgnoreCase(filtered.get(1));
            String target = filtered.getFirst();
            if (str.equals(target)) {
                return condition;
            } else {
                return !condition;
            }
        } else {
            throw new IllegalArgumentException("invalid syntax - got command: " + command);
        }
    }

    public static Double convertDouble(Object input){
        return switch (input) {
            case Double i -> i;
            case String s -> Double.parseDouble(s);
            default -> throw new IllegalArgumentException("Cannot convert input to double: " + input);
        };
    }

    public static Integer convertInteger(Object input){
        return switch (input){
            case Integer i -> i;
            case String s -> Integer.parseInt(s);
            default -> throw new IllegalArgumentException("Cannot convert input to integer: " + input);
        };
    }

    public static Long convertLong(Object input){
        return switch (input) {
            case Long i -> i;
            case String s -> Long.parseLong(s);
            default -> throw new IllegalArgumentException("Cannot convert input to long: " + input);
        };
    }

    public static Float convertFloat(Object input){
        return switch (input) {
            case Float i -> i;
            case String s -> Float.parseFloat(s);
            default -> throw new IllegalArgumentException("Cannot convert input to float: " + input);
        };
    }

    public static String getTextValue(Object value) {
        return switch (value) {
            case String str -> str;
            case byte[] b -> util.getUTF(b);
            case Map<?, ?> map -> SimpleMapper.getInstance().getMapper().writeValueAsString(map);
            default -> String.valueOf(value);
        };
    }

    public static byte[] getBinaryValue(Object value) {
        return switch (value) {
            case byte[] b -> b;
            case String str -> util.getUTF(str);
            case Map<?, ?> map -> SimpleMapper.getInstance().getMapper().writeValueAsBytes(map);
            default -> util.getUTF(String.valueOf(value));
        };
    }

    public static int getLength(Object value) {
        return switch (value) {
            case null -> 0;
            case byte[] b -> b.length;
            case String str -> str.length();
            case List<?> item -> item.size();
            default -> String.valueOf(value).length();
        };
    }

    public static Object getB64(Object value) {
        if (value instanceof byte[] b) {
            return util.bytesToBase64(b);
        } else if (value instanceof String str) {
            try {
                return util.base64ToBytes(str);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid base64 text");
            }
        }
        return value;
    }

    public static Map<String, Object> deepCopy(Map<String, Object> value) {
        return util.deepCopy(value);
    }

    public static List<Object> deepCopy(List<Object> value) {
        return util.deepCopy(value);
    }

    /* Following methods are reserved for ListOfMap() and UpdateListOfMap() plugins */

    /**
     * Find the first occurrence of a map containing a list
     * (the input is usually result from a JSON-Path search)
     * @param input of list elements
     * @return closest map of list elements
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> findMapOfLists(Map<String, Object> input) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getValue() instanceof List) {
                return input;
            }
            if (entry.getValue() instanceof Map<?, ?> inner) {
                return findMapOfLists((Map<String, Object>) inner);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Prepare a list of Maps that contains the inner map containing list elements
     *
     * @param input of objects to the ListOfMap or UpdateListOfMap plugins
     * @return list of maps excluding the first argument
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> prepareMerge(Object[] input) {
        var size = ((List<Object>) input[0]).size();
        var subsequentOnes = new ArrayList<Map<String, Object>>();
        for (int i = 1; i < input.length; i++) {
            if (input[i] instanceof Map<?,?> data) {
                var map = TypeConversionUtils.findMapOfLists((Map<String, Object>) data);
                if (!map.isEmpty()) {
                    if (validDataStructure(map, size)) {
                        subsequentOnes.add(map);
                    } else {
                        return Collections.emptyList();
                    }
                }
            }
        }
        return subsequentOnes;
    }

    private static boolean validDataStructure(Map<String, Object> map, int size) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof List<?> list) {
                if (list.size() != size) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Merge a list of "maps of list elements" with the first argument
     *
     * @param first argument that is a list of maps
     * @param additional maps of list elements
     * @return merged list
     */
    @SuppressWarnings("unchecked")
    public static Object merge(List<Object> first, List<Map<String, Object>> additional) {
        var size = first.size();
        var copy = util.deepCopy(first);
        for (int i = 0; i < size; i++) {
            var baseMap = (Map<String, Object>) copy.get(i);
            for (Map<String, Object> additionalMap : additional) {
                for (Map.Entry<String, Object> entry : additionalMap.entrySet()) {
                    var key = String.valueOf(entry.getKey());
                    var listValue = (List<Object>) entry.getValue();
                    if (size == listValue.size()) {
                        baseMap.put(key, listValue.get(i));
                    }
                }
            }
        }
        return copy;
    }

    /**
     * Validate and return the size of the first argument
     *
     * @param first argument that is a list of maps
     * @return true if valid
     */
    public static boolean validateFirstArgument(Object first) {
        if (first instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (!(item instanceof Map)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
