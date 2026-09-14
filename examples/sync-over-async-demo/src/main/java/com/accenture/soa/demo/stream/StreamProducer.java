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

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.redis.RedisConfig;
import org.platformlambda.sync.ReturnRouteStore;
import org.platformlambda.sync.StreamResponder;
import org.platformlambda.sync.StreamSegment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The stream-producer pod's backend service ({@code POST /api/produce}): posts segments into a streaming
 * rendezvous opened by the stream-ui pod, purely through Redis ({@link StreamResponder} - no coordinator,
 * no broker; design D3). The request body selects a scenario:
 *
 * <pre>
 * {"cid": "...", "mode": "chat"}                              five ordered tokens + eof (use case 2)
 * {"cid": "...", "mode": "notify", "name": "orders", "body": "order 42 shipped"}
 * {"cid": "...", "mode": "close"}                             terminal eof (any producer may close)
 * {"cid": "...", "mode": "stall"}                             two tokens, NO terminal (producer-death chaos)
 * {"cid": "...", "mode": "lost", "type": "data|eof", ...}     CHAOS: store the segment but suppress the
 *                                                             wake-up - simulates a lost notification
 * {"cid": "...", "mode": "llm", "prompt": "..."}              REAL LLM tokens (experiment E4): pull the
 *                                                             provider's SSE stream through the shipped
 *                                                             SSE consumer; LlmStreamBridge forwards each
 *                                                             token batch into the rendezvous
 * </pre>
 *
 * The response reports {@code live}: {@code false} means the rendezvous is over (the UI pod closed,
 * timed out, or died - the orphan contract), which is the producer's signal to stop. The {@code lost}
 * mode is chaos tooling only: it appends via {@link ReturnRouteStore} without publishing, to demonstrate
 * the recovery paths (the next real post's drain, or the UI pod's final drain at idle expiry).
 */
