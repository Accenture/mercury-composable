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
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.support.RedisConfig;
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

    // shared per pod, lazily built on first use: @PreLoad construction runs before the
    // @MainApplication bootstrap, so nothing configuration-derived is frozen in the constructor
    private static volatile StreamResponder responder;
    private static volatile StatefulRedisConnection<String, String> chaosConnection;

    private static StreamResponder responder() {
        if (responder == null) {
            synchronized (StreamProducer.class) {
                if (responder == null) {
                    responder = new StreamResponder(RedisConfig.from(AppConfigReader.getInstance()));
                }
            }
        }
        return responder;
    }

    /** Chaos-mode plumbing: a store handle of our own, so a segment can be appended with NO wake-up. */
    private static ReturnRouteStore chaosStore() {
        if (chaosConnection == null) {
            synchronized (StreamProducer.class) {
                if (chaosConnection == null) {
                    chaosConnection = RedisClient
                            .create(RedisConfig.from(AppConfigReader.getInstance()).toUri()).connect();
                }
            }
        }
        return new ReturnRouteStore(chaosConnection);
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
            case "chat" -> {
                // a single sequential producer over one connection: list order == posting order (D7)
                boolean live = true;
                int posted = 0;
                for (String token : TOKENS) {
                    live = responder().post(cid, StreamSegment.DATA, null, token);
                    if (!live) {
                        break;   // orphan - the rendezvous is over, stop producing
                    }
                    posted++;
                }
                if (live) {
                    live = responder().post(cid, StreamSegment.EOF, null, EOT_METADATA);
                    posted++;
                }
                result.put("live", live);
                result.put("posted", posted);
            }
            case "notify" -> {
                boolean live = responder().post(cid, StreamSegment.DATA,
                        asText(request.get("name")), asText(request.get("body")));
                result.put("live", live);
                result.put("posted", 1);
            }
            case "close" -> result.put("live", responder().post(cid, StreamSegment.EOF, null, null));
            case "stall" -> {
                // the producer "dies" after two tokens: no terminal ever arrives, so the UI pod's
                // idle expiry must fail the render in-band (the kill-the-producer chaos check)
                boolean live = responder().post(cid, StreamSegment.DATA, null, "first")
                        && responder().post(cid, StreamSegment.DATA, null, "second");
                result.put("live", live);
                result.put("posted", 2);
            }
            case "lost" -> {
                // CHAOS ONLY: store-first happens, the wake-up never does - a lost Pub/Sub notification.
                // Recovery is the next real post's drain, or the UI pod's final drain at idle expiry.
                String type = request.get("type") == null ? StreamSegment.DATA : asText(request.get("type"));
                StreamSegment segment = StreamSegment.of(type, asText(request.get("name")),
                        asText(request.get("body")));
                chaosStore().appendSegment(cid, segment.toJson(), CHAOS_TTL_SECONDS);
                result.put("stored", true);
                result.put("wakeUpSuppressed", true);
            }
            default -> throw new AppException(400, "Unknown mode: " + mode);
        }
        return result;
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
