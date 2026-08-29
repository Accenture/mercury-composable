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

package org.platformlambda.automation.models;

import io.vertx.core.buffer.Buffer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-request state for a streaming HTTP response (x-event-stream), held on the
 * AsyncContextHolder. The pending queue implements drain-aware buffering for slow
 * clients: writes queue here when the vert.x write queue is full and flush on drain,
 * bounded by a byte cap.
 */
public class EventStreamState {

    public enum Mode { SSE, CHUNKED }

    private final Mode mode;
    // guards the pending queue and response writes: the request's reply lane and the
    // vert.x drain handler (event-loop thread) both flush, and must never interleave
    private final ReentrantLock lock = new ReentrantLock();
    private final Queue<Buffer> pending = new ArrayDeque<>();
    private long pendingBytes = 0;
    private long keepAliveTimer = -1;
    private boolean drainHandlerSet = false;
    private boolean endAfterFlush = false;
    private boolean closed = false;
    private long eventCount = 0;
    private long byteCount = 0;

    public EventStreamState(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Queue one buffer, bounded by a byte cap
     *
     * @param buffer content to queue
     * @param capBytes maximum queued bytes
     * @return false when accepting the buffer would exceed the cap
     */
    public boolean offer(Buffer buffer, long capBytes) {
        if (pendingBytes + buffer.length() > capBytes) {
            return false;
        }
        pending.add(buffer);
        pendingBytes += buffer.length();
        return true;
    }

    /**
     * @return the next queued buffer or null
     */
    public Buffer poll() {
        Buffer buffer = pending.poll();
        if (buffer != null) {
            pendingBytes -= buffer.length();
        }
        return buffer;
    }

    public boolean hasPending() {
        return !pending.isEmpty();
    }

    public long getPendingBytes() {
        return pendingBytes;
    }

    public long getKeepAliveTimer() {
        return keepAliveTimer;
    }

    public void setKeepAliveTimer(long keepAliveTimer) {
        this.keepAliveTimer = keepAliveTimer;
    }

    public boolean isDrainHandlerSet() {
        return drainHandlerSet;
    }

    public void setDrainHandlerSet(boolean drainHandlerSet) {
        this.drainHandlerSet = drainHandlerSet;
    }

    public boolean isEndAfterFlush() {
        return endAfterFlush;
    }

    public void setEndAfterFlush(boolean endAfterFlush) {
        this.endAfterFlush = endAfterFlush;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public long getEventCount() {
        return eventCount;
    }

    public void incrementEventCount() {
        this.eventCount++;
    }

    public long getByteCount() {
        return byteCount;
    }

    public void addByteCount(long bytes) {
        this.byteCount += bytes;
    }
}
