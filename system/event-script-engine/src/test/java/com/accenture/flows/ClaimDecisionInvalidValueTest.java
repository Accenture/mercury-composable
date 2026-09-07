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

package com.accenture.flows;

import com.accenture.adapters.FlowExecutor;
import com.accenture.setup.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claims-fixture pin (claim id: decision-invalid-value).
 * <p>
 * TaskExecutor.handleDecisionTask: a decision task that produces NO decision value (null)
 * aborts the flow with "returned invalid decision"; a NON-NUMERIC string is coerced via
 * Math.max(1, str2int(...)) - str2int returns -1 for non-numeric input - and therefore
 * silently routes to branch 1. The fixture flow 'claim-decision-invalid-test' echoes the
 * caller-supplied decision value verbatim (no.op), so the engine's own coercion is what is
 * being exercised, not a task-side conversion.
 */
class ClaimDecisionInvalidValueTest extends TestBase {
    private static final String FLOW_ID = "claim-decision-invalid-test";

    @SuppressWarnings("unchecked")
    private Map<String, Object> runFlow(Map<String, Object> body, EventEnvelope[] envelopeHolder) throws Exception {
        Utility util = Utility.getInstance();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", Map.of());
        dataset.put("body", body);
        EventEnvelope result = FlowExecutor.getInstance()
                .request("unit.test", util.getUuid(), "TEST /claim/decision/invalid",
                        FLOW_ID, dataset, util.getUuid(), 8000).get();
        assertInstanceOf(Map.class, result.getBody());
        envelopeHolder[0] = result;
        return (Map<String, Object>) result.getBody();
    }

    @Test
    void nullDecisionAbortsTheFlow() throws Exception {
        // no 'decision' key in the request body -> the decision task yields no decision value
        EventEnvelope[] holder = new EventEnvelope[1];
        Map<String, Object> body = runFlow(Map.of("hello", "world"), holder);
        assertEquals(500, holder[0].getStatus(), "a null decision must abort the flow: " + body);
        assertEquals(500, body.get("status"));
        String message = String.valueOf(body.get("message"));
        assertTrue(message.contains("returned invalid decision"),
                "the abort reason must be the invalid decision: " + body);
    }

    @Test
    void nonNumericDecisionSilentlyRoutesToBranchOne() throws Exception {
        // str2int("pizza") == -1, Math.max(1, -1) == 1 -> branch 1, no error
        EventEnvelope[] holder = new EventEnvelope[1];
        Map<String, Object> body = runFlow(Map.of("decision", "pizza"), holder);
        assertEquals(200, holder[0].getStatus(), "a non-numeric decision must not abort: " + body);
        assertEquals("one", body.get("branch"),
                "a non-numeric decision string must coerce to branch 1: " + body);
    }

    @Test
    void numericDecisionRoutesNormally() throws Exception {
        // control case: proves branch selection is real, so the branch-1 assertion above
        // cannot pass merely because everything routes to the first branch
        EventEnvelope[] holder = new EventEnvelope[1];
        Map<String, Object> body = runFlow(Map.of("decision", "2"), holder);
        assertEquals(200, holder[0].getStatus());
        assertEquals("two", body.get("branch"), "decision '2' must select the second branch: " + body);
    }
}
