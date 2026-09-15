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

package com.accenture.minigraph.common;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the shared runtime guards in GraphLambdaFunction: the model-metadata
 * immutability check every model-writing mapping path calls (data-mapping RHS validation,
 * fetcher input/output mappings, for_each expansion) in both walker lanes, and the guarded
 * async completion that turns a failure inside a skill's completion callback into the node's
 * error instead of a silently pending Mono.
 */
class GraphLambdaFunctionGuardTest {

    /** Minimal concrete subclass to reach the protected guard. */
    private static class Probe extends GraphLambdaFunction {
        @Override
        public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
            return null;
        }
    }

    private final Probe probe = new Probe();

    @Test
    void reservedMetadataWriteTargetsAreRejected() {
        for (var key : new String[]{"cid", "instance", "flow", "ttl", "trace", "parent", "root", "none", "run"}) {
            var rhs = "model." + key;
            assertThrows(IllegalArgumentException.class,
                    () -> probe.assertMutableModelTarget("worker", rhs), rhs + " must be rejected");
        }
        // composite forms cannot smuggle a write target into a reserved key either
        assertThrows(IllegalArgumentException.class,
                () -> probe.assertMutableModelTarget("worker", "model.cid.x"));
        assertThrows(IllegalArgumentException.class,
                () -> probe.assertMutableModelTarget("worker", "model.ttl[0]"));
    }

    @Test
    void ordinaryTargetsPassTheGuard() {
        // an ordinary model key is writable
        assertDoesNotThrow(() -> probe.assertMutableModelTarget("worker", "model.custom"));
        // a name that merely STARTS with a reserved word is not blocked (no false positive)
        assertDoesNotThrow(() -> probe.assertMutableModelTarget("worker", "model.ttlx"));
        assertDoesNotThrow(() -> probe.assertMutableModelTarget("worker", "model.cids"));
        // non-model targets are outside this guard's scope
        assertDoesNotThrow(() -> probe.assertMutableModelTarget("worker", "worker.result"));
        assertDoesNotThrow(() -> probe.assertMutableModelTarget("worker", "output.body"));
    }

    @Test
    void guardedCompletionReturnsNextPath() {
        var pending = CompletableFuture.completedFuture(new EventEnvelope().setBody("ok"));
        assertEquals("next", probe.guardedCompletion(pending, response -> "next").block());
    }

    @Test
    void guardedCompletionSurfacesCallbackException() {
        // a RuntimeException thrown while handling the response (e.g. an invalid output data
        // mapping) must terminate the Mono with that error - the naive thenAccept form let the
        // CompletableFuture swallow it, the sink never completed, and the caller timed out
        var pending = CompletableFuture.completedFuture(new EventEnvelope().setBody("ok"));
        var mono = probe.guardedCompletion(pending, response -> {
            throw new IllegalArgumentException("Invalid output data mapping");
        });
        var e = assertThrows(IllegalArgumentException.class, mono::block);
        assertEquals("Invalid output data mapping", e.getMessage());
    }

    @Test
    void guardedCompletionSurfacesFailedFuture() {
        // an exceptionally completed future must terminate the Mono too, unwrapped
        var direct = new CompletableFuture<EventEnvelope>();
        direct.completeExceptionally(new IllegalStateException("connection lost"));
        var directMono = probe.guardedCompletion(direct, response -> "next");
        var e1 = assertThrows(IllegalStateException.class, directMono::block);
        assertEquals("connection lost", e1.getMessage());
        // the same when the failure arrives wrapped in a CompletionException (dependent stage)
        var wrapped = new CompletableFuture<EventEnvelope>();
        wrapped.completeExceptionally(new CompletionException(new IllegalStateException("wrapped loss")));
        var wrappedMono = probe.guardedCompletion(wrapped, response -> "next");
        var e2 = assertThrows(IllegalStateException.class, wrappedMono::block);
        assertEquals("wrapped loss", e2.getMessage());
    }
}
