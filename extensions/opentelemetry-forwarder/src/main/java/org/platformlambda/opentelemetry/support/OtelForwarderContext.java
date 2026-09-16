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
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Holds the OTLP exporter, OpenTelemetry {@link Resource} and instrumentation scope used by the
 * {@code distributed.trace.forwarder}. The {@link org.platformlambda.opentelemetry.OpenTelemetryForwarder}
 * reads configuration from {@code application.properties} and constructs this; tests construct it
 * directly with a chosen exporter (e.g. an in-memory one).
 */
public class OtelForwarderContext {
    private static final Logger log = LoggerFactory.getLogger(OtelForwarderContext.class);

    public static final String INSTRUMENTATION_NAME = "org.platformlambda.opentelemetry-forwarder";
    private static final String SERVICE_NAME_KEY = "service.name";

    private final SpanExporter exporter;
    private final Resource resource;
    private final InstrumentationScopeInfo scope;

    public OtelForwarderContext(SpanExporter exporter, String serviceName) {
        this.exporter = exporter;
        this.resource = Resource.create(Attributes.builder().put(SERVICE_NAME_KEY, serviceName).build());
        // The instrumentation-scope version is resolved at runtime from the running application
        // (jar manifest, else the info.app.version parameter), so it can never go stale the way
        // a hard-coded constant did across releases.
        var util = Utility.getInstance();
        this.scope = InstrumentationScopeInfo.builder(INSTRUMENTATION_NAME)
                                             .setVersion(util.getVersion()).build();
    }

    /**
     * Whether this context can export. There is no separate enable flag: reaching this class at all
     * already means {@code otel.forwarding=true} let the {@code @OptionalService} gate register the
     * forwarder, so having somewhere to send is the only remaining condition.
     */
    public boolean isEnabled() {
        return exporter != null;
    }

    /**
     * Map a Mercury trace dataset to an OpenTelemetry span and export it (non-blocking).
     */
    public void forward(Map<String, Object> dataset) {
        if (exporter == null) {
            return;
        }
        SpanData span = TraceMetricsSpanData.map(dataset, resource, scope);
        if (span == null) {
            return;
        }
        CompletableResultCode rc = exporter.export(Collections.singletonList(span));
        rc.whenComplete(() -> {
            if (!rc.isSuccess()) {
                // the cause makes a dropped span diagnosable (a bare "failed" hid the reason
                // behind an occasional CI flake for weeks)
                Throwable cause = rc.getFailureThrowable();
                log.warn("OTLP export failed for span {} of trace {} - {}", span.getSpanId(),
                        span.getTraceId(), describeFailure(cause));
            }
        });
    }

    /**
     * Render an export failure so it is actionable from THIS log line alone.
     * <p>
     * The SDK's HTTP failure exception has a {@code toString()} of just its class name, so a rejection by
     * the backend reads as an unexplained failure; the status code appears only in the SDK's own JUL
     * warning - a different logger, not correlated with the span, and easy to miss entirely in a
     * JSON-formatted application log. This pulls the status out and adds a hint for the two rejections
     * that actually happen: a <b>404</b> is nearly always the signal path missing from the endpoint (the
     * exporter needs the full {@code .../v1/traces} URL, not the vendor's base URL), and a
     * <b>401/403</b> is the credential.
     * <p>
     * The status is read <b>reflectively</b> on purpose. The exception type lives in
     * {@code io.opentelemetry.exporter.internal}, which OpenTelemetry marks as unstable and may change
     * without notice, and its artifact is only a <em>runtime</em> dependency here. Type-depending on it
     * would trade a silent break at the next OTel upgrade for a diagnostic nicety; reflection on a
     * failure-only path costs nothing measurable and degrades to the plain {@code toString()} if the
     * shape ever changes. Response bodies are truncated and request headers are never touched, so no
     * credential can reach the log.
     */
    static String describeFailure(Throwable cause) {
        if (cause == null) {
            return "no cause reported";
        }
        String http = describeHttpFailure(cause);
        return http != null ? http : cause.toString();
    }

    /** {@code null} when the cause is not an HTTP failure carrying a response we can read. */
    private static String describeHttpFailure(Throwable cause) {
        HttpResponse response = httpResponseOf(cause);
        if (response == null) {
            return null;
        }
        int status = response.getStatusCode();
        StringBuilder sb = new StringBuilder("HTTP ").append(status);
        String statusMessage = response.getStatusMessage();
        if (statusMessage != null && !statusMessage.isBlank()) {
            sb.append(' ').append(statusMessage);
        }
        String body = responseBody(response.getResponseBody());
        if (!body.isEmpty()) {
            sb.append(" - ").append(body);
        }
        String hint = hintFor(status);
        if (hint != null) {
            sb.append(" | ").append(hint);
        }
        return sb.toString();
    }

