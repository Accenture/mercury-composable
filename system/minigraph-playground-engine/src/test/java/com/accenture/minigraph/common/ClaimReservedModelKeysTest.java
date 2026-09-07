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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claims-fixture pin for claim id {@code reserved-model-keys-nine}: the engine-managed,
 * runtime-immutable model-metadata key set has EXACTLY nine names - cid, instance, flow,
 * ttl, trace, parent, root, none, run (GraphLambdaFunction.RESERVED_MODEL_METADATA, the
 * same nine as the suspend/resume NON_PERSISTED_MODEL_KEYS and Event Script's reserved
 * model keys, per the documented contract).
 *
 * <p>The set-equality assertion fails when a name is ADDED or REMOVED (the companion
 * {@link GraphLambdaFunctionGuardTest} pins removal behaviorally but cannot detect an
 * addition). The behavioral spot-checks tie the constant to the guard that enforces it,
 * so the pin cannot be satisfied by an unused constant.
 */
class ClaimReservedModelKeysTest {

    /** Minimal concrete subclass to reach the protected guard. */
    private static class Probe extends GraphLambdaFunction {
        @Override
        public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
            return null;
        }
    }

    private final Probe probe = new Probe();

    @Test
    void reservedModelMetadataIsExactlyTheDocumentedNineNames() {
        var documented = Set.of("cid", "instance", "flow", "ttl", "trace", "parent", "root", "none", "run");
        assertEquals(documented, GraphLambdaFunction.RESERVED_MODEL_METADATA,
                "the reserved model-metadata key set must stay exactly the nine documented names - "
                        + "adding or removing one changes the documented contract "
                        + "(claims-registry: reserved-model-keys-nine)");
        assertEquals(9, GraphLambdaFunction.RESERVED_MODEL_METADATA.size());
    }

    @Test
    void everyReservedNameIsEnforcedByTheRuntimeGuard() {
        for (var key : GraphLambdaFunction.RESERVED_MODEL_METADATA) {
            assertThrows(IllegalArgumentException.class,
                    () -> probe.assertMutableModelTarget("worker", "model." + key),
                    "model." + key + " must be rejected as an engine-managed metadata write target");
        }
        // and a tenth, non-reserved name passes: the guard scope IS the nine-name set
        assertDoesNotThrow(() -> probe.assertMutableModelTarget("worker", "model.custom"));
    }
}
