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
import com.accenture.models.Flows;
import com.accenture.setup.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claims-fixture pin (claim id: flows-location-default).
 * <p>
 * When a flow manifest (flows.yaml) omits the optional 'location' key, flow files are loaded
 * from the documented default 'classpath:/flows/' (CompileFlows). The fixture manifest
 * 'claim-flows-no-location.yaml' has no 'location' key and lists 'claim-location-default.yml',
 * which exists only under src/test/resources/flows/ - so this flow can load ONLY through the
 * default location. If the engine's default changed, the flow would fail to load and both
 * tests here would fail.
 */
class ClaimFlowsLocationDefaultTest extends TestBase {
    private static final String FLOW_ID = "claim-location-default";

    @Test
    void manifestWithoutLocationLoadsFromDefaultClasspathFlows() {
        assertTrue(Flows.flowExists(FLOW_ID),
                "a manifest without 'location' must load its flows from classpath:/flows/");
    }

    @SuppressWarnings("unchecked")
    @Test
    void flowLoadedFromDefaultLocationIsExecutable() throws Exception {
        Utility util = Utility.getInstance();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", Map.of());
        dataset.put("body", Map.of("hello", "world"));
        EventEnvelope result = FlowExecutor.getInstance()
                .request("unit.test", util.getUuid(), "TEST /claim/location/default",
                        FLOW_ID, dataset, util.getUuid(), 8000).get();
        assertInstanceOf(Map.class, result.getBody(), "the flow response body must be a Map");
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("world", body.get("hello"),
                "the flow loaded from the default location must execute normally");
    }
}
