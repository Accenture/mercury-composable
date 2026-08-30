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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;

/**
 * The engine-to-wrapper streaming composition: this endpoint's function forwards
 * its own reply lane and correlation id into a 'send' to the event-over-http mapped
 * "hello.tokens" function - the python/node demo apps' streaming function - and
 * opts in with the "accept: text/event-stream" event header. The remote segments
 * relay through the peer's /api/event in envelope mode and re-render progressively
 * out this application's HTTP edge, with no imperative streaming code in between.
 * <p>
 * Requires yaml.event.over.http=classpath:/event-over-http.yaml (see
 * application.properties) and a running wrapper demo app - the README's
 * "Streaming from a remote function" section walks through it.
 */
@PreLoad(route = "hello.remote.relay", instances = 10)
@EventInterceptor
public class HelloRemoteRelay implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final String REMOTE_ROUTE = "hello.tokens";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        if (EventEmitter.getInstance().getEventHttpTarget(REMOTE_ROUTE) == null) {
            // teaching failure: the demo depends on the declarative routing map
            new EventStreamWriter(request).fail(new AppException(503,
                    "Remote streaming demo is not configured - start a wrapper demo app and run " +
                    "this application with -Dyaml.event.over.http=classpath:/event-over-http.yaml"));
            return null;
        }
        AsyncHttpRequest http = new AsyncHttpRequest(request.getRawBody());
        EventEnvelope forward = new EventEnvelope().setTo(REMOTE_ROUTE)
                .setReplyTo(request.getReplyTo())
                .setCorrelationId(request.getCorrelationId())
                // the event-level opt-in for progressive streaming over Event-over-HTTP
                .setHeader("accept", "text/event-stream")
                // idle allowance between stream events on both hops (ms)
                .setHeader("x-ttl", "30000");
        String delay = http.getQueryParameter("delay");
        if (delay != null) {
            forward.setHeader("delay", delay);
        }
        String count = http.getQueryParameter("count");
        if (count != null) {
            forward.setHeader("count", count);
        }
        // a trace-aware PostOffice stamps the current trace onto the outbound
        // event, so the distributed trace continues across the hop
        new PostOffice(headers, instance).send(forward);
        return null;
    }
}
