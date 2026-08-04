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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the runtime model-metadata immutability guard - the shared check every
 * model-writing mapping path calls (data-mapping RHS validation, fetcher input/output
 * mappings, for_each expansion) in both walker lanes.
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
        // composite forms cannot smuggle a write into a reserved key either
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
}
