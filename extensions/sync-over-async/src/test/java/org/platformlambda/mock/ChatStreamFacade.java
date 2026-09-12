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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.Utility;
import org.platformlambda.sync.StreamBridge;
import org.platformlambda.sync.SyncRuntime;

import java.util.Map;

/**
 * The "chat with an AI agent" facade (streaming-return-route use case 2): an {@code @EventInterceptor}
 * addressed directly by the {@code stream: true} endpoint {@code POST /api/chat} (design D2). It opens
 * the streaming rendezvous through {@link StreamBridge} and fires the request leg - here an event to the
 * in-JVM {@link MockAiBackend}; in production, any transport the application likes. The backend's
 * ordered token posts then render progressively out this pod's HTTP edge.
 */
@PreLoad(route = "chat.stream.facade", instances = 50)
@EventInterceptor
public class ChatStreamFacade implements TypedLambdaFunction<EventEnvelope, Void> {

    private static final long IDLE_SECONDS = 10;

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        String cid = request.getCorrelationId() == null
                ? Utility.getInstance().getUuid() : request.getCorrelationId();
        StreamBridge.open(SyncRuntime.coordinator(), request, cid, IDLE_SECONDS);
        // the request leg: hand the rendezvous cid to the backend service
        EventEmitter.getInstance().send(new EventEnvelope()
                .setTo(MockAiBackend.ROUTE).setHeader(SyncRuntime.CID, cid));
        return null;
    }
}
