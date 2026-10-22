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

package org.platformlambda.dynatrace;

import com.dynatrace.oneagent.sdk.api.enums.SDKState;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.spi.InvocationInfo;

import java.util.Map;
import java.util.function.BinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Startup: whether an observer is built, and how it is configured.
 */
class OneAgentSetupTest {
    private static final InvocationInfo HOP = new InvocationInfo("demo.route", null, "event-1",
            "trace-1", null, null, 1);

    @Test
    void noObserverWithoutAnAttachedAgent() {
        FakeOneAgent agent = new FakeOneAgent();
        agent.state = SDKState.PERMANENTLY_INACTIVE;

        assertNull(OneAgentSetup.configure(agent.sdk, settings(Map.of())));
        assertNotNull(agent.logging);
    }

    @Test
    void anObserverWhenTheAgentIsActiveOrStarting() {
        FakeOneAgent active = new FakeOneAgent();
        assertNotNull(OneAgentSetup.configure(active.sdk, settings(Map.of())));

        FakeOneAgent starting = new FakeOneAgent();
        starting.state = SDKState.TEMPORARILY_INACTIVE;
        assertNotNull(OneAgentSetup.configure(starting.sdk, settings(Map.of())));
    }

    @Test
    void theSettingsChooseTheDestinationAndTheLogContext() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver observer = OneAgentSetup.configure(agent.sdk, settings(Map.of(
                OneAgentSetup.DESTINATION, "route", OneAgentSetup.LOG_CONTEXT, "true",
                "otel.tracing", "true", "otel.forwarding", "true")));

        assertNotNull(observer);
        observer.begin(HOP, new OneAgentObserverTest.MapCarrier(Map.of())).end();
        assertEquals("demo.route", agent.infos.getFirst());
    }

    @Test
    void theSdksDiagnosticsGoToTheLog() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentSetup.configure(agent.sdk, settings(Map.of()));

        assertDoesNotThrow(() -> {
            agent.logging.warn("tracer used on the wrong thread");
            agent.logging.error("agent failure");
        });
    }

    private static BinaryOperator<String> settings(Map<String, String> values) {
        return values::getOrDefault;
    }
}
