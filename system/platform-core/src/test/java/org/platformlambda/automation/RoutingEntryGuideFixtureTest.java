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

package org.platformlambda.automation;

import org.junit.jupiter.api.Test;
import org.platformlambda.automation.config.RoutingEntry;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.automation.models.AssignedRoute;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.ConfigReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutingEntryGuideFixtureTest {
    private static final String FUNCTION_ROUTE = "agent.skill.rest.function";
    private static final String FLOW_ADAPTER = "http.flow.adapter";
    private static final String FIXTURE =
            "classpath:/mercury/agent-skill/references/fixtures/rest-bindings.yaml";
    private static final String APPLICATION_REST_CONFIG = "classpath:/rest.yaml";

    @Test
    void canonicalGuideFixtureResolvesThroughProductionRoutingEntry() {
        Platform platform = Platform.getInstance();
        boolean addedFunction = registerIfMissing(platform, FUNCTION_ROUTE);
        boolean addedFlowAdapter = registerIfMissing(platform, FLOW_ADAPTER);
        try {
            RoutingEntry routing = RoutingEntry.getInstance();
            routing.load(new ConfigReader(FIXTURE));

            AssignedRoute function = requireRoute(routing, "GET", "/api/agent-skills/rest/function/customer-42");
            assertEquals(FUNCTION_ROUTE, function.info.primary);
            assertEquals(List.of(FUNCTION_ROUTE), function.info.services);
            assertEquals(List.of("GET", "OPTIONS"), function.info.methods);
            assertEquals(10, function.info.timeoutSeconds);
            assertEquals("customer-42", function.arguments.get("id"));

            AssignedRoute flow = requireRoute(routing, "POST", "/api/agent-skills/rest/flow");
            assertEquals(FLOW_ADAPTER, flow.info.primary);
            assertEquals(List.of(FLOW_ADAPTER), flow.info.services);
            assertEquals("agent-skill-rest-flow", flow.info.flowId);
            assertEquals(List.of("POST", "OPTIONS"), flow.info.methods);
            assertEquals(30, flow.info.timeoutSeconds);

            AssignedRoute relay = requireRoute(routing, "GET", "/api/agent-skills/rest/relay");
            assertEquals(AsyncHttpClient.ASYNC_HTTP_REQUEST, relay.info.primary);
            assertEquals(List.of("https://example.org"), relay.info.services);
            assertEquals("https://example.org", relay.info.host);
            assertEquals(List.of("/api/agent-skills/rest/relay", "/upstream"), relay.info.urlRewrite);
            assertEquals(List.of("GET", "OPTIONS"), relay.info.methods);
            assertEquals(20, relay.info.timeoutSeconds);
            assertTrue(relay.info.tracing);
        } finally {
            // RoutingEntry is a process-wide singleton. Restore the application
            // routing model before later test classes inspect static-content rules.
            RoutingEntry.getInstance().load(new ConfigReader(APPLICATION_REST_CONFIG));
            if (addedFlowAdapter) {
                platform.release(FLOW_ADAPTER);
            }
            if (addedFunction) {
                platform.release(FUNCTION_ROUTE);
            }
        }
    }

    private static boolean registerIfMissing(Platform platform, String route) {
        if (platform.hasRoute(route)) {
            return false;
        }
        platform.registerPrivate(route, (headers, input, instance) -> input, 1);
        return true;
    }

    private static AssignedRoute requireRoute(RoutingEntry routing, String method, String url) {
        AssignedRoute route = routing.getRouteInfo(method, url);
        assertNotNull(route, () -> method + " " + url + " was not registered");
        assertNotNull(route.info, () -> method + " " + url + " did not resolve for this method");
        return route;
    }
}
