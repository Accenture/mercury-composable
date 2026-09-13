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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Token-free pin of the LLM bridge's provider-frame parsing. The fixtures live in
 * {@link MockGeminiEndpoint} and mirror the {@code streamGenerateContent?alt=sse} frame shape, so
 * these pins and the emulated round-trip ({@link StreamingLlmEmulatedTest}) speak the same wire
 * dialect; the live-provider leg is the cross-pod dry-run's job (test report, scenario 6).
 */
class LlmFrameParsingTest {

    private static final String MID_STREAM_FRAME = MockGeminiEndpoint.MID_STREAM_FRAME;
    private static final String FINAL_FRAME = MockGeminiEndpoint.FINAL_FRAME;

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
        assertNotNull(usage, "the final frame carries usage metadata");
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
