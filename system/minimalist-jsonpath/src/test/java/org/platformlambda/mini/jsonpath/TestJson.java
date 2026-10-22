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

package org.platformlambda.mini.jsonpath;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small JSON reader for the tests, so the module needs no JSON library: objects read as {@link LinkedHashMap}
 * (member order kept), arrays as {@link ArrayList}, integers as {@link Long} (a {@link BigInteger} beyond its
 * range), other numbers as {@link Double}, or a {@link BigDecimal} when a double cannot hold them - the number types
 * Mercury's own trees carry.
 */
final class TestJson {
    private final String text;
    private int pos;

    private TestJson(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        TestJson reader = new TestJson(text);
        reader.blank();
        Object value = reader.value();
        reader.blank();
        if (reader.pos != text.length()) {
            throw new IllegalArgumentException("trailing text at " + reader.pos);
        }
        return value;
    }

    private Object value() {
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> word("true", Boolean.TRUE);
            case 'f' -> word("false", Boolean.FALSE);
            case 'n' -> word("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        blank();
        if (text.charAt(pos) == '}') {
            pos++;
            return map;
        }
        while (true) {
            blank();
            String key = string();
            blank();
            expect(':');
            blank();
            map.put(key, value());
            blank();
            if (text.charAt(pos) == '}') {
                pos++;
                return map;
            }
            expect(',');
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        pos++;
        blank();
        if (text.charAt(pos) == ']') {
            pos++;
            return list;
        }
        while (true) {
            blank();
            list.add(value());
            blank();
            if (text.charAt(pos) == ']') {
                pos++;
                return list;
            }
            expect(',');
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = text.charAt(pos++);
            switch (e) {
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> sb.append(e);
            }
        }
    }

    private Object number() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        String number = text.substring(start, pos);
        if (number.isEmpty()) {
            throw new IllegalArgumentException("unexpected '" + text.charAt(pos) + "' at " + pos);
        }
        if (number.indexOf('.') < 0 && number.indexOf('e') < 0 && number.indexOf('E') < 0) {
            BigInteger integer = new BigInteger(number);
            return integer.bitLength() < 64 ? (Object) integer.longValue() : integer;
        }
        double d = Double.parseDouble(number);
        return Double.isFinite(d) ? (Object) d : new BigDecimal(number);
    }

    private Object word(String word, Object value) {
        if (!text.startsWith(word, pos)) {
            throw new IllegalArgumentException("unexpected text at " + pos);
        }
        pos += word.length();
        return value;
    }

    private void expect(char c) {
        if (text.charAt(pos) != c) {
            throw new IllegalArgumentException("expected '" + c + "' at " + pos);
        }
        pos++;
    }

    private void blank() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }
}
