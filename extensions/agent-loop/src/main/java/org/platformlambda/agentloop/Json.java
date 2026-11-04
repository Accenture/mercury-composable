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

package org.platformlambda.agentloop;

import java.util.List;
import java.util.Map;

/**
 * Compact JSON text, written the way the language packs write a tool result ({@code separators=(",", ":")},
 * non-ASCII characters as they are): a map's keys in the order the map gives them, and a number as both engines
 * print it (no exponent, no trailing zeros). The loop uses it to measure a result against {@code result_limit},
 * to cut one, and to quote a value in a message.
 */
final class Json {

    private Json() { }

    static String compact(Object value) {
        var sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        switch (value) {
            case null -> sb.append("null");
            case Boolean b -> sb.append(b);
            case Number n -> sb.append(ToolSchema.render(ToolSchema.toDecimal(n)));
            case String s -> quote(sb, s);
            case Map<?, ?> map -> {
                sb.append('{');
                boolean first = true;
                for (var entry : map.entrySet()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;
                    quote(sb, String.valueOf(entry.getKey()));
                    sb.append(':');
                    write(sb, entry.getValue());
                }
                sb.append('}');
            }
            case List<?> list -> {
                sb.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    write(sb, list.get(i));
                }
                sb.append(']');
            }
            default -> quote(sb, value.toString());
        }
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
