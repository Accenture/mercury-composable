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

package org.platformlambda.core.serializers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SimpleXmlWriter {

    private static final String SPACES = "  ";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_INSTANT;

    private enum TagType {
        START, BODY, END
    }

    public String write(Object map) {
        String className = map.getClass().getSimpleName();
        // className hierarchy filtering: dot for subclass and dollar-sign for nested class
        String root = "HashMap".equals(className) ? "root" : className;
        return write(root.toLowerCase(), map);
    }

    @SuppressWarnings("unchecked")
    public String write(String rootName, Object map) {
        if (map instanceof Map) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            write(buffer, rootName, (Map<String, Object>) map, 0);
            return buffer.toString();

        } else {
            throw new IllegalArgumentException("Input should be a Map object");
        }
    }

    private void write(StringBuilder buffer, String nodeName, Map<String, Object> map, int indent) {
        int currentIndent = indent;
        if (nodeName != null) {
            // Add one indent unit for the startTag
            indentBlock(buffer, currentIndent);
            buffer.append('<');
            buffer.append(escapeXml(nodeName, TagType.START));
            buffer.append('>');
            indent++;
        }
        // Next line after a startTag of a map or after a block of elements
        buffer.append('\n');
        List<String> keys = new ArrayList<>(map.keySet());
        // Arrange the map element in ascending order
        if (keys.size() > 1) {
            Collections.sort(keys);
        }
        for (String k: keys) {
            Object o = map.get(k);
            appendNode(buffer, k, o, indent);
        }
        // Go back one indent unit for the endTag
        // No need to add new line as the element block has already added one.
        indentBlock(buffer, currentIndent-1);
        if (nodeName != null) {
            buffer.append("</");
            buffer.append(escapeXml(nodeName, TagType.END));
            buffer.append('>');
        }
    }

    @SuppressWarnings({"unchecked"})
    private void appendNode(StringBuilder buffer, String nodeName, Object value, int indent) {
        // Skip null value
        if (value == null) {
            return;
        }
        if (value instanceof List<?> items) {
            // To preserve original sequence, DO NOT sort list elements
            for (Object object: items) {
                appendNode(buffer, nodeName, object, indent);
            }
        } else {
            // Add one indent unit for the startTag
            indentBlock(buffer, indent);
            buffer.append('<');
            buffer.append(escapeXml(nodeName, TagType.START));
            buffer.append('>');
            switch (value) {
                case Date d -> {
                    long ms = d.getTime();
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.of("UTC"));
                    buffer.append(zdt.format(ISO_DATE));
                }
                case Map<?, ?> map -> write(buffer, null, (Map<String, Object>) map, indent + 1);
                case String str -> buffer.append(escapeXml(str, TagType.BODY));
                case BigDecimal bDecimal -> buffer.append(bDecimal.toPlainString());
                default -> buffer.append(escapeXml(value.toString(), TagType.BODY));
            }
            buffer.append("</");
            buffer.append(escapeXml(nodeName, TagType.END));
            buffer.append('>');
            // Next line after the endTag
            buffer.append('\n');
        }
    }

    private void indentBlock(StringBuilder buffer, int indent) {
        buffer.append(SPACES.repeat(Math.max(0, indent)));
    }

    private String escapeXml(String value, TagType type) {
        return switch (type) {
            case START -> {
                if (value.startsWith("/") || value.startsWith("{")) {
                    yield "node value=\"" + value + "\"";
                }
                yield safeXmlKey(value);
            }
            case END -> {
                if (value.startsWith("/") || value.startsWith("{")) {
                    yield "node";
                }
                yield safeXmlKey(value);
            }
            default -> escapeHtml(value);
        };
    }

    private String escapeHtml(String text) {
        if (text.contains("&")) {
            text = text.replace("&", "&amp;");
        }
        if (text.contains("'")) {
            text = text.replace("'", "&apos;");
        }
        if (text.contains("\"")) {
            text = text.replace("\"", "&quot;");
        }
        if (text.contains(">")) {
            text = text.replace(">", "&gt;");
        }
        if (text.contains("<")) {
            text = text.replace("<", "&lt;");
        }
        return text;
    }

    private String safeXmlKey(String str) {
        if (validKey(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private boolean validKey(String str) {
        for (int i=0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!((c >= '0' && c <= '9') ||  (c >= 'a' && c <= 'z') ||  (c >= 'A' && c <= 'Z') || c == '-')) {
                return false;
            }
        }
        return true;
    }
}
