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

package org.platformlambda.mock;

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
 * The "event notification" facade (streaming-return-route use case 1): the UI opens
 * {@code GET /api/notifications} as an SSE request, and this {@code @EventInterceptor} opens the
 * rendezvous and hands the session's cid back to the UI as the first SSE event ({@code event: cid}) -
 * the UI quotes it in its subsequent POSTs so backend services know where to post. Long quiet stretches
 * are normal on such a channel, so the idle allowance is widened per request via the
 * {@code x-stream-idle-seconds} HTTP header (default 8s here; a real deployment would use minutes).
 */
@PreLoad(route = "notify.stream.facade", instances = 50)
@EventInterceptor
public class NotificationStreamFacade implements TypedLambdaFunction<EventEnvelope, Void> {

    public static final String IDLE_HEADER = "x-stream-idle-seconds";
    public static final String CID_EVENT = "cid";
    private static final long DEFAULT_IDLE_SECONDS = 8;

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        AsyncHttpRequest http = new AsyncHttpRequest(request.getBody());
        String override = http.getHeader(IDLE_HEADER);
        long idleSeconds = override == null
                ? DEFAULT_IDLE_SECONDS : Utility.getInstance().str2long(override);
        String cid = request.getCorrelationId() == null
                ? Utility.getInstance().getUuid() : request.getCorrelationId();
        EventStreamWriter writer = StreamBridge.open(SyncRuntime.coordinator(), request, cid, idleSeconds);
        // announce the session's cid before any producer can know it - after this, the serialized
        // drain is the only writer
        writer.write(CID_EVENT, cid);
        return null;
    }
}
