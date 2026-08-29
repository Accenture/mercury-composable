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

package org.platformlambda.automation.service;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;

import java.util.Map;

/**
 * The Event-over-HTTP streaming composition: a streaming edge endpoint whose
 * function forwards its own reply lane and correlation id into a 'send' to a
 * REMOTE (event-over-http mapped) streaming function, opting in with the
 * "accept: text/event-stream" event header. The remote function's segments
 * relay through "/api/event" and re-render progressively out this app's edge -
 * engine-to-engine streaming with no imperative streaming code.
 */
@EventInterceptor
public class MockRemoteRelayService implements TypedLambdaFunction<EventEnvelope, Void> {

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        AsyncHttpRequest http = new AsyncHttpRequest(request.getRawBody());
        String mode = http.getQueryParameter("mode");
        EventEmitter.getInstance().send(new EventEnvelope().setTo("hello.stream.remote")
                .setReplyTo(request.getReplyTo())
                .setCorrelationId(request.getCorrelationId())
                .setHeader("accept", "text/event-stream")
                .setHeader("x-ttl", "10000")
                .setHeader("mode", mode == null? "tokens" : mode));
        return null;
    }
}
