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

package com.accenture.automation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Event Script lowercases an {@code input.header.*} reference, which matches the HTTP adapter
 * because that adapter ingests headers lowercased. The <b>Kafka</b> flow adapter delivers record
 * headers in their original wire casing, so a producer-sent {@code Content-Type} could not be
 * addressed by ANY mapping - the header was unreachable rather than merely awkward.
 *
 * <p>The fix is a case-insensitive lookup rather than lowercasing at the Kafka adapter: normalizing
 * there would change what the {@code *} whole-body passthrough hands a function and break flows
 * matching exact casing today.
 */
class HeaderCaseInsensitiveLookupTest {

    /** Headers as the Kafka adapter delivers them - original wire casing, insertion-ordered. */
    private static Map<String, Object> wireHeaders() {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Correlation-Id", "abc-123");
        headers.put("already-lower", "value");
        return headers;
    }

    @Test
    void findsAMixedCaseHeaderFromTheLowercasedReference() {
        // the reference arrives lowercased by TaskExecutor; the wire key is not
        assertEquals("application/json", TaskExecutor.headerIgnoringCase(wireHeaders(), "content-type"));
        assertEquals("abc-123", TaskExecutor.headerIgnoringCase(wireHeaders(), "x-correlation-id"));
    }

    @Test
    void stillFindsAHeaderThatWasAlreadyLowercase() {
        // the HTTP path must keep working - it is the same scan when the direct lookup misses
        assertEquals("value", TaskExecutor.headerIgnoringCase(wireHeaders(), "already-lower"));
    }

    @Test
    void returnsNullWhenTheHeaderIsGenuinelyAbsent() {
        // absence must stay absence - the scan must not invent a value
        assertNull(TaskExecutor.headerIgnoringCase(wireHeaders(), "x-not-sent"));
        assertNull(TaskExecutor.headerIgnoringCase(Map.of(), "content-type"));
    }
}
