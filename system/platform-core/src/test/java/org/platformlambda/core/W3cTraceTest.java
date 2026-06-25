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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.W3cTrace;

import static org.junit.jupiter.api.Assertions.*;

class W3cTraceTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String SPAN_ID = "00f067aa0ba902b7";

    @Test
    void buildsValidTraceparent() {
        // version 00 - trace-id - parent-id - sampled flag
        assertEquals("00-" + TRACE_ID + "-" + SPAN_ID + "-01",
                W3cTrace.traceparent(TRACE_ID, SPAN_ID));
    }

    @Test
    void roundTripsThroughParse() {
        var value = W3cTrace.traceparent(TRACE_ID, SPAN_ID);
        var parsed = W3cTrace.parse(value);
        assertNotNull(parsed);
        assertEquals(TRACE_ID, parsed[0]);
        assertEquals(SPAN_ID, parsed[1]);
    }

    @Test
    void mercuryGeneratedIdsAreW3cCompatible() {
        // Mercury's auto-generated trace ID (32 hex) and span ID (16 hex) must propagate as-is
        var traceId = Utility.getInstance().getUuid();
        var spanId = String.format("%016x", java.util.UUID.randomUUID().getLeastSignificantBits());
        var value = W3cTrace.traceparent(traceId, spanId);
        assertNotNull(value, "Mercury IDs should be W3C-compatible");
        var parsed = W3cTrace.parse(value);
        assertNotNull(parsed);
        assertEquals(traceId, parsed[0]);
        assertEquals(spanId, parsed[1]);
    }

    @Test
    void rejectsNonW3cIds() {
        // custom (non-hex / wrong-length) IDs such as those used in tests must not be emitted
        assertNull(W3cTrace.traceparent("ch-3", SPAN_ID));
        assertNull(W3cTrace.traceparent(TRACE_ID, "short"));
        assertNull(W3cTrace.traceparent(null, SPAN_ID));
        assertNull(W3cTrace.traceparent(TRACE_ID, null));
        // all-zero ids are invalid per the W3C spec
        assertNull(W3cTrace.traceparent("0".repeat(32), SPAN_ID));
        assertNull(W3cTrace.traceparent(TRACE_ID, "0".repeat(16)));
    }

    @Test
    void parseRejectsMalformedValues() {
        assertNull(W3cTrace.parse(null));
        assertNull(W3cTrace.parse(""));
        assertNull(W3cTrace.parse("not-a-traceparent"));
        assertNull(W3cTrace.parse("00-" + TRACE_ID + "-" + SPAN_ID)); // missing flags field
        assertNull(W3cTrace.parse("00-tooShortTraceId-" + SPAN_ID + "-01"));
        assertNull(W3cTrace.parse("00-" + TRACE_ID + "-0000000000000000-01")); // zero parent id
    }

    @Test
    void parseToleratesFutureVersionsWithTrailingFields() {
        // a higher version may append fields; trace-id and parent-id remain fields 1 and 2
        var future = "01-" + TRACE_ID + "-" + SPAN_ID + "-01-extrafield";
        var parsed = W3cTrace.parse(future);
        assertNotNull(parsed);
        assertEquals(TRACE_ID, parsed[0]);
        assertEquals(SPAN_ID, parsed[1]);
    }
}
