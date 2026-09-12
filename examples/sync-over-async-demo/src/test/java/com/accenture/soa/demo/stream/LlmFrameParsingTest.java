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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Token-free pin of the LLM bridge's provider-frame parsing (the live Gemini leg is exercised by
 * the E4 dry-run, not by CI - same principle as the agent-orchestration E0's mock-by-config CI).
 * Fixtures mirror the {@code streamGenerateContent?alt=sse} frame shape.
 */
class LlmFrameParsingTest {

    private static final String MID_STREAM_FRAME = """
            {"candidates":[{"content":{"parts":[{"text":"Event-driven systems scale"}],"role":"model"}}],
             "usageMetadata":{"promptTokenCount":12,"totalTokenCount":12},
             "modelVersion":"gemini-2.5-flash"}""";

    private static final String FINAL_FRAME = """
            {"candidates":[{"content":{"parts":[{"text":" because components decouple."}],"role":"model"},
              "finishReason":"STOP"}],
             "usageMetadata":{"promptTokenCount":12,"candidatesTokenCount":19,"totalTokenCount":31},
             "modelVersion":"gemini-2.5-flash"}""";

    @Test
    void extractsTokenTextFromAFrame() {
        assertEquals("Event-driven systems scale", LlmStreamBridge.extractText(MID_STREAM_FRAME));
        assertEquals(" because components decouple.", LlmStreamBridge.extractText(FINAL_FRAME));
    }

    @Test
    void frameWithoutTextYieldsNull() {
        assertNull(LlmStreamBridge.extractText("{\"candidates\":[{\"finishReason\":\"STOP\"}]}"));
        assertNull(LlmStreamBridge.extractText("not json"));
    }

    @Test
    void usageMetadataBecomesCompactEofMetadata() {
        String usage = LlmStreamBridge.extractUsage(FINAL_FRAME);
        assertTrue(usage.contains("\"provider\":\"gemini\""), usage);
        assertTrue(usage.contains("\"model\":\"gemini-2.5-flash\""), usage);
        assertTrue(usage.contains("\"finishReason\":\"STOP\""), usage);
        assertTrue(usage.contains("\"totalTokenCount\":31"), usage);
    }

    @Test
    void frameWithoutUsageYieldsNull() {
        assertNull(LlmStreamBridge.extractUsage("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"x\"}]}}]}"));
        assertNull(LlmStreamBridge.extractUsage("not json"));
    }
}
