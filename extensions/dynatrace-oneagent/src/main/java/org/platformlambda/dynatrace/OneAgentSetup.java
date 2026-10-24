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

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.LoggingCallback;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.enums.SDKState;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.spi.InvocationObservers;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BinaryOperator;

/**
 * Registers the {@link OneAgentObserver} at startup. The jar alone does nothing: the master switch is
 * {@code dynatrace.oneagent.enabled=true} (default off), so one artifact ships and each environment decides.
 * The switch ends in {@code .enabled} because the settings below share its prefix: a configuration key
 * cannot be both a value and the parent of other keys, so a bare {@code dynatrace.oneagent=true} would be
 * lost beside them.
 * <p>
 * Settings:
 * <ul>
 *   <li>{@code dynatrace.oneagent.destination} - {@code application} (default) or {@code route}</li>
 *   <li>{@code dynatrace.oneagent.log.context} - {@code true} writes OneAgent's ids into the log context</li>
 * </ul>
 * One SDK instance is created for the JVM. Its state is logged at startup: without an attached OneAgent
 * the SDK is permanently inactive and the observer is not registered. The SDK's own warnings (a tracer
 * used on a wrong thread, for example) go to this application's log.
 */
@OptionalService(OneAgentSetup.ENABLED)
@BeforeApplication(sequence = 3)
public class OneAgentSetup implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(OneAgentSetup.class);
    public static final String ENABLED = "dynatrace.oneagent.enabled";
    public static final String DESTINATION = "dynatrace.oneagent.destination";
    public static final String LOG_CONTEXT = "dynatrace.oneagent.log.context";
    private static final String OTEL_FORWARDING = "otel.forwarding";
    private static final String APPLICATION = "application";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    @Override
    public void start(String[] args) {
        OneAgentObserver observer = configure(OneAgentSDKFactory.createInstance(),
                AppConfigReader.getInstance()::getProperty);
        if (observer != null) {
            InvocationObservers.register(observer);
        }
    }

    /**
     * Build the observer for an SDK instance, or report why there is none.
     *
     * @param sdk the SDK instance
     * @param property reads a setting: (key, default) to value
     * @return the observer, or null when OneAgent is not attached to this JVM
     */
    static OneAgentObserver configure(OneAgentSDK sdk, BinaryOperator<String> property) {
        sdk.setLoggingCallback(new SdkLog());
        SDKState state = sdk.getCurrentState();
        if (state == SDKState.PERMANENTLY_INACTIVE) {
            log.warn("Dynatrace OneAgent is not attached to this JVM (SDK state {}) - " +
                    "invocation observer not registered", state);
            return null;
        }
        String setting = property.apply(DESTINATION, APPLICATION);
        OneAgentObserver.Destination destination = OneAgentObserver.Destination.of(setting);
        if (destination == null) {
            log.warn("{}={} is not application, route or application-route - using application", DESTINATION, setting);
            destination = OneAgentObserver.Destination.APPLICATION;
        }
        boolean logContext = TRUE.equalsIgnoreCase(property.apply(LOG_CONTEXT, FALSE));
        String application = Platform.getInstance().getName();
        log.info("Dynatrace OneAgent invocation observer - SDK state {}, destination {}, log context {}",
                state, destination.name().toLowerCase().replace('_', '-'), logContext);
        if (TRUE.equalsIgnoreCase(property.apply(OTEL_FORWARDING, FALSE))) {
            log.warn("{}=true as well - do not export the OTLP spans to the same Dynatrace tenant: " +
                    "the forwarder's trace and the PurePath are two unconnected traces of one request",
                    OTEL_FORWARDING);
        }
        return new OneAgentObserver(sdk, application, destination, logContext);
    }

    /**
     * The SDK's diagnostics, in this application's log.
     */
    static class SdkLog implements LoggingCallback {
        @Override
        public void warn(String message) {
            log.warn("OneAgent SDK: {}", message);
        }

        @Override
        public void error(String message) {
            log.error("OneAgent SDK: {}", message);
        }
    }
}
