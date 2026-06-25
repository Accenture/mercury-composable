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

package org.platformlambda.support;

/**
 * Kafka record header names exchanged on the request/response topics.
 */
public final class SyncOverAsyncHeaders {

    /** Correlation-id that ties a Kafka request/response pair back to the pending HTTP call. */
    public static final String CORRELATION_ID = "cid";

    /** W3C trace context, propagated across the async hops (same name the OpenTelemetry forwarder uses). */
    public static final String TRACE_PARENT = "traceparent";

    private SyncOverAsyncHeaders() {}
}
