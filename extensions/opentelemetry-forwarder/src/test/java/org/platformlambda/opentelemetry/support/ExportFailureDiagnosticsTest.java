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

import io.opentelemetry.sdk.common.export.HttpResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A rejected export must be diagnosable from the forwarder's own log line.
 *
 * <p>This exists because of a real dead end: a first live run against a SaaS backend logged
 * {@code OTLP export failed ... FailedExportException$HttpExportException} for every span - the
 * exception's {@code toString()} is just its class name - while the actual cause (HTTP 404, the signal
 * path missing from the endpoint) sat in the SDK's separate JUL warning, uncorrelated and easy to miss in
 * a JSON application log.</p>
 *
 * <p>The <em>exception</em> is duck-typed here rather than built from the SDK's real one - that is the
 * point of reaching it reflectively: neither the production code nor this test depends on an unstable
 * {@code io.opentelemetry.exporter.internal} type. The <em>response</em> is the SDK's genuine
 * {@link HttpResponse}, which is public API in a compile-scope artifact, so the fields are read through a
 * stable type on both sides.</p>
 */
class ExportFailureDiagnosticsTest {

    /** Mimics the SDK's internal HTTP failure: a Throwable exposing getResponse(), read reflectively. */
    static class FakeHttpFailure extends RuntimeException {
        private final HttpResponse response;

        FakeHttpFailure(int status, String statusMessage, String body) {
            this.response = new FakeResponse(status, statusMessage, body);
        }

        @SuppressWarnings("unused")   // read reflectively, exactly as the SDK's own type is
        public HttpResponse getResponse() {
            return response;
        }
    }

    /** The SDK's real response contract - public API, so no duck typing needed on this side. */
    record FakeResponse(int status, String statusMessage, String body) implements HttpResponse {
        @Override
        public int getStatusCode() {
            return status;
        }

        @Override
        public String getStatusMessage() {
            return statusMessage;
        }

        @Override
        public byte[] getResponseBody() {
            return body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Test
    void a404NamesTheMostLikelyCause() {
        String text = OtelForwarderContext.describeFailure(new FakeHttpFailure(404, "Not Found", null));
        assertTrue(text.startsWith("HTTP 404 Not Found"), "the status must lead the line, got: " + text);
        assertTrue(text.contains("signal path"),
                "a 404 is nearly always the endpoint missing /v1/traces - say so: " + text);
        assertTrue(text.contains("/v1/traces"), "the hint should show the shape expected: " + text);
    }

    @Test
    void a401PointsAtTheCredentialRatherThanTheEndpoint() {
        String text = OtelForwarderContext.describeFailure(new FakeHttpFailure(401, "Unauthorized", null));
        assertTrue(text.startsWith("HTTP 401"), text);
        assertTrue(text.contains("otel.exporter.otlp.headers"), "name the key to fix: " + text);
        assertFalse(text.contains("signal path"), "a 401 is not an endpoint-path problem: " + text);
    }

    @Test
    void a403PointsAtThePermissionRatherThanTheCredential() {
        // Observed live: the token authenticated fine but lacked a scope, and Dynatrace said so in the
        // body. Telling the reader to re-check the header would have sent them the wrong way.
        String text = OtelForwarderContext.describeFailure(new FakeHttpFailure(
                403, "Forbidden", "User is missing required permission: openpipeline:traces:ingest"));
        assertTrue(text.startsWith("HTTP 403 Forbidden"), text);
        assertTrue(text.contains("openpipeline:traces:ingest"),
                "the backend names the missing scope - that is the actionable part: " + text);
        assertTrue(text.contains("lacks permission"), "distinguish 403 from a bad credential: " + text);
        assertFalse(text.contains("the header name and any auth scheme"),
                "a 403 is not a malformed-header problem: " + text);
    }

    @Test
    void theResponseBodyIsIncludedButBounded() {
        String text = OtelForwarderContext.describeFailure(
                new FakeHttpFailure(400, "Bad Request", "span 0 rejected: invalid trace id"));
        assertTrue(text.contains("span 0 rejected: invalid trace id"),
                "the backend's own explanation is the most useful part: " + text);

        // a verbose backend must not flood the log with one span's rejection
        String flood = OtelForwarderContext.describeFailure(
                new FakeHttpFailure(400, "Bad Request", "x".repeat(5000)));
        assertTrue(flood.length() < 600, "the body must be truncated, was " + flood.length() + " chars");
        assertTrue(flood.endsWith("..."), "truncation should be visible: " + flood.substring(flood.length() - 20));
    }

    @Test
    void aNonHttpCauseFallsBackToItsOwnDescription() {
        // a connect timeout carries no HTTP response - the reflective read must degrade, not throw
        String text = OtelForwarderContext.describeFailure(new java.net.SocketTimeoutException("connect timed out"));
        assertTrue(text.contains("SocketTimeoutException"), text);
        assertTrue(text.contains("connect timed out"), text);
    }

    @Test
    void aMissingCauseIsStatedRatherThanNull() {
        assertEquals("no cause reported", OtelForwarderContext.describeFailure(null));
    }
}
