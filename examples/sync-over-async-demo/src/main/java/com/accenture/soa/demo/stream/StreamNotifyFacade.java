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

package com.accenture.soa.demo.stream;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.util.Utility;
import org.platformlambda.sync.StreamBridge;
import org.platformlambda.sync.SyncRuntime;

import java.util.Map;

/**
 * The stream-ui pod's notification channel ({@code GET /api/notifications}, declared {@code stream: true}):
 * an {@code @EventInterceptor} addressed directly by the endpoint - the streaming-return-route facade
 * shape (design D2). {@code StreamBridge} does the heavy lifting (SSE head, drain-to-reply-lane, idle
 * watchdog with the single final drain); this class only announces the session's correlation-id back to
 * the UI as the first SSE event ({@code event: cid}), which the UI quotes when it POSTs to producer pods
 * so they know where to post.
 *
 * <p>The idle allowance defaults to 30s for the demo and can be set per request with the
 * {@code x-stream-idle-seconds} HTTP header (the chaos runs use a short one to show the idle-expiry
 * endings quickly; a production notification channel would use minutes).</p>
 */
@PreLoad(route = "demo.stream.facade", instances = 50)
@EventInterceptor
public class StreamNotifyFacade implements TypedLambdaFunction<EventEnvelope, Void> {

    public static final String CID_EVENT = "cid";
    public static final String IDLE_HEADER = "x-stream-idle-seconds";
    private static final long DEFAULT_IDLE_SECONDS = 30;

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        AsyncHttpRequest http = new AsyncHttpRequest(request.getBody());
        String override = http.getHeader(IDLE_HEADER);
        long idleSeconds = override == null
                ? DEFAULT_IDLE_SECONDS : Utility.getInstance().str2long(override);
        String cid = request.getCorrelationId() == null
                ? Utility.getInstance().getUuid() : request.getCorrelationId();
        EventStreamWriter writer = StreamBridge.open(SyncRuntime.coordinator(), request, cid, idleSeconds);
        // hand the session's cid to the UI before any producer can know it
        writer.write(CID_EVENT, cid);
        return null;
    }
}
