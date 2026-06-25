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

package org.platformlambda.opentelemetry.mock;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A mock OTLP/HTTP collector written the Mercury composable way: a plain function exposed at
 * {@code POST /api/v2/otlp/v1/traces} (and the Splunk path) by {@code rest.yaml}. Boot the test app
 * ({@code MockOtlpAppMain}) from an IDE and watch real exporters (or {@code curl}) hit the endpoint.
 * <p>
 * The HTTP body the OpenTelemetry SDK sends is OTLP <b>protobuf</b> ({@code application/x-protobuf}) -
 * a serialized {@code ExportTraceServiceRequest}. This collector <b>decodes</b> that payload and logs
 * the key span fields (trace/span IDs, name, timing, status, attributes). It also stashes the decoded
 * IDs in {@link #CAPTURED} so a test can assert that what we mapped survived the wire. It replies with
 * an empty HTTP 200 - a valid (zero-field) OTLP {@code ExportTraceServiceResponse}.
 */
@PreLoad(route = "mock.otlp.collector")
public class MockOtlpCollector implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final Logger log = LoggerFactory.getLogger(MockOtlpCollector.class);

    /** Captured requests (transport fields + decoded span fields) for assertions. */
    public static final BlockingQueue<Map<String, Object>> CAPTURED = new LinkedBlockingQueue<>();

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        Map<String, Object> received = new HashMap<>();
        received.put("method", input.getMethod());
        received.put("path", input.getUrl());
        received.put("authorization", input.getHeader("authorization"));
        received.put("content-type", input.getHeader("content-type"));

        Object raw = input.getBody();
        byte[] body = (raw instanceof byte[]) ? (byte[]) raw : null;
        decodeAndLog(body, received);

        CAPTURED.add(received);
        return new EventEnvelope().setStatus(200).setHeader("content-type", "application/x-protobuf");
    }

    /**
     * Decode the OTLP protobuf produced by the OpenTelemetry SDK and log it as human-readable
     * key-values. Best-effort: a non-protobuf or empty body is logged and skipped, never thrown.
     */
    private void decodeAndLog(byte[] body, Map<String, Object> received) {
        if (body == null || body.length == 0) {
            log.info("Mock OTLP received {} - empty/non-binary body", received.get("path"));
            return;
        }
        try {
            ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(body);
            int spanCount = 0;
            log.info("Mock OTLP received POST {} - {} bytes, {} ResourceSpans",
                    received.get("path"), body.length, request.getResourceSpansCount());
            for (ResourceSpans rs : request.getResourceSpansList()) {
                String serviceName = serviceName(rs.getResource().getAttributesList());
                log.info("  resource: service.name={}", serviceName);
                received.put("wire.service.name", serviceName);
                for (ScopeSpans ss : rs.getScopeSpansList()) {
                    for (Span span : ss.getSpansList()) {
                        spanCount++;
                        String traceId = hex(span.getTraceId());
                        String spanId = hex(span.getSpanId());
                        String parentSpanId = hex(span.getParentSpanId());
                        log.info("  span: name={} kind={} trace_id={} span_id={} parent_span_id={} "
                                        + "start={} end={} status={}",
                                span.getName(), span.getKind(), traceId, spanId, parentSpanId,
                                span.getStartTimeUnixNano(), span.getEndTimeUnixNano(),
                                span.getStatus().getCode());
                        Map<String, String> attrs = new LinkedHashMap<>();
                        for (KeyValue kv : span.getAttributesList()) {
                            String value = anyValue(kv.getValue());
                            log.info("    attr {}={}", kv.getKey(), value);
                            attrs.put(kv.getKey(), value);
                        }
                        // stash the first span's decoded fields for round-trip assertions
                        received.putIfAbsent("wire.trace_id", traceId);
                        received.putIfAbsent("wire.span_id", spanId);
                        received.putIfAbsent("wire.parent_span_id", parentSpanId);
                        received.putIfAbsent("wire.span_name", span.getName());
                        received.putIfAbsent("wire.attributes", attrs);
                    }
                }
            }
            received.put("wire.span_count", spanCount);
        } catch (Exception e) {
            log.warn("Mock OTLP received {} - not a parseable OTLP payload: {}", received.get("path"), e.toString());
        }
    }

    private static String serviceName(java.util.List<KeyValue> attributes) {
        for (KeyValue kv : attributes) {
            if ("service.name".equals(kv.getKey())) {
                return anyValue(kv.getValue());
            }
        }
        return null;
    }

    private static String anyValue(AnyValue v) {
        if (v.hasStringValue()) {
            return v.getStringValue();
        }
        if (v.hasIntValue()) {
            return String.valueOf(v.getIntValue());
        }
        if (v.hasDoubleValue()) {
            return String.valueOf(v.getDoubleValue());
        }
        if (v.hasBoolValue()) {
            return String.valueOf(v.getBoolValue());
        }
        return v.toString().trim();
    }

    /** OTLP carries IDs as raw bytes (trace_id 16 bytes, span_id 8 bytes); render them as hex. */
    private static String hex(ByteString bytes) {
        StringBuilder sb = new StringBuilder(bytes.size() * 2);
        for (byte b : bytes.toByteArray()) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
