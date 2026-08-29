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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.util.Utility;

import java.util.Map;

/**
 * Test producer for HTTP response streaming - the callee side of the multi-shot
 * reply route. Modes are selected by the "mode" query parameter.
 */
@EventInterceptor
public class MockEventStreamService implements LambdaFunction {
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final String LINE_FEED = "\n";
    private static final String PADDING = "x".repeat(8192);
    private static final long PACE = 250;

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        var util = Utility.getInstance();
        EventEnvelope request = (EventEnvelope) input;
        AsyncHttpRequest http = new AsyncHttpRequest(request.getRawBody());
        String mode = http.getQueryParameter("mode");
        var out = new EventStreamWriter(request);
        switch (mode == null? "sse" : mode) {
            case "sse" -> {
                out.first(200, TEXT_EVENT_STREAM);
                out.write("Hello");
                util.sleep(PACE);
                out.write("token stream");
                util.sleep(PACE);
                out.close(Map.of("segments", 2));
            }
            case "sse-named" -> {
                out.first(200, TEXT_EVENT_STREAM);
                out.write("tokens", Map.of("n", 1));
                out.write("tokens", Map.of("n", 2));
                out.close();
            }
            case "sse-multiline" -> {
                out.first(200, TEXT_EVENT_STREAM);
                out.write("line1\nline2");
                out.close();
            }
            case "ndjson" -> {
                // no first() - content type falls back to Accept negotiation
                for (int i = 1; i <= 3; i++) {
                    out.write(Map.of("seq", i));
                }
                out.close(Map.of("ignored", true));
            }
            case "chunk" -> {
                out.first(200, "text/plain");
                out.write("alpha");
                out.write("beta");
                out.close();
            }
            case "error" -> {
                out.first(200, TEXT_EVENT_STREAM);
                out.write("partial");
                out.fail(new AppException(503, "backend on fire"));
            }
            case "error-first" -> out.fail(new AppException(503, "no backend"));
            case "stall" -> {
                // one-second idle allowance, then silence - the housekeeper must fail it in-band
                out.first(200, TEXT_EVENT_STREAM, 1);
                out.write("one");
            }
            case "ping" -> {
                // the keep-alive timer starts when the first segment commits the head,
                // so the quiet period must come after the first write
                out.first(200, TEXT_EVENT_STREAM);
                out.write("early");
                util.sleep(2500);
                out.write("late");
                out.close();
            }
            case "empty-close" -> out.close(Map.of("done", true));
            case "slow-paced" -> {
                // total duration exceeds the 1s idle allowance, but every gap is within it:
                // each arriving segment must extend the stream's life (touch semantics)
                out.first(200, TEXT_EVENT_STREAM, 1);
                for (int i = 1; i <= 3; i++) {
                    out.write("segment-" + i);
                    util.sleep(700);
                }
                out.close();
            }
            case "burst" -> {
                // 50 unpaced 8KB segments - strict FIFO is guaranteed by the request's
                // ordered reply lane, and the payload size forces the vert.x write queue
                // to fill so the drain-handler path is exercised deterministically
                out.first(200, "text/plain");
                for (int i = 1; i <= 50; i++) {
                    out.write(i + "|" + PADDING + LINE_FEED);
                }
                out.close();
            }
            case "headers" -> {
                // raw first event carrying custom headers - the endpoint's response
                // header transform must add/drop exactly as for a single-shot response
                EventEmitter.getInstance().send(new EventEnvelope()
                        .setTo(request.getReplyTo()).setCorrelationId(request.getCorrelationId())
                        .setHeader(EventStreamWriter.X_EVENT_STREAM, EventStreamWriter.DATA)
                        .setHeader("content-type", TEXT_EVENT_STREAM)
                        .setHeader("x-secret-header", "hide-me")
                        .setHeader("x-custom-note", "visible")
                        .setStatus(200).setBody("transformed"));
                out.close();
            }
            case "conflict" -> {
                // a stray x-stream-id alongside x-event-stream: the stream marker wins (warn log)
                EventEmitter.getInstance().send(new EventEnvelope()
                        .setTo(request.getReplyTo()).setCorrelationId(request.getCorrelationId())
                        .setHeader(EventStreamWriter.X_EVENT_STREAM, EventStreamWriter.DATA)
                        .setHeader("x-stream-id", "stream.fake.in")
                        .setHeader("content-type", TEXT_EVENT_STREAM)
                        .setStatus(200).setBody("resolved"));
                out.close();
            }
            default -> out.fail(new AppException(400, "unknown mode " + mode));
        }
        return null;
    }
}
