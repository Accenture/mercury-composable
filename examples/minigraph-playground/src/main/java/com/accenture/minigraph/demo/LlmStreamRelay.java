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

package com.accenture.minigraph.demo;

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
 * The AI-token streaming composition (agent-orchestration follow-up to experiment E0):
 * this endpoint's function forwards its own reply lane and correlation id into a 'send'
 * to the event-over-http mapped "llm.stream" function - the python demo app's streaming
 * AI node, which pulls the provider's REAL token stream (Gemini or Claude models) - and
 * opts in with the "accept: text/event-stream" event header. The provider's token
 * batches relay through the peer's /api/event in envelope mode and re-render
 * progressively out this application's HTTP edge as SSE, with no imperative streaming
 * code and no LLM dependency in the engine.
 * <p>
 * Requires yaml.event.over.http (see application.properties) and a running wrapper
 * demo app with the provider credential in its environment.
 */
@PreLoad(route = "llm.stream.relay", instances = 50)
@EventInterceptor
public class LlmStreamRelay implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final String REMOTE_ROUTE = "llm.stream";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        var po = new PostOffice(headers, instance);
        // reachable when registered locally (a test mock) or mapped declaratively
        if (!po.exists(REMOTE_ROUTE) && EventEmitter.getInstance().getEventHttpTarget(REMOTE_ROUTE) == null) {
            new EventStreamWriter(request).fail(new AppException(503,
                    "AI streaming demo is not configured - start a wrapper demo app with an LLM " +
                    "provider credential and map llm.stream in event-over-http.yaml"));
            return null;
        }
        AsyncHttpRequest http = new AsyncHttpRequest(request.getRawBody());
        EventEnvelope forward = new EventEnvelope().setTo(REMOTE_ROUTE)
                .setReplyTo(request.getReplyTo())
                .setCorrelationId(request.getCorrelationId())
                // the POST body carries the AI node's request surface
                // (prompt | messages, system, params)
                .setBody(http.getBody())
                // the event-level opt-in for progressive streaming over Event-over-HTTP
                .setHeader("accept", "text/event-stream")
                // idle allowance between stream events on both hops (ms) - an LLM can
                // pause between token batches while it reasons
                .setHeader("x-ttl", "60000");
        // the trace-aware PostOffice stamps the current trace onto the outbound event,
        // so the distributed trace continues across the hop into the AI node
        po.send(forward);
        return null;
    }
}