    /**
     * Pull the HTTP response off the SDK's failure exception.
     * <p>
     * Reflection reaches only as far as the {@code getResponse()} accessor, whose declaring type lives in
     * the unstable {@code io.opentelemetry.exporter.internal} package and is a runtime-scope artifact
     * here. The RESULT is then cast to {@link HttpResponse} - public API in {@code sdk-common}, a
     * compile-scope dependency - so every field is read through a stable type. Reading the fields
     * reflectively instead does not work: the response implementation class is package-private, so
     * {@code Method.invoke} on it fails with an access error, which is exactly how the first attempt at
     * this silently fell back to the useless bare class name.
     */
    private static HttpResponse httpResponseOf(Throwable cause) {
        try {
            var accessor = cause.getClass().getMethod("getResponse");
            return accessor.invoke(cause) instanceof HttpResponse response ? response : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // not an HTTP failure (e.g. a connect timeout), or the SDK's internal shape changed
            return null;
        }
    }

    private static final int MAX_BODY_CHARS = 256;

    private static String responseBody(byte[] raw) {
        if (raw.length == 0) {
            return "";
        }
        String body = new String(raw, java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        return body.length() > MAX_BODY_CHARS ? body.substring(0, MAX_BODY_CHARS) + "..." : body;
    }

    private static String hintFor(int status) {
        return switch (status) {
            case 404 -> "check otel.exporter.otlp.endpoint includes the signal path "
                    + "(e.g. .../v1/traces), not just the vendor base URL";
            case 401 -> "the backend rejected the credential itself - check otel.exporter.otlp.headers "
                    + "(the header name and any auth scheme must match what the backend expects)";
            // 403 means the credential was ACCEPTED but lacks a scope. Seen live against Dynatrace as
            // "User is missing required permission: openpipeline:traces:ingest" - the response body
            // names the missing scope, so point the reader at it rather than at the header.
            case 403 -> "the credential was accepted but lacks permission - grant the trace-ingest "
                    + "scope on the token (the response body above names it)";
            case 413 -> "the backend rejected the payload as too large";
            case 429 -> "the backend is rate-limiting; the exporter retries with backoff";
            default -> null;
        };
    }

    /** OTLP/HTTP compression: {@code gzip} or {@code none} (the OpenTelemetry default). */
    private static final String NO_COMPRESSION = "none";
    /** Matches the OpenTelemetry SDK's default connect timeout (10s), so an unconfigured build is unchanged. */
    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 10_000;

    /**
     * Build the OTLP/HTTP span exporter with defaults (no compression, 10s connect timeout).
     * Header <em>values</em> are never logged.
     */
    public static SpanExporter buildExporter(String endpoint, long timeoutMs, Map<String, String> headers) {
        return buildExporter(endpoint, timeoutMs, DEFAULT_CONNECT_TIMEOUT_MS, NO_COMPRESSION, headers);
    }

    /**
     * Build the exporter with a FIXED header map. Convenience for tests and for callers whose
     * credentials are known at construction; production uses the {@link Supplier} overload so a
     * late-published credential is not frozen out.
     */
    public static SpanExporter buildExporter(String endpoint, long timeoutMs, long connectTimeoutMs,
                                             String compression, Map<String, String> headers) {
        Map<String, String> fixed = headers == null ? Map.of() : Map.copyOf(headers);
        return buildExporter(endpoint, timeoutMs, connectTimeoutMs, compression, () -> fixed);
    }

    /**
     * Wrap a raw {@code OTEL_EXPORTER_OTLP_HEADERS}-style supplier as a credential-header supplier that
     * reparses on every call, and announces ONCE when a credential first resolves.
     * <p>
     * The announcement matters for the late-credential case: an operator whose vault bootstrap publishes
     * the token after start-up sees "no credential yet" at boot and then a single confirmation line when
     * exports start carrying it - instead of having to infer it from the absence of warnings. Header
     * <em>values</em> are never logged, only key names.
     */
    public static Supplier<Map<String, String>> reloadingHeaders(Supplier<String> rawSupplier) {
        AtomicBoolean announced = new AtomicBoolean(false);
        return () -> {
            Map<String, String> headers = parseHeaders(rawSupplier.get());
            if (!headers.isEmpty() && announced.compareAndSet(false, true)) {
                log.info("OTLP credential header resolved - {}", headers.keySet());
            }
            return headers;
        };
    }

    /**
     * Build the OTLP/HTTP span exporter. {@code compression} is {@code gzip} or {@code none} (a blank
     * value is treated as {@code none}); {@code connectTimeoutMs} bounds the TCP/TLS handshake, separate
     * from the per-export {@code timeoutMs}. Header <em>values</em> are never logged.
     */
    public static SpanExporter buildExporter(String endpoint, long timeoutMs, long connectTimeoutMs,
                                             String compression, Supplier<Map<String, String>> headers) {
        String encoding = (compression == null || compression.isBlank()) ? NO_COMPRESSION : compression.trim();
        // Exports must never be REJECTED at enqueue: the sender's managed dispatcher runs a
        // zero-queue thread pool (core=0, SynchronousQueue) whose execute() rejects during
        // transient full-occupancy races - surfaced in CI as InterruptedIOException
        // "executor rejected", dropping the span before any interceptor (and therefore any
        // retry policy) could run. A small fixed pool with an UNBOUNDED queue makes
        // saturation mean "later", never "lost"; the wrapper below ties the pool's lifecycle
        // to the exporter so a closed exporter leaks no threads.
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), OtelForwarderContext::exportThread);
        pool.allowCoreThreadTimeOut(true);
        var builder = OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofMillis(timeoutMs))
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setCompression(encoding)
                .setExecutorService(pool)
                // The SDK's default retry whitelists only a few IOException types (connect/socket
                // timeouts, UnknownHost, SocketException); anything else - notably a pooled
                // keep-alive connection the server closed as we reused it ("unexpected end of
                // stream") - fails on the FIRST attempt and silently drops the span. Telemetry
                // delivery is at-least-once by design: duplicates are tolerated, drops are what
                // hurt, so every IOException is worth the default bounded backoff (5 attempts,
                // 1s..5s). HTTP status handling is unchanged (retry on 429/502/503/504 only).
                .setRetryPolicy(RetryPolicy.builder().setRetryExceptionPredicate(e -> true).build())
                // Headers are supplied per export, NOT baked in at build time. A @PreLoad function is
                // constructed BEFORE any @MainApplication credential bootstrap runs (AppStarter:
                // BeforeApplication -> preload() -> MainApplication), so resolving the token once here
                // would freeze it as absent for the life of the process and every export would 401
                // forever - visible only as a per-span warning. Re-reading makes a vault-published
                // credential take effect without a restart, the same lazy-resolution shape the Kafka
                // and Redis health checks use.
                .setHeaders(headers);
        return new PooledSpanExporter(builder.build(), pool);
    }

    private static Thread exportThread(Runnable r) {
        Thread t = new Thread(r, "otlp-export");
        t.setDaemon(true);
        return t;
    }

    /**
     * Ties the export thread pool's lifecycle to the exporter: the SDK treats a caller-supplied
     * {@code ExecutorService} as unmanaged and leaves it running on shutdown, which would leak
     * two threads per exporter (tests build many). Everything else delegates verbatim.
     */
    private record PooledSpanExporter(SpanExporter delegate, ThreadPoolExecutor pool) implements SpanExporter {

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            return delegate.export(spans);
        }

        @Override
        public CompletableResultCode flush() {
            return delegate.flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            CompletableResultCode rc = delegate.shutdown();
            pool.shutdown();
            return rc;
        }
    }

    /**
     * Parse an OpenTelemetry {@code OTEL_EXPORTER_OTLP_HEADERS}-style value: comma-separated pairs,
     * each split on the FIRST {@code =} <em>or</em> {@code :}, whichever appears earlier.
     * <p>
     * {@code =} is the OpenTelemetry environment-variable convention; {@code :} is literal HTTP header
     * syntax, which is what an operator naturally writes and what a backend's own documentation shows
     * ({@code Authorization: Api-Token <token>}). Accepting both means a deployment can compose the
     * header from a vendor-specific prefix and a bare secret without the two having to agree on a
     * separator. Splitting on the first occurrence only means the VALUE may contain either character -
     * a base64 token ending in {@code =}, or a URL with {@code https://} - while the NAME may contain
     * neither, so the earlier separator always delimits the name.
     * <p>
     * A blank or {@code "null"} value (an unset credential env var) yields no headers. A value cannot
     * itself contain a comma: the list is split on {@code ,} first.
     */
    public static Map<String, String> parseHeaders(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw != null) {
            for (String pair : raw.split(",")) {
                int sep = firstSeparator(pair);
                if (sep > 0) {
                    String key = pair.substring(0, sep).trim();
                    String val = pair.substring(sep + 1).trim();
                    if (!key.isEmpty()) {
                        out.put(key, val);
                    }
                }
            }
        }
        return out;
    }

    /** Index of the first {@code =} or {@code :} in the pair, or -1 when it holds neither. */
    private static int firstSeparator(String pair) {
        int eq = pair.indexOf('=');
        int colon = pair.indexOf(':');
        if (eq < 0) {
            return colon;
        }
        if (colon < 0) {
            return eq;
        }
        return Math.min(eq, colon);
    }
}
