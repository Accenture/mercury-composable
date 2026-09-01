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

package com.accenture.minigraph.tests;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventStreamWriter;

import java.util.Map;

/**
 * Token-free stand-in for the streaming AI node (the E0 follow-up). In production the
 * llm.stream.relay endpoint reaches 'llm.stream' on a polyglot wrapper pulling a REAL
 * provider token stream; in this test scope the same route name resolves to this mock,
 * which streams three fixed token batches over the same multi-shot reply contract -
 * so the endpoint wiring, the lane checkout and the SSE rendering are all pinned
 * without tokens or credentials.
 */
@PreLoad(route = "llm.stream", instances = 10)
@EventInterceptor
public class MockLlmStream implements TypedLambdaFunction<EventEnvelope, Void> {

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        var out = new EventStreamWriter(request);
        out.first(200, "text/event-stream");
        out.write("Mercury ");
        out.write("streams ");
        out.write("tokens");
        out.close(Map.of("model", "mock-llm", "stop_reason", "end_turn",
                "usage", Map.of("input_tokens", 5, "output_tokens", 3)));
        return null;
    }
}
