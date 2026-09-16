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

package org.platformlambda.opentelemetry;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.opentelemetry.support.OtelForwarderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Drop-in {@code distributed.trace.forwarder}: Mercury's {@code Telemetry} service forwards every
 * completed trace's performance metrics here, and this function exports them to an OpenTelemetry
 * collector over OTLP - preserving the W3C trace/span/parent-span IDs Mercury already propagated.
 * <p>
 * Living under {@code org.platformlambda} (a base scan package) means the jar alone would auto-register
 * the route, so the feature is gated by {@code @OptionalService("otel.forwarding")}: an application can
 * carry the dependency while DevOps decides, per environment, whether traces leave the process.
 * <b>The default is OFF</b> - the route is not registered at all unless {@code otel.forwarding=true},
 * which can be set in {@code application.properties} or at runtime with {@code -Dotel.forwarding=true}.
 * {@code @ZeroTracing} keeps the forwarder out of the trace it reports.
 * <p>
 * Configuration is read from {@code application.properties} at construction (keys
 * {@code otel.exporter.otlp.endpoint}, {@code otel.service.name}, {@code otel.exporter.otlp.headers},
 * {@code otel.exporter.otlp.timeout}, {@code otel.exporter.otlp.connect.timeout},
 * {@code otel.exporter.otlp.compression}); values may use
 * {@code ${ENV_VAR:default}} substitution. Credentials belong in {@code otel.exporter.otlp.headers}
 * sourced from an environment variable with <b>no default value</b> so no secret is hard-coded - an
 * unset variable resolves to {@code null}, which parses to zero headers. See the module README for
 * configuration examples.
 * <p>
 * <b>The credential is resolved per export, not once at construction.</b> A {@code @PreLoad} function is
 * built BEFORE any {@code @MainApplication} credential bootstrap runs, so a token published by a vault
 * loader would otherwise be frozen out for the life of the process. Re-reading it means such a token
 * takes effect without a restart; a credential supplied the ordinary way (an environment variable set
 * before the JVM starts) behaves exactly as before.
 */
@OptionalService("otel.forwarding")
@PreLoad(route = "distributed.trace.forwarder")
@ZeroTracing
public class OpenTelemetryForwarder implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryForwarder.class);

    private static final String ENDPOINT = "otel.exporter.otlp.endpoint";
    private static final String TIMEOUT = "otel.exporter.otlp.timeout";
    private static final String CONNECT_TIMEOUT = "otel.exporter.otlp.connect.timeout";
    private static final String COMPRESSION = "otel.exporter.otlp.compression";
    private static final String HEADERS = "otel.exporter.otlp.headers";
    private static final String SERVICE_NAME = "otel.service.name";
    private static final String APP_NAME = "application.name";
    private static final String DEFAULT_ENDPOINT = "http://localhost:4318/v1/traces";
    private static final String DEFAULT_TIMEOUT = "10000";
    private static final String DEFAULT_CONNECT_TIMEOUT = "10000";
    private static final String DEFAULT_COMPRESSION = "none";
    private static final String DEFAULT_SERVICE = "mercury";

    private final OtelForwarderContext context;

    /**
     * Production entry point. {@code @PreLoad} instantiates this once; it configures itself from
     * {@code application.properties} via {@link AppConfigReader} (which resolves {@code ${ENV:default}}).
     */
    public OpenTelemetryForwarder() {
        AppConfigReader config = AppConfigReader.getInstance();
        String serviceName = config.getProperty(SERVICE_NAME, config.getProperty(APP_NAME, DEFAULT_SERVICE));
        String endpoint = config.getProperty(ENDPOINT, DEFAULT_ENDPOINT);
        Utility util = Utility.getInstance();
        long timeoutMs = util.str2long(config.getProperty(TIMEOUT, DEFAULT_TIMEOUT));
        long connectTimeoutMs = util.str2long(config.getProperty(CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT));
        String compression = config.getProperty(COMPRESSION, DEFAULT_COMPRESSION);
        // Credentials come from the environment, interpolated into otel.exporter.otlp.headers in
        // application.properties (the variable name is the application's choice - see the module README).
        // No hard-coded default (static-analysis-safe): an unset variable -> null -> "null" -> no header.
        // Read through a supplier so a credential published AFTER this @PreLoad constructor (a vault
        // bootstrap at @MainApplication) is picked up rather than frozen out.
        Supplier<Map<String, String>> headers = OtelForwarderContext.reloadingHeaders(
                () -> String.valueOf(AppConfigReader.getInstance().getProperty(HEADERS)));
        SpanExporter exporter =
                OtelForwarderContext.buildExporter(endpoint, timeoutMs, connectTimeoutMs, compression, headers);
        // The platform owns ONE JVM shutdown hook and runs registered callbacks in reverse
        // registration order, each error-isolated. Registering here - where the exporter is actually
        // opened - rather than hand-rolling addShutdownHook means this flush is ordered with every
        // other component's teardown instead of racing it on its own thread.
        Platform.getInstance().onShutdown(() -> {
            exporter.flush().join(timeoutMs, TimeUnit.MILLISECONDS);
            exporter.shutdown();
        });
        var headerKeys = headers.get().keySet();
        log.info("OpenTelemetry trace forwarder ready - service={}, OTLP endpoint={}, compression={}, "
                + "credential headers={}", serviceName, endpoint, compression, headerKeys);
        if (headerKeys.isEmpty()) {
            log.info("No OTLP credential header yet ({} unset) - it is re-read on every export, so a "
                    + "credential published later by a @MainApplication bootstrap takes effect without "
                    + "a restart", HEADERS);
        }
        this.context = new OtelForwarderContext(exporter, serviceName);
    }

    /**
     * Test seam: inject a context (e.g. one backed by an in-memory exporter).
     */
    OpenTelemetryForwarder(OtelForwarderContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        if (input instanceof Map) {
            context.forward((Map<String, Object>) input);
        }
        return null;
    }
}
