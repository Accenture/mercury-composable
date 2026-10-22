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

package org.platformlambda.otel.tracer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.spi.InvocationObservers;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BinaryOperator;

/**
 * Registers the {@link OtelInvocationObserver} at startup. The jar alone does nothing: the master switch is
 * {@code otel.tracing=true} (default off).
 * <p>
 * It uses {@link GlobalOpenTelemetry}: whatever the attached agent installs. The implementation's class is
 * logged at startup, so a field experiment can see which agent is recording; the observer is registered
 * either way, because an agent may implement the API by instrumenting it in place.
 */
@OptionalService("otel.tracing")
@BeforeApplication(sequence = 3)
public class OtelTracerSetup implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(OtelTracerSetup.class);
    private static final String DYNATRACE_ONEAGENT = "dynatrace.oneagent";
    private static final String OTEL_FORWARDING = "otel.forwarding";
    private static final String TRUE = "true";

    @Override
    public void start(String[] args) {
        InvocationObservers.register(configure(GlobalOpenTelemetry.get(), AppConfigReader.getInstance()::getProperty));
    }

    /**
     * @param openTelemetry the API implementation
     * @param property reads a setting: (key, default) to value
     * @return the observer
     */
    static OtelInvocationObserver configure(OpenTelemetry openTelemetry, BinaryOperator<String> property) {
        log.info("OpenTelemetry invocation tracer - API implementation {}", openTelemetry.getClass().getName());
        if (TRUE.equalsIgnoreCase(property.apply(DYNATRACE_ONEAGENT, "false"))) {
            log.warn("{}=true as well - two live tracers record every hop twice; run one at a time", DYNATRACE_ONEAGENT);
        }
        if (TRUE.equalsIgnoreCase(property.apply(OTEL_FORWARDING, "false"))) {
            log.warn("{}=true as well - the forwarder exports a second, post-hoc copy of every hop; " +
                    "an agent capturing OpenTelemetry API spans may record both", OTEL_FORWARDING);
        }
        return new OtelInvocationObserver(openTelemetry);
    }
}
