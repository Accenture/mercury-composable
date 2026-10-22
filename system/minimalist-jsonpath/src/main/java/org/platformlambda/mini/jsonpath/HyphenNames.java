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

/**
 * Mercury's one declared extension of RFC 9535: a dot-notation member name may contain a hyphen, because graph node
 * names carry hyphens. The rewrite is the Rust engine's {@code bracketize_lenient_segments} (crate event-script,
 * {@code mlm.rs}), step for step, so both engines accept the same queries: outside brackets and quotes, a name after
 * '.' or '..' made of alphanumerics, '_' and '-' that contains a hyphen becomes a bracketed name -
 * {@code $.fetcher-ext.result} becomes {@code $['fetcher-ext'].result}. Like the Rust rewrite, the quote tracking
 * does not read escapes.
 */
final class HyphenNames {

    private HyphenNames() { }

    static String bracketize(String path) {
        int[] chars = path.codePoints().toArray();
        StringBuilder out = new StringBuilder(path.length() + 8);
        int inBrackets = 0;
        int inQuote = -1;
        int i = 0;
        while (i < chars.length) {
            int c = chars[i];
            if (inQuote >= 0) {
                out.appendCodePoint(c);
                if (c == inQuote) {
                    inQuote = -1;
                }
                i++;
            } else if (c == '.' && inBrackets == 0) {
                i = rewriteName(chars, i, out);
            } else {
                if (c == '\'' || c == '"') {
                    inQuote = c;
                } else if (c == '[') {
                    inBrackets++;
                } else if (c == ']') {
                    inBrackets = Math.max(0, inBrackets - 1);
                }
                out.appendCodePoint(c);
                i++;
            }
        }
        return out.toString();
    }

    // the name after '.' or '..' at chars[i]; returns the index after it
    private static int rewriteName(int[] chars, int i, StringBuilder out) {
        boolean descendant = i + 1 < chars.length && chars[i + 1] == '.';
        int start = descendant ? i + 2 : i + 1;
        int end = start;
        while (end < chars.length && (isAlphanumeric(chars[end]) || chars[end] == '_' || chars[end] == '-')) {
            end++;
        }
        String name = new String(chars, start, end - start);
        if (name.indexOf('-') >= 0) {
            out.append(descendant ? ".." : "").append("['").append(name).append("']");
        } else {
            out.append(descendant ? ".." : ".").append(name);
        }
        return end;
    }

    // Rust's char::is_alphanumeric: the Alphabetic property, or a numeric general category (Nd, Nl, No)
    private static boolean isAlphanumeric(int c) {
        if (Character.isAlphabetic(c)) {
            return true;
        }
        int type = Character.getType(c);
        return type == Character.DECIMAL_DIGIT_NUMBER || type == Character.LETTER_NUMBER ||
                type == Character.OTHER_NUMBER;
    }
}
