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

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingStreamsTest {

    private static final Consumer<StreamSegment> NO_OP_SINK = segment -> { };

    @Test
    void registerThenRemove() {
        PendingStreams streams = new PendingStreams(100);
        PendingStreams.StreamEntry entry = streams.register("cid-1", NO_OP_SINK);
        assertTrue(streams.contains("cid-1"));
        assertEquals(entry, streams.get("cid-1"));
        assertEquals(1, streams.size());
        streams.remove("cid-1");
        assertFalse(streams.contains("cid-1"));
        assertNull(streams.get("cid-1"));
        streams.remove("cid-1");   // idempotent
        assertEquals(0, streams.size());
    }

    @Test
    void rejectsDuplicateCorrelationId() {
        PendingStreams streams = new PendingStreams(100);
        streams.register("cid-1", NO_OP_SINK);
        assertThrows(IllegalStateException.class, () -> streams.register("cid-1", NO_OP_SINK));
    }

    @Test
    void enforcesMaxOpenStreams() {
        PendingStreams streams = new PendingStreams(1);
        streams.register("cid-1", NO_OP_SINK);
        assertThrows(IllegalStateException.class, () -> streams.register("cid-2", NO_OP_SINK));
        streams.remove("cid-1");                      // slot freed on close
        streams.register("cid-3", NO_OP_SINK);        // can register again at cap 1
        assertTrue(streams.contains("cid-3"));
    }

    @Test
    void rejectedRegistrationDoesNotConsumeASlot() {
        PendingStreams streams = new PendingStreams(1);
        streams.register("cid-1", NO_OP_SINK);
        // repeated over-cap attempts must not consume anything
        assertThrows(IllegalStateException.class, () -> streams.register("cid-2", NO_OP_SINK));
        assertThrows(IllegalStateException.class, () -> streams.register("cid-2", NO_OP_SINK));
        streams.remove("cid-1");
        // the rejected attempts must not have leaked reserved slots: the freed slot is fully usable...
        streams.register("cid-3", NO_OP_SINK);
        assertTrue(streams.contains("cid-3"));
        // ...and the cap is still exactly 1 - no phantom capacity in either direction
        assertThrows(IllegalStateException.class, () -> streams.register("cid-4", NO_OP_SINK));
        assertEquals(1, streams.size());
    }

    @Test
    void idempotentRemoveReleasesTheSlotOnlyOnce() {
        PendingStreams streams = new PendingStreams(1);
        streams.register("cid-1", NO_OP_SINK);
        streams.remove("cid-1");
        streams.remove("cid-1");   // double close must not free a second (phantom) slot
        streams.register("cid-2", NO_OP_SINK);
        assertThrows(IllegalStateException.class, () -> streams.register("cid-3", NO_OP_SINK),
                "cap is still 1: the duplicate remove must not have widened it");
    }

    @Test
    void drainFlagIsExclusiveUntilReleased() {
        PendingStreams streams = new PendingStreams(100);
        PendingStreams.StreamEntry entry = streams.register("cid-1", NO_OP_SINK);
        assertTrue(entry.tryAcquireDrain(), "first caller owns the drain loop");
        assertFalse(entry.tryAcquireDrain(), "a concurrent wake-up must not start a second drain");
        entry.releaseDrain();
        assertTrue(entry.tryAcquireDrain(), "released flag can be re-acquired by the next wake-up");
    }
}
