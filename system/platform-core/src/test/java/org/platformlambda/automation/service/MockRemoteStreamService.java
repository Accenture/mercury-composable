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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.util.Utility;

import java.util.Map;

/**
 * Test producer for Event-over-HTTP peer streaming: a PUBLIC streaming target
 * invoked through "/api/event". Unlike the edge mock, it is addressed as a plain
 * function - modes are selected by the "mode" event header, not a query parameter.
 */
@EventInterceptor
public class MockRemoteStreamService implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final long PACE = 250;

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        var util = Utility.getInstance();
        String mode = headers.get("mode");
        var out = new EventStreamWriter(request);
        switch (mode == null? "tokens" : mode) {
            case "tokens" -> {
                out.first(200, TEXT_EVENT_STREAM);
                out.write("alpha");
                util.sleep(PACE);
                out.write("beta");
                util.sleep(PACE);
                out.close(Map.of("segments", 2));
            }
            case "typed" -> {
                // every escape-hatch trigger: a Map body, text with a carriage return,
                // a user event name colliding with the reserved "envelope" word, a
                // byte body - plus one plain text segment that rides a raw frame
                out.first(200, TEXT_EVENT_STREAM);
                out.write(Map.of("n", 1));
                out.write("crlf", "line1\r\nline2");
                out.write("envelope", "reserved-name");
                out.write(new byte[]{1, 2, 3, 4});
                out.write("plain token");
                out.close(Map.of("done", true));
            }
            case "error-mid" -> {
                out.first(200, TEXT_EVENT_STREAM);
                out.write("partial");
                out.fail(new AppException(503, "backend on fire"));
            }
            case "error-first" -> out.fail(new AppException(503, "no backend"));
            case "stall" -> {
                // one-second declared idle allowance, then silence - the server
                // housekeeper or the consuming client must fail the stream in-band
                out.first(200, TEXT_EVENT_STREAM, 1);
                out.write("one");
            }
            default -> out.fail(new AppException(400, "unknown mode " + mode));
        }
        return null;
    }
}
