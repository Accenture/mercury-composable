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
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.sync.StreamSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The LLM-to-rendezvous bridge (streaming-return-route experiment E4): the reply route of an
 * {@code async.http.request} SSE consumption of a provider's token stream (Gemini
 * {@code streamGenerateContent?alt=sse}). Each relayed {@code x-event-stream} envelope arrives here
 * with the rendezvous cid as its correlation id, and the bridge forwards it into the streaming
 * return route via {@link StreamProducer#postSegment} - token text as {@code data} segments, the
 * clean upstream end as {@code eof} carrying the provider's usage metadata, failures as
 * {@code exception}. Whichever pod holds the user's SSE connection renders the tokens - the LLM
 * stream made horizontal.
 *
 * <p>This is also the wrapper-side shape a {@code graph.task} node drives (route decoupling): the
 * node streams progress out-of-band through the rendezvous while its own graph edge stays plain
 * request/response - no engine change.</p>
 */
// instances = 1 is the ordering contract, not an oversight: the producer side of the rendezvous
// must post in order (design D7), and a multi-instance reply consumer would process upstream
// frames concurrently - a token batch sequenced behind the terminal is silently discarded. One
// instance = one sequential poster, the same reason the HTTP edge's reply lanes are
// single-instance routes. (A high-fanout application would mint one temporary route per stream.)
@PreLoad(route = LlmStreamBridge.ROUTE)
@EventInterceptor
public class LlmStreamBridge implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Logger log = LoggerFactory.getLogger(LlmStreamBridge.class);

    public static final String ROUTE = "demo.llm.bridge";
    private static final String X_EVENT_STREAM = "x-event-stream";
    // the provider's usage metadata rides each chunk; the freshest one becomes the eof metadata
    private static final ConcurrentMap<String, String> lastUsage = new ConcurrentHashMap<>();

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) {
        String cid = event.getCorrelationId();
        if (cid == null) {
            return null;
        }
        String marker = headers.get(X_EVENT_STREAM);
        if (marker == null) {
            // not a stream envelope: the provider refused before streaming began (e.g. auth error) -
            // the SSE consumer answers single-shot, so fail the rendezvous in-band with the reason
            if (event.getStatus() >= 400) {
                lastUsage.remove(cid);
                StreamProducer.postSegment(cid, StreamSegment.EXCEPTION, null,
                        "LLM request failed with status " + event.getStatus());
                log.warn("LLM request for {} failed with status {}", cid, event.getStatus());
            }
            return null;
        }
        switch (marker) {
            case StreamSegment.DATA -> {
                String frame = event.getBody() == null ? "" : String.valueOf(event.getBody());
                String usage = extractUsage(frame);
                if (usage != null) {
                    lastUsage.put(cid, usage);
                }
                String text = extractText(frame);
                if (text != null && !text.isEmpty()
                        && !StreamProducer.postSegment(cid, StreamSegment.DATA, null, text)) {
                    // rendezvous over (UI closed/timed out) - drop the remainder; the upstream
                    // generation is already bounded by maxOutputTokens
                    log.info("Rendezvous {} is over - dropping remaining LLM tokens", cid);
                }
            }
            case StreamSegment.EOF ->
                    StreamProducer.postSegment(cid, StreamSegment.EOF, null, lastUsage.remove(cid));
            case StreamSegment.EXCEPTION -> {
                lastUsage.remove(cid);
                StreamProducer.postSegment(cid, StreamSegment.EXCEPTION, null,
                        event.getBody() == null ? "LLM stream failed" : String.valueOf(event.getBody()));
            }
            default -> log.warn("Unexpected stream marker {} for {}", marker, cid);
        }
        return null;
    }

    /** @return the token text of one provider SSE frame, or {@code null} when the frame carries none */
    @SuppressWarnings("unchecked")
    static String extractText(String frameJson) {
        try {
            Map<String, Object> frame = SimpleMapper.getInstance().getMapper().readValue(frameJson, Map.class);
            Object text = new MultiLevelMap(frame).getElement("candidates[0].content.parts[0].text");
            return text == null ? null : String.valueOf(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** @return compact usage metadata (model, finish reason, token counts) when the frame carries it */
    @SuppressWarnings("unchecked")
    static String extractUsage(String frameJson) {
        try {
            Map<String, Object> frame = SimpleMapper.getInstance().getMapper().readValue(frameJson, Map.class);
            MultiLevelMap map = new MultiLevelMap(frame);
            Object total = map.getElement("usageMetadata.totalTokenCount");
            if (total == null) {
                return null;
            }
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("provider", "gemini");
            Object model = map.getElement("modelVersion");
            if (model != null) {
                usage.put("model", model);
            }
            Object finishReason = map.getElement("candidates[0].finishReason");
            if (finishReason != null) {
                usage.put("finishReason", finishReason);
            }
            Object thoughts = map.getElement("usageMetadata.thoughtsTokenCount");
            if (thoughts != null) {
                usage.put("thoughtsTokenCount", thoughts);
            }
            usage.put("promptTokenCount", map.getElement("usageMetadata.promptTokenCount", 0));
            usage.put("candidatesTokenCount", map.getElement("usageMetadata.candidatesTokenCount", 0));
            usage.put("totalTokenCount", total);
            return SimpleMapper.getInstance().getCompactGson().toJson(usage);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
