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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * I-Regexp (RFC 9485) translated into java.util.regex: where the two read the same text differently, the
 * translation keeps I-Regexp's meaning, and a text outside I-Regexp's grammar is not a pattern at all.
 */
class IRegexpTest {

    private static boolean matches(String regex, String text) {
        Pattern p = IRegexp.compile(regex);
        assertNotNull(p, regex);
        return p.matcher(text).matches();
    }

    private static boolean finds(String regex, String text) {
        Pattern p = IRegexp.compile(regex);
        assertNotNull(p, regex);
        return p.matcher(text).find();
    }

    @Test
    void aDotMatchesAnythingButALineFeedOrACarriageReturn() {
        // Java's own '.' refuses these three
        assertTrue(matches(".", "\u0085"));
        assertTrue(matches(".", " "));
        assertTrue(matches(".", " "));
        assertFalse(matches(".", "\n"));
        assertFalse(matches(".", "\r"));
        // a supplementary character is one character
        assertTrue(matches(".", "😀"));
    }

    @Test
    void caretAndDollarAnchorTheWholeString() {
        assertTrue(finds("bc$", "abc"));
        // Java's '$' would also match before a final line terminator
        assertFalse(finds("bc$", "abc\n"));
        assertFalse(finds("^bc", "abc"));
        assertTrue(finds("^ab", "abc"));
    }

    @Test
    void literalsNeverTurnIntoJavaSyntax() {
        // '&&' is an intersection in a Java class, an ordinary pair in I-Regexp
        assertTrue(matches("[a&&b]", "&"));
        assertTrue(matches("[a-c-]", "-"));
        assertTrue(matches("[-a]", "-"));
        assertTrue(matches("[--]", "-"));
        assertTrue(matches("[^-]", "a"));
        assertTrue(matches("[\\--\\.]", "."));
        assertTrue(matches("a#b", "a#b"));
        assertTrue(matches("\\(\\)\\*\\+\\-\\.\\?\\[\\\\\\]\\^\\{\\|\\}", "()*+-.?[\\]^{|}"));
        assertTrue(matches("\\n\\r\\t", "\n\r\t"));
        assertTrue(matches("[\\n]", "\n"));
    }

    @Test
    void categoriesQuantifiersAndGroups() {
        assertTrue(matches("\\p{Lu}\\P{Lu}", "Ab"));
        assertTrue(matches("[\\p{Nd}x]+", "1x2"));
        assertTrue(matches("a{2}b{1,}c{0,1}", "aabbb"));
        assertTrue(matches("(ab|cd)*", "abcdab"));
        assertTrue(matches("a|", ""));
        assertTrue(matches("()", ""));
        assertTrue(matches("\\p{Cn}", "͸"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(", ")", "a)", "[", "[]", "[^]", "[a", "[a-", "[z-a]", "[\\p{L}-z]", "[a-\\p{L}]", "a**", "*a", "{1}",
            "a{", "a{x}", "a{2,1}", "a{1,2", "a{99999999999}", "\\", "\\d", "\\p", "\\p{", "\\p{Xx}", "\\p{L",
            "a\\", "[\\d]", "[[]", "[a-b-c]", "a{,2}"})
    void aTextOutsideTheGrammarIsNotAPattern(String regex) {
        assertNull(IRegexp.compile(regex), regex);
    }

    @Test
    void surrogatesAndDeepNestingAreRefused() {
        assertNull(IRegexp.compile("\uD800"));
        assertNull(IRegexp.compile("[\uD800]"));
        String deep = "(".repeat(65) + "a" + ")".repeat(65);
        assertNull(IRegexp.compile(deep));
        String allowed = "(".repeat(64) + "a" + ")".repeat(64);
        assertNotNull(IRegexp.compile(allowed));
    }

    @ParameterizedTest
    @CsvSource({"'a.c', 'abc', true", "'a.c', 'xabcx', false", "'b', 'abc', false", "'a+', 'aaa', true"})
    void matchIsAWholeStringMatch(String regex, String text, boolean expected) {
        assertEquals(expected, matches(regex, text));
    }
}
