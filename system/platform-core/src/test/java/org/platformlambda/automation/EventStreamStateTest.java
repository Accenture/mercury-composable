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

package org.platformlambda.automation;

import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.models.EventStreamState;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The drain-aware pending queue is the slow-client guard: writes queue here when the
 * socket back-pressures and the byte cap bounds the buffering.
 */
class EventStreamStateTest {

    @Test
    void pendingQueueEnforcesTheByteCap() {
        var state = new EventStreamState(EventStreamState.Mode.SSE);
        assertEquals(EventStreamState.Mode.SSE, state.getMode());
        assertTrue(state.offer(Buffer.buffer(new byte[600]), 1000));
        assertEquals(600, state.getPendingBytes());
        assertFalse(state.offer(Buffer.buffer(new byte[500]), 1000), "cap exceeded must be rejected");
        assertEquals(600, state.getPendingBytes(), "a rejected buffer is not queued");
        assertTrue(state.offer(Buffer.buffer(new byte[400]), 1000), "exactly at the cap is accepted");
        assertEquals(1000, state.getPendingBytes());
    }

    @Test
    void pollReleasesBytesInFifoOrder() {
        var state = new EventStreamState(EventStreamState.Mode.CHUNKED);
        state.offer(Buffer.buffer("first"), 100);
        state.offer(Buffer.buffer("second"), 100);
        assertTrue(state.hasPending());
        assertEquals("first", state.poll().toString());
        assertEquals("second", state.poll().toString());
        assertNull(state.poll());
        assertFalse(state.hasPending());
        assertEquals(0, state.getPendingBytes());
    }

    @Test
    void lifecycleFlagsAndCountersBehave() {
        var state = new EventStreamState(EventStreamState.Mode.SSE);
        assertFalse(state.isClosed());
        assertFalse(state.isEndAfterFlush());
        assertFalse(state.isDrainHandlerSet());
        assertEquals(-1, state.getKeepAliveTimer());
        state.setClosed(true);
        state.setEndAfterFlush(true);
        state.setDrainHandlerSet(true);
        state.setKeepAliveTimer(42);
        state.incrementEventCount();
        state.incrementEventCount();
        state.addByteCount(123);
        assertTrue(state.isClosed());
        assertTrue(state.isEndAfterFlush());
        assertTrue(state.isDrainHandlerSet());
        assertEquals(42, state.getKeepAliveTimer());
        assertEquals(2, state.getEventCount());
        assertEquals(123, state.getByteCount());
    }
}