@PreLoad(route = "demo.stream.producer", instances = 50)
public class StreamProducer implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    public static final List<String> TOKENS = List.of("Streaming", " across", " pods", " by", " design");
    public static final String EOT_METADATA = "{\"tokens\":5}";
    private static final long CHAOS_TTL_SECONDS = 120;
    private static final String LIVE = "live";
    private static final String POSTED = "posted";

    // shared per pod, lazily built on first use: @PreLoad construction runs before the
    // @MainApplication bootstrap, so nothing configuration-derived is frozen in the constructor.
    // Plain fields guarded by the class monitor: every write happens inside a synchronized ensure
    // method, and every reader calls that method first on its own thread before touching the field.
    private static StreamResponder responder;
    private static RedisClient chaosClient;
    private static StatefulRedisConnection<String, String> chaosConnection;

    /**
     * Post one segment through the pod's shared responder. The forwarding path of
     * {@link LlmStreamBridge}, which relays provider tokens into the same rendezvous.
     *
     * @return {@code true} while the rendezvous is live; {@code false} for an orphan (stop producing)
     */
    static boolean postSegment(String cid, String type, String name, String body) {
        ensureResponder();
        return responder.post(cid, type, name, body);
    }

    private static synchronized void ensureResponder() {
        if (responder == null) {
            responder = new StreamResponder(RedisConfig.from(AppConfigReader.getInstance()));
        }
    }

    /** Chaos-mode plumbing: a store handle of our own, so a segment can be appended with NO wake-up. */
    private static void appendWithoutWakeUp(String cid, StreamSegment segment) {
        ensureChaosConnection();
        new ReturnRouteStore(chaosConnection).appendSegment(cid, segment.toJson(), CHAOS_TTL_SECONDS);
    }

    private static synchronized void ensureChaosConnection() {
        if (chaosConnection == null) {
            chaosClient = RedisClient.create(RedisConfig.from(AppConfigReader.getInstance()).toUri());
            chaosConnection = chaosClient.connect();
        }
    }

    /** Test-lifecycle hook: release the shared Redis clients at class teardown. */
    public static synchronized void closeSharedClients() {
        if (responder != null) {
            responder.close();
            responder = null;
        }
        if (chaosConnection != null) {
            chaosConnection.close();
            chaosConnection = null;
        }
        if (chaosClient != null) {
            chaosClient.shutdown();
            chaosClient = null;
        }
    }

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input,
                                           int instance) throws AppException {
        // addressed directly by rest.yaml (no flow in between), so the input is the whole
        // AsyncHttpRequest map - the JSON request body is inside it
        Object body = new AsyncHttpRequest(input).getBody();
        if (!(body instanceof Map<?, ?> request)) {
            throw new AppException(400, "Request body must be a JSON object");
        }
        String cid = asText(request.get("cid"));
        String mode = asText(request.get("mode"));
        if (cid == null || mode == null) {
            throw new AppException(400, "Request body must carry 'cid' and 'mode'");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", mode);
        result.put("cid", cid);
        switch (mode) {
            case "chat" -> produceChatTokens(cid, result);
            case "notify" -> produceNotification(cid, request, result);
            case "close" -> result.put(LIVE, postSegment(cid, StreamSegment.EOF, null, null));
            case "stall" -> produceStalledStream(cid, result);
            case "llm" -> startLlmStream(cid, request, result);
            case "lost" -> storeWithoutWakeUp(cid, request, result);
            default -> throw new AppException(400, "Unknown mode: " + mode);
        }
        return result;
    }

    /** Mode {@code chat}: a single sequential producer over one connection - list order == posting order (D7). */
    private static void produceChatTokens(String cid, Map<String, Object> result) {
        boolean live = true;
        int posted = 0;
        for (String token : TOKENS) {
            live = postSegment(cid, StreamSegment.DATA, null, token);
            if (!live) {
                break;   // orphan - the rendezvous is over, stop producing
            }
            posted++;
        }
        if (live) {
            live = postSegment(cid, StreamSegment.EOF, null, EOT_METADATA);
            posted++;
        }
        result.put(LIVE, live);
        result.put(POSTED, posted);
    }

    /** Mode {@code notify}: one named event, as an uncoordinated backend service would emit. */
    private static void produceNotification(String cid, Map<?, ?> request, Map<String, Object> result) {
        boolean live = postSegment(cid, StreamSegment.DATA,
                asText(request.get("name")), asText(request.get("body")));
        result.put(LIVE, live);
        result.put(POSTED, 1);
    }

    /**
     * Mode {@code stall}: the producer "dies" after two tokens - no terminal ever arrives, so the UI
     * pod's idle expiry must fail the render in-band (the kill-the-producer chaos check).
     */
    private static void produceStalledStream(String cid, Map<String, Object> result) {
        boolean live = postSegment(cid, StreamSegment.DATA, null, "first")
                && postSegment(cid, StreamSegment.DATA, null, "second");
        result.put(LIVE, live);
        result.put(POSTED, 2);
    }

    /**
     * Mode {@code llm} (experiment E4): a real LLM token stream, cross-pod. The provider's SSE endpoint
     * is consumed by the platform's own SSE-capable HTTP client ({@code Accept: text/event-stream} plus
     * a reply route), and each relayed x-event-stream envelope reaches {@link LlmStreamBridge} with this
     * cid as its correlation id. The bridge posts the tokens into the rendezvous as they arrive.
     */
    private static void startLlmStream(String cid, Map<?, ?> request, Map<String, Object> result)
            throws AppException {
        AppConfigReader config = AppConfigReader.getInstance();
        String apiKey = config.getProperty("llm.api.key", "");
        if (apiKey.isBlank()) {
            throw new AppException(503, "llm.api.key is not configured (set GEMINI_API_KEY)");
        }
        String model = config.getProperty("llm.gemini.model", "gemini-flash-latest");
        String host = config.getProperty("llm.gemini.host", "https://generativelanguage.googleapis.com");
        String prompt = asText(request.get("prompt"));
        if (prompt == null || prompt.isBlank()) {
            prompt = "In one short sentence, why do event-driven systems scale well?";
        }
        AsyncHttpRequest upstream = new AsyncHttpRequest()
                .setMethod("POST").setTargetHost(host)
                .setUrl("/v1beta/models/" + model + ":streamGenerateContent")
                .setQueryParameter("alt", "sse")
                .setHeader("accept", "text/event-stream")
                .setHeader("content-type", "application/json")
                .setHeader("x-goog-api-key", apiKey)   // from the environment; never logged
                .setTimeoutSeconds(30)
                .setBody(Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of(
                                // a short direct answer: no thinking budget (flash aliases resolve
                                // to thinking models that would otherwise spend the whole budget
                                // on reasoning), bounded output for quota etiquette
                                "maxOutputTokens", 200,
                                "thinkingConfig", Map.of("thinkingBudget", 0))));
        EventEmitter.getInstance().send(new EventEnvelope()
                .setTo("async.http.request").setBody(upstream.toMap())
                .setReplyTo(LlmStreamBridge.ROUTE).setCorrelationId(cid));
        result.put("streaming", true);
        result.put("model", model);
    }

    /**
     * Mode {@code lost} - CHAOS ONLY: store-first happens, the wake-up never does (a lost Pub/Sub
     * notification). Recovery is the next real post's drain, or the UI pod's final drain at idle expiry.
     */
    private static void storeWithoutWakeUp(String cid, Map<?, ?> request, Map<String, Object> result) {
        String type = request.get("type") == null ? StreamSegment.DATA : asText(request.get("type"));
        StreamSegment segment = StreamSegment.of(type, asText(request.get("name")),
                asText(request.get("body")));
        appendWithoutWakeUp(cid, segment);
        result.put("stored", true);
        result.put("wakeUpSuppressed", true);
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
