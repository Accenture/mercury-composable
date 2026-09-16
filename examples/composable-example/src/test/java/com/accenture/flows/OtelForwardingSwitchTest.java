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

import com.accenture.support.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression guard for the OpenTelemetry forwarding switch.
 *
 * <p>This application carries the {@code opentelemetry-forwarder} dependency but ships with
 * {@code otel.forwarding=false}, which is the shape the switch exists to make safe: a team can put the
 * jar on the classpath and let DevOps decide, per environment, whether traces leave the process. The
 * forwarder lives under {@code org.platformlambda} - a base scan package - so without the
 * {@code @OptionalService("otel.forwarding")} gate the jar alone would auto-register the route and start
 * exporting. That is what this test pins.</p>
 *
 * <p>The opposite direction (switch on, route registered, spans reaching a collector) is covered in the
 * forwarder module itself, whose test config sets {@code otel.forwarding=true} and drives real OTLP over
 * the wire into a mock collector.</p>
 */
class OtelForwardingSwitchTest extends TestBase {

    private static final String FORWARDER_ROUTE = "distributed.trace.forwarder";

    @Test
    void theForwarderIsNotRegisteredWhenForwardingIsOff() {
        AppConfigReader config = AppConfigReader.getInstance();
        assertEquals("false", config.getProperty("otel.forwarding"),
                "this example ships with forwarding off - it is the 'dependency present, feature off' case");
        assertFalse(Platform.getInstance().hasRoute(FORWARDER_ROUTE),
                "otel.forwarding=false must skip registration entirely, not merely make the forwarder a "
                        + "no-op: the jar is on the classpath and under a scanned package, so only the "
                        + "@OptionalService gate keeps the route from existing");
    }
}
