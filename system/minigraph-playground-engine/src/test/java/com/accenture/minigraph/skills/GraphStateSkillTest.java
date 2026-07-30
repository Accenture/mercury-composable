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

package com.accenture.minigraph.skills;

import com.accenture.minigraph.models.GraphInstance;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphStateSkillTest {

    private static class TestSkill extends GraphStateSkill {
        @Override
        public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
            return null;
        }
    }

    @Test
    void businessCorrelationIdIsTrimmed() {
        // a business correlation ID (e.g. an order number) may be entered by an operator
        // in a web UI with accidental padding - the cid is the store key, and both
        // engines trim identically so the mixed-fleet key space stays one key per id
        var graphInstance = new GraphInstance("unit-test");
        graphInstance.stateMachine.setElement("model.cid", "  order-1001  ");
        assertEquals("order-1001", new TestSkill().getRequiredCorrelationId(graphInstance, "suspend"));
        // blank remains a missing correlation ID, not an empty key
        graphInstance.stateMachine.setElement("model.cid", "   ");
        assertThrows(IllegalArgumentException.class,
                () -> new TestSkill().getRequiredCorrelationId(graphInstance, "suspend"));
    }
}
