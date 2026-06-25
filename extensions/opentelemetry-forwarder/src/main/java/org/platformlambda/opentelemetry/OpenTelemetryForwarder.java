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
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.opentelemetry.support.OtelForwarderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Drop-in {@code distributed.trace.forwarder}: Mercury's {@code Telemetry} service forwards every
 * completed trace's performance metrics here, and this function exports them to an OpenTelemetry
 * collector over OTLP - preserving the W3C trace/span/parent-span IDs Mercury already propagated.
 * <p>
 * Living under {@code org.platformlambda} (a base scan package) means simply having this jar on the
 * classpath auto-registers the route. {@code @ZeroTracing} keeps the forwarder out of the trace it
 * reports.
 * <p>
 * Configuration is read from {@code application.properties} at construction (keys
 * {@code otel.exporter.otlp.endpoint}, {@code otel.service.name}, {@code otel.exporter.otlp.headers},
 * {@code otel.exporter.otlp.timeout}, {@code otel.trace.forwarder.enabled}); values may use
 * {@code ${ENV_VAR:default}} substitution. Credentials belong in {@code otel.exporter.otlp.headers}
 * sourced from an environment variable with <b>no default value</b> so no secret is hard-coded - an
 * unset variable resolves to {@code null}, which parses to zero headers. See the module README for
 * configuration examples.
 */
@PreLoad(route = "distributed.trace.forwarder")
@ZeroTracing
public class OpenTelemetryForwarder implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryForwarder.class);

    private static final String ENABLED = "otel.trace.forwarder.enabled";
    private static final String ENDPOINT = "otel.exporter.otlp.endpoint";
    private static final String TIMEOUT = "otel.exporter.otlp.timeout";
    private static final String HEADERS = "otel.exporter.otlp.headers";
    private static final String SERVICE_NAME = "otel.service.name";
    private static final String APP_NAME = "application.name";
    private static final String DEFAULT_ENDPOINT = "http://localhost:4318/v1/traces";
    private static final String DEFAULT_TIMEOUT = "10000";
    private static final String DEFAULT_SERVICE = "mercury";

    private final OtelForwarderContext context;

    /**
     * Production entry point. {@code @PreLoad} instantiates this once; it configures itself from
     * {@code application.properties} via {@link AppConfigReader} (which resolves {@code ${ENV:default}}).
     */
    public OpenTelemetryForwarder() {
        AppConfigReader config = AppConfigReader.getInstance();
        boolean enabled = !"false".equalsIgnoreCase(config.getProperty(ENABLED, "true"));
        String serviceName = config.getProperty(SERVICE_NAME, config.getProperty(APP_NAME, DEFAULT_SERVICE));
        if (!enabled) {
            log.info("distributed.trace.forwarder present but disabled ({}=false)", ENABLED);
            this.context = new OtelForwarderContext(false, null, serviceName);
            return;
        }
        String endpoint = config.getProperty(ENDPOINT, DEFAULT_ENDPOINT);
        long timeoutMs = Utility.getInstance().str2long(config.getProperty(TIMEOUT, DEFAULT_TIMEOUT));
        // Credentials come from the environment via ${OTEL_EXPORTER_OTLP_HEADERS} in application.properties.
        // No hard-coded default (static-analysis-safe): an unset variable -> null -> "null" -> no header.
        Map<String, String> headers = OtelForwarderContext.parseHeaders(String.valueOf(config.getProperty(HEADERS)));
        SpanExporter exporter = OtelForwarderContext.buildExporter(endpoint, timeoutMs, headers);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            exporter.flush().join(timeoutMs, TimeUnit.MILLISECONDS);
            exporter.shutdown();
        }));
        log.info("OpenTelemetry trace forwarder ready - service={}, OTLP endpoint={}, credential headers={}",
                serviceName, endpoint, headers.keySet());
        this.context = new OtelForwarderContext(true, exporter, serviceName);
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
