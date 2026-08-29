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

package org.platformlambda.services;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.util.Utility;

import java.util.Map;

/**
 * This demo function serves an HTTP endpoint with progressive result set rendering
 * (HTTP response streaming). The endpoint is declared with "stream: true" in rest.yaml,
 * and the function streams a sequence of test messages slowly so that you can watch
 * them render one by one as Server-Sent Events.
 * <p>
 * A streaming producer is an interceptor - it receives the raw event envelope
 * (including the caller's reply_to address) and streams segments through the
 * EventStreamWriter until it declares end of transmission.
 * <p>
 * Optional query parameters: "delay" in milliseconds between messages
 * (default 1000, bounded 50 - 5000) and "count" for the number of messages
 * (default 10, bounded 1 - 100).
 * <p>
 * Try it with the companion script: {@code node scripts/sse-client.mjs} or with
 * {@code curl -N -H 'accept: text/event-stream' http://127.0.0.1:8085/api/hello/sse}
 */
@PreLoad(route = "hello.sse", instances = 10)
@EventInterceptor
public class HelloSse implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final long DEFAULT_DELAY_MS = 1000;
    private static final int DEFAULT_COUNT = 10;

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        var util = Utility.getInstance();
        AsyncHttpRequest http = new AsyncHttpRequest(request.getRawBody());
        String delayParam = http.getQueryParameter("delay");
        long delay = delayParam == null ? DEFAULT_DELAY_MS : Math.clamp(util.str2long(delayParam), 50, 5000);
        String countParam = http.getQueryParameter("count");
        int count = countParam == null ? DEFAULT_COUNT : Math.clamp(util.str2int(countParam), 1, 100);
        var out = new EventStreamWriter(request);
        out.first(200, "text/event-stream");
        out.write("The following messages are rendered slowly to demonstrate the SSE feature:");
        for (int i = 1; i <= count; i++) {
            util.sleep(delay);
            out.write("test message " + i);
        }
        out.close("end of SSE page.");
        return null;
    }
}
