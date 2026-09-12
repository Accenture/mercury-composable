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
    void registerThenCompleteResolvesTheFutureInPlace() throws Exception {
        PendingRequests pending = new PendingRequests(100);
        CompletableFuture<String> future = pending.register("cid-1");
        assertTrue(pending.isPending("cid-1"));
        assertTrue(pending.complete("cid-1", "ok"));
        assertEquals("ok", future.get(1, TimeUnit.SECONDS));
        // D8: completion is IN PLACE - the entry stays until the awaiting (or aborting) path removes it,
        // because the response was destructively popped from Redis and the future is the only copy left;
        // an await-by-cid arriving after completion must still find it.
        assertTrue(pending.isPending("cid-1"), "completed entry stays registered for the await-by-cid lookup");
        assertEquals(future, pending.get("cid-1"), "await-by-cid finds the completed future");
        pending.cancel("cid-1");   // the awaiting path's finally block
        assertFalse(pending.isPending("cid-1"));
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

    @Test
    void capacityIsReleasedOnCancelNotOnComplete() {
        PendingRequests pending = new PendingRequests(1);
        pending.register("cid-1");
        pending.complete("cid-1", "ok");
        // D8: in-place completion keeps the slot reserved - the request is not over until awaited/aborted
        assertThrows(IllegalStateException.class, () -> pending.register("cid-2"),
                "a completed-but-unawaited request still holds its capacity slot");
        pending.cancel("cid-1");                              // the awaiting path releases the slot
        pending.register("cid-3");                            // can register again at cap 1
        pending.cancel("cid-3");
        pending.register("cid-4");
        assertTrue(pending.isPending("cid-4"));
    }

    @Test
    void rejectedRegistrationDoesNotConsumeASlot() {
        PendingRequests pending = new PendingRequests(1);
        pending.register("cid-1");
        assertThrows(IllegalStateException.class, () -> pending.register("cid-2"));   // over cap
        pending.cancel("cid-1");
        // the over-cap attempt must not have leaked a reserved slot
        pending.register("cid-3");
        assertTrue(pending.isPending("cid-3"));
    }
}
