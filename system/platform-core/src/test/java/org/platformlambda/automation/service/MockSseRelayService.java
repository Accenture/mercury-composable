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

import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;

import java.util.Map;

/**
 * The SSE-to-SSE relay demo: a streaming endpoint whose function forwards its own
 * reply_to and correlation id into an async.http.request aimed at this very
 * application's SSE demo endpoint. The HTTP client consumes the upstream stream
 * progressively and its segments ride the caller's reply lane straight out the edge -
 * an SSE relay with no imperative streaming code.
 */
@EventInterceptor
public class MockSseRelayService implements TypedLambdaFunction<EventEnvelope, Void> {

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", config.getProperty("server.port", "8100"));
        AsyncHttpRequest upstream = new AsyncHttpRequest();
        upstream.setMethod("GET");
        upstream.setTargetHost("http://127.0.0.1:" + port);
        upstream.setUrl("/api/hello/stream");
        upstream.setQueryParameter("mode", "sse");
        // explicit Accept opts into progressive SSE consumption (D1)
        upstream.setHeader("accept", "text/event-stream");
        upstream.setTimeoutSeconds(10);
        EventEmitter.getInstance().send(new EventEnvelope()
                .setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(upstream.toMap())
                .setReplyTo(request.getReplyTo())
                .setCorrelationId(request.getCorrelationId()));
        return null;
    }
}
