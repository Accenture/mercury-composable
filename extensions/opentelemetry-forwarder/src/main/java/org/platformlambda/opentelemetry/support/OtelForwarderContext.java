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

package org.platformlambda.opentelemetry.support;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the OTLP exporter, OpenTelemetry {@link Resource} and instrumentation scope used by the
 * {@code distributed.trace.forwarder}. The {@link org.platformlambda.opentelemetry.OpenTelemetryForwarder}
 * reads configuration from {@code application.properties} and constructs this; tests construct it
 * directly with a chosen exporter (e.g. an in-memory one).
 */
public class OtelForwarderContext {
    private static final Logger log = LoggerFactory.getLogger(OtelForwarderContext.class);

    public static final String INSTRUMENTATION_NAME = "org.platformlambda.opentelemetry-forwarder";
    private static final String VERSION = "4.5.0";
    private static final String SERVICE_NAME_KEY = "service.name";

    private final boolean enabled;
    private final SpanExporter exporter;
    private final Resource resource;
    private final InstrumentationScopeInfo scope;

    public OtelForwarderContext(boolean enabled, SpanExporter exporter, String serviceName) {
        this.enabled = enabled;
        this.exporter = exporter;
        this.resource = Resource.create(Attributes.builder().put(SERVICE_NAME_KEY, serviceName).build());
        this.scope = InstrumentationScopeInfo.builder(INSTRUMENTATION_NAME).setVersion(VERSION).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Map a Mercury trace dataset to an OpenTelemetry span and export it (non-blocking).
     */
    public void forward(Map<String, Object> dataset) {
        if (!enabled || exporter == null) {
            return;
        }
        SpanData span = TraceMetricsSpanData.map(dataset, resource, scope);
        if (span == null) {
            return;
        }
        CompletableResultCode rc = exporter.export(Collections.singletonList(span));
        rc.whenComplete(() -> {
            if (!rc.isSuccess()) {
                log.warn("OTLP export failed for span {} of trace {}", span.getSpanId(), span.getTraceId());
            }
        });
    }

    /**
     * Build the OTLP/HTTP span exporter. Header <em>values</em> are never logged.
     */
    public static SpanExporter buildExporter(String endpoint, long timeoutMs, Map<String, String> headers) {
        var builder = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofMillis(timeoutMs));
        headers.forEach(builder::addHeader);
        return builder.build();
    }

    /**
     * Parse an OpenTelemetry {@code OTEL_EXPORTER_OTLP_HEADERS}-style value: comma-separated
     * {@code key=value} pairs, split on the first {@code =} so token values may contain {@code =}.
     * A blank or {@code "null"} value (an unset credential env var) yields no headers.
     */
    public static Map<String, String> parseHeaders(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw != null) {
            for (String pair : raw.split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String key = pair.substring(0, eq).trim();
                    String val = pair.substring(eq + 1).trim();
                    if (!key.isEmpty()) {
                        out.put(key, val);
                    }
                }
            }
        }
        return out;
    }
}
