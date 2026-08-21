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
import org.platformlambda.automation.models.AssignedRoute;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.ConfigReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the documentation guides' canonical rest.yaml worked example
 * (guide-fixtures/rest-bindings.yaml, embedded into the guides by mkdocs) through the
 * production RoutingEntry parser. The docs once taught a flow binding with {@code flow:}
 * alone - a form the parser silently skips because {@code service:} is mandatory - so this
 * test pins the documented example to what the router actually accepts.
 */
class RoutingEntryGuideFixtureTest extends TestBase {
    private static final String FIXTURE = "classpath:/guide-fixtures/rest-bindings.yaml";
    private static final String APPLICATION_REST_CONFIG = "classpath:/rest.yaml";
    private static final String FLOW_ADAPTER = "http.flow.adapter";
    private static final String PROFILE_FUNCTION = "profile.lookup";

    @Test
    void guideWorkedExampleLoadsThroughTheProductionParser() {
        Platform platform = Platform.getInstance();
        // the fixture's service routes must exist when RoutingEntry validates the entries
        boolean stubFlowAdapter = registerIfMissing(platform, FLOW_ADAPTER);
        boolean stubProfile = registerIfMissing(platform, PROFILE_FUNCTION);
        RoutingEntry routing = RoutingEntry.getInstance();
        try {
            routing.load(new ConfigReader(FIXTURE));

            // flow-backed: 'service' selects the flow adapter and 'flow' selects the flow
            AssignedRoute flow = requireRoute(routing, "POST", "/api/orders/1001/status");
            assertEquals(FLOW_ADAPTER, flow.info.primary);
            assertEquals("order-status", flow.info.flowId);
            assertEquals(List.of("POST", "OPTIONS"), flow.info.methods);
            assertEquals(30, flow.info.timeoutSeconds);
            assertTrue(flow.info.tracing);
            assertEquals("1001", flow.arguments.get("order_id"));
            // the reusable cors/headers configs resolved by id
            assertEquals("cors_1", flow.info.corsId);
            assertEquals("header_1", flow.info.responseTransformId);

            // function-backed endpoint with a path parameter
            AssignedRoute function = requireRoute(routing, "GET", "/api/profile/8300");
            assertEquals(PROFILE_FUNCTION, function.info.primary);
            assertEquals(10, function.info.timeoutSeconds);
            assertEquals("8300", function.arguments.get("id"));

            // HTTP relay: 'service' is a URL and url_rewrite maps the path prefix
            AssignedRoute relay = requireRoute(routing, "GET", "/api/upstream/orders");
            assertEquals("https://example.org", relay.info.host);
            assertEquals(List.of("/api/upstream", "/v1"), relay.info.urlRewrite);
            assertEquals(20, relay.info.timeoutSeconds);
            assertTrue(relay.info.tracing);
        } finally {
            // RoutingEntry is a process-wide singleton and load() is additive - re-load the
            // application config so its routes stay authoritative for later test classes.
            // The fixture's paths are distinct from rest.yaml's, so nothing collides.
            routing.load(new ConfigReader(APPLICATION_REST_CONFIG));
            if (stubFlowAdapter) {
                platform.release(FLOW_ADAPTER);
            }
            if (stubProfile) {
                platform.release(PROFILE_FUNCTION);
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
