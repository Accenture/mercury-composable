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

package org.platformlambda.core.services;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 * <p>
 * This service is traced so the "/api/event" edge is visible in the span tree:
 * its span parents onto the remote caller's span (carried by the inbound trace
 * headers), the relayed local event carries this service's span so the target
 * function parents onto it, and the HTTP response leg does the same.
 */
@EventInterceptor
@PreLoad(route=EventApiService.EVENT_API_SERVICE, instances=250)
public class EventApiService implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String EVENT_API_SERVICE = "event.api.service";
    private static final Logger log = LoggerFactory.getLogger(EventApiService.class);
    private static final String TYPE = "type";
    private static final String ASYNC = "async";
    private static final String DELIVERED = "delivered";
    private static final String TIME = "time";
    private static final String CONTENT_TYPE = "content-type";
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String X_TTL = "x-ttl";
    private static final String X_ASYNC = "X-Async";
    private static final String MISSING_ROUTING_PATH = "Missing routing path";
    private static final String PRIVATE_FUNCTION = " is private";
    private static final String ROUTE = "Route ";
    private static final String NOT_FOUND = " not found";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        if (input.getRawBody() instanceof Map && input.getReplyTo() != null) {
            Utility util = Utility.getInstance();
            AsyncHttpRequest request = new AsyncHttpRequest(input.getRawBody());
            Map<String, String> sessionInfo = request.getSessionInfo();
            long timeout = Math.max(1000, util.str2long(request.getHeader(X_TTL)));
            boolean async = "true".equals(request.getHeader(X_ASYNC));
            PostOffice po = new PostOffice(headers, instance);
            // capture this service's span on the worker thread - the trace context is
            // thread-keyed and torn down when the worker returns, so the async RPC
            // callbacks below cannot look it up later
            TraceInfo trace = po.getTrace();
            String spanId = trace == null? null : trace.spanId;
            if (request.getBody() instanceof byte[] b) {
                try {
                    handleRequest(sessionInfo, po, spanId, b, input, timeout, async);
                } catch (Exception e) {
                    // the envelope could not be decoded, so its format is unknown -
                    // fall back to the classic compact format for the error reply
                    sendError(input, spanId, EventEnvelope.Format.COMPACT, 400, e.getMessage());
                }
            } else {
                sendError(input, spanId, EventEnvelope.Format.COMPACT, 500, "Invalid event-over-http data format");
            }
        }
        return null;
    }

    private void handleRequest(Map<String, String> sessionInfo, PostOffice po, String spanId,
                               byte[] requestBody, EventEnvelope input,
                               long timeout, boolean async) {
        EventEnvelope request = new EventEnvelope(requestBody);
        // Mirror the requester's serialization format in the response so a
        // caller on either wire format (compact or standard) needs no
        // configuration to read the reply.
        EventEnvelope.Format format = request.getWireFormat() == null?
                EventEnvelope.Format.COMPACT : request.getWireFormat();
        // propagate session info if any
        sessionInfo.forEach(request::setHeader);
        if (request.getTo() != null) {
            if (po.exists(request.getTo())) {
                if (Platform.getInstance().isPrivate(request.getTo())) {
                    sendError(input, spanId, format, 403, request.getTo() + PRIVATE_FUNCTION);
                } else {
                    if (async) {
                        // Drop-n-forget
                        po.send(request);
                        Map<String, Object> ackBody = new HashMap<>();
                        ackBody.put(TYPE, ASYNC);
                        ackBody.put(DELIVERED, true);
                        ackBody.put(TIME, new Date());
                        EventEnvelope pending = new EventEnvelope().setStatus(202).setBody(ackBody);
                        sendResponse(input, spanId, format, pending);
                    } else {
                        // RPC - po.asyncRequest stamps this service's span on the relayed
                        // event (worker thread), so the target function parents onto it
                        po.asyncRequest(request, timeout)
                                .onSuccess(result -> sendResponse(input, spanId, format, result))
                                .onFailure(e -> sendError(input, spanId, format, 408, e.getMessage()));
                    }
                }
            } else {
                sendError(input, spanId, format, 404, ROUTE + request.getTo() + NOT_FOUND);
            }
        } else {
            sendError(input, spanId, format, 400, MISSING_ROUTING_PATH);
        }
    }

    private void sendResponse(EventEnvelope input, String spanId, EventEnvelope.Format format, EventEnvelope result) {
        try {
            EventEnvelope response = new EventEnvelope().setTo(input.getReplyTo())
                    .setFrom(EVENT_API_SERVICE)
                    .setTrace(input.getTraceId(), input.getTracePath())
                    .setSpanId(spanId)
                    .setCorrelationId(input.getCorrelationId())
                    .setHeader(CONTENT_TYPE, OCTET_STREAM)
                    .setBody(result.toBytes(format));
            EventEmitter.getInstance().send(response);
        } catch (IllegalArgumentException e) {
            log.error("Unable to send response {} -> {} - {}", EVENT_API_SERVICE, input.getReplyTo(), e.getMessage());
        }
    }

    private void sendError(EventEnvelope input, String spanId, EventEnvelope.Format format, int status, String error) {
        try {
            EventEnvelope result = new EventEnvelope().setStatus(status).setBody(error);
            EventEnvelope response = new EventEnvelope().setTo(input.getReplyTo())
                    .setFrom(EVENT_API_SERVICE)
                    .setTrace(input.getTraceId(), input.getTracePath())
                    .setSpanId(spanId)
                    .setCorrelationId(input.getCorrelationId())
                    .setHeader(CONTENT_TYPE, OCTET_STREAM)
                    .setStatus(status)
                    .setBody(result.toBytes(format));
            EventEmitter.getInstance().send(response);
        } catch (IllegalArgumentException e) {
            log.error("Unable to send error {} -> {} - {}", EVENT_API_SERVICE, input.getReplyTo(), e.getMessage());
        }
    }
}
