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

package org.platformlambda.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamSegmentTest {

    @Test
    void roundTripAllFields() {
        StreamSegment original = StreamSegment.of(StreamSegment.DATA, "tokens", "{\"t\":\"Hello\"}");
        StreamSegment restored = StreamSegment.fromJson(original.toJson());
        assertEquals(original, restored);
        assertFalse(restored.isTerminal());
    }

    @Test
    void nullFieldsAreOmittedFromTheWireForm() {
        StreamSegment bareEof = StreamSegment.of(StreamSegment.EOF, null, null);
        String json = bareEof.toJson();
        assertEquals("{\"type\":\"eof\"}", json, "compact wire form omits null name and body");
        StreamSegment restored = StreamSegment.fromJson(json);
        assertNull(restored.name());
        assertNull(restored.body());
        assertTrue(restored.isTerminal());
    }

    @Test
    void eofAndExceptionAreTerminal() {
        assertTrue(StreamSegment.of(StreamSegment.EOF, null, "{}").isTerminal());
        assertTrue(StreamSegment.of(StreamSegment.EXCEPTION, null, "boom").isTerminal());
        assertFalse(StreamSegment.of(StreamSegment.DATA, null, "x").isTerminal());
    }

    @Test
    void bodyWithJsonAndSpecialCharactersSurvivesTheRoundTrip() {
        // the body is opaque text: embedded JSON, quotes, newlines and non-ASCII must survive escaping
        String body = "{\"quote\":\"He said \\\"hi\\\"\",\n \"emoji\":\"🚀\", \"tab\":\"\t\"}";
        StreamSegment restored = StreamSegment.fromJson(StreamSegment.of(StreamSegment.DATA, null, body).toJson());
        assertEquals(body, restored.body());
    }

    @Test
    void invalidTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> StreamSegment.of("bogus", null, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> StreamSegment.fromJson("{\"type\":\"bogus\",\"body\":\"x\"}"));
        assertThrows(IllegalArgumentException.class, () -> StreamSegment.fromJson("{\"body\":\"no type\"}"));
    }

    @Test
    void malformedJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> StreamSegment.fromJson("not json at all"));
    }
}
