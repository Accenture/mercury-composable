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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingRequestsTest {

    @Test
    void registerThenCompleteResolvesTheFuture() throws Exception {
        PendingRequests pending = new PendingRequests(100);
        CompletableFuture<String> future = pending.register("cid-1");
        assertTrue(pending.isPending("cid-1"));
        assertTrue(pending.complete("cid-1", "ok"));
        assertEquals("ok", future.get(1, TimeUnit.SECONDS));
        assertFalse(pending.isPending("cid-1"), "entry is removed on completion");
    }

    @Test
    void duplicateOrOrphanCompletionIsNoOp() {
        PendingRequests pending = new PendingRequests(100);
        pending.register("cid-1");
        assertTrue(pending.complete("cid-1", "first"));
        assertFalse(pending.complete("cid-1", "second"), "already completed -> no-op");
        assertFalse(pending.complete("unknown", "x"), "orphan -> no-op");
    }

    @Test
    void cancelDropsWithoutCompleting() {
        PendingRequests pending = new PendingRequests(100);
        CompletableFuture<String> future = pending.register("cid-1");
        pending.cancel("cid-1");
        assertFalse(pending.isPending("cid-1"));
        assertFalse(future.isDone());
    }

    @Test
    void enforcesMaxPending() {
        PendingRequests pending = new PendingRequests(1);
        pending.register("cid-1");
        assertThrows(IllegalStateException.class, () -> pending.register("cid-2"));
    }

    @Test
    void rejectsDuplicateCorrelationId() {
        PendingRequests pending = new PendingRequests(100);
        pending.register("cid-1");
        assertThrows(IllegalStateException.class, () -> pending.register("cid-1"));
    }
}
