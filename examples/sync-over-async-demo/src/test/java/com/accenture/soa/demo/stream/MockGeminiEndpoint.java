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

package com.accenture.soa.demo.stream;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventStreamWriter;

import java.util.Map;

/**
 * The emulated LLM provider: a {@code stream: true} endpoint in this very application that answers a
 * {@code streamGenerateContent}-shaped request with Gemini-shaped SSE frames (unnamed {@code data:}
 * events, exactly like the real wire) through the platform's own streaming producer API. Pointing
 * {@code llm.gemini.host} at this application lets CI run the complete
 * {@code produce(llm) → SSE consumer → LlmStreamBridge → rendezvous → SSE edge} circle with no
 * network and no API key. The frame fixtures are shared with {@link LlmFrameParsingTest}, so the
 * parsing pins and the round-trip test speak the same wire dialect.
 */
@PreLoad(route = "mock.gemini.stream", instances = 10)
@EventInterceptor
public class MockGeminiEndpoint implements TypedLambdaFunction<EventEnvelope, Void> {

    /** A mid-stream provider frame: token text, running usage, no finish reason yet. */
    public static final String MID_STREAM_FRAME = """
            {"candidates":[{"content":{"parts":[{"text":"Event-driven systems scale"}],"role":"model"}}],
             "usageMetadata":{"promptTokenCount":12,"totalTokenCount":12},
             "modelVersion":"gemini-2.5-flash"}""";

    /** The final provider frame: last token batch, finish reason and complete usage metadata. */
    public static final String FINAL_FRAME = """
            {"candidates":[{"content":{"parts":[{"text":" because components decouple."}],"role":"model"},
              "finishReason":"STOP"}],
             "usageMetadata":{"promptTokenCount":12,"candidatesTokenCount":19,"totalTokenCount":31},
             "modelVersion":"gemini-2.5-flash"}""";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope request, int instance) {
        var out = new EventStreamWriter(request);
        out.first(200, "text/event-stream");
        out.write(MID_STREAM_FRAME.replace("\n", ""));   // one compact frame per SSE data event
        out.write(FINAL_FRAME.replace("\n", ""));
        out.close();
        return null;
    }
}
