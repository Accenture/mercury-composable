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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.sync.SyncRuntime;
import redis.embedded.RedisServer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The emulated-LLM round-trip in CI (follow-up to streaming-return-route experiment E4): one JVM
 * plays every role, and the complete circle runs with <b>no network and no API key</b> -
 *
 * <pre>
 *   collector ◄─SSE── /api/notifications (StreamBridge facade + rendezvous, embedded Redis)
 *      │                                       ▲
 *      └─► POST /api/produce {mode: "llm"} ──► async.http.request ──SSE──► /v1beta/models/{...}
 *                                              (the shipped SSE consumer)   (MockGeminiEndpoint)
 *                                                     │
 *                                                LlmStreamBridge ──RPUSH+publish──► rendezvous
 * </pre>
 *
 * The stub serves the same Gemini-shaped frames {@link LlmFrameParsingTest} pins, and the
 * producer's real URL contract ({@code .../models/<model>:streamGenerateContent?alt=sse}) is sent
 * verbatim through the platform HTTP client (the RFC 3986 colon pin itself lives in platform-core's
 * UtilityTests). The live-provider leg remains the cross-pod dry-run's job (test report, scenario 6).
 */
class StreamingLlmEmulatedTest {

    private static final int REST_PORT = 8611;    // matches rest.server.port in test application.properties
    private static final int REDIS_PORT = 16479;  // matches redis.port in test application.properties
    private static final String REDIS_DATA_DIR = "/tmp/soa-demo-redis";
    private static final String X_EVENT_STREAM = "x-event-stream";
    private static final String X_EVENT_NAME = "x-event-name";

    // Not a credential: mode "llm" requires a non-blank key before it proceeds, and the emulated
    // provider never reads it. Injected as a system property (resolved before the properties file)
    // so no config file carries a credential-shaped literal for secret scanners to flag.
    private static final String DUMMY_KEY_FOR_EMULATION = "emulated";

    private static RedisServer redisServer;

    // S5443: the fixed transient /tmp working dir mirrors the redis-standalone helper (wiped per run)
    @SuppressWarnings("java:S5443")
    @BeforeAll
    static void boot() throws Exception {
        System.setProperty("llm.api.key", DUMMY_KEY_FOR_EMULATION);
        File dir = new File(REDIS_DATA_DIR);
        Utility.getInstance().cleanupDir(dir);
        if (!dir.mkdirs()) {
            throw new IllegalStateException("Unable to create " + REDIS_DATA_DIR);
        }
        redisServer = RedisServer.newRedisServer()
                .port(REDIS_PORT)
                .setting("dir " + REDIS_DATA_DIR)
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        redisServer.start();
        AutoStart.main(new String[0]);
        BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(AsyncHttpClient.ASYNC_HTTP_RESPONSE, 20).onSuccess(ready::add);
        if (!Boolean.TRUE.equals(ready.poll(20, TimeUnit.SECONDS))) {
            throw new IllegalStateException("REST automation HTTP server did not become ready");
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        StreamProducer.closeSharedClients();
        SyncRuntime.shutdown();
        if (redisServer != null) {
            redisServer.stop();
        }
        System.clearProperty("llm.api.key");
    }

    private record Frame(Map<String, String> headers, Object body) {
        String marker() {
            return headers.get(X_EVENT_STREAM);
        }
        String name() {
            return headers.get(X_EVENT_NAME);
        }
    }

    private static final class SseCollector implements LambdaFunction {
        final List<Frame> frames = new CopyOnWriteArrayList<>();
        final CountDownLatch ended = new CountDownLatch(1);

        @Override
        public Object handleEvent(Map<String, String> headers, Object input, int instance) {
            frames.add(new Frame(new HashMap<>(headers), input));
            String marker = headers.get(X_EVENT_STREAM);
            if ("eof".equals(marker) || "exception".equals(marker)) {
                ended.countDown();
            }
            return null;
        }

        List<Object> dataBodies() {
            return frames.stream()
                    .filter(f -> "data".equals(f.marker()) && f.name() == null)
                    .map(Frame::body).toList();
        }

        List<Frame> named(String eventName) {
            return frames.stream()
                    .filter(f -> "data".equals(f.marker()) && eventName.equals(f.name()))
                    .toList();
        }
    }

    private static String awaitAnnouncedCid(SseCollector collector) {
        for (int i = 0; i < 500; i++) {
            List<Frame> announce = collector.named(StreamNotifyFacade.CID_EVENT);
            if (!announce.isEmpty()) {
                return String.valueOf(announce.getFirst().body());
            }
            Utility.getInstance().sleep(10);
        }
        fail("the notification facade did not announce the session cid in time");
        return null;   // unreachable
    }

    @Test
    @SuppressWarnings("unchecked")
    void emulatedLlmTokensCompleteTheFullCircle() throws Exception {
        // the UI side: open the notification channel and learn the session cid
        SseCollector collector = new SseCollector();
        Platform.getInstance().registerPrivate("llm.collector", collector, 1);
        AsyncHttpRequest channel = new AsyncHttpRequest()
                .setMethod("GET").setTargetHost("http://127.0.0.1:" + REST_PORT).setUrl("/api/notifications")
                .setHeader("accept", "text/event-stream")
                .setTimeoutSeconds(20);
        EventEmitter.getInstance().send(new EventEnvelope()
                .setTo("async.http.request").setBody(channel.toMap())
                .setReplyTo("llm.collector").setCorrelationId(Utility.getInstance().getUuid()));
        String cid = awaitAnnouncedCid(collector);
        assertNotNull(cid);

        // the request leg: trigger mode "llm" - the producer calls the emulated provider in this JVM
        AsyncHttpRequest produce = new AsyncHttpRequest()
                .setMethod("POST").setTargetHost("http://127.0.0.1:" + REST_PORT).setUrl("/api/produce")
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json")
                .setBody(Map.of("cid", cid, "mode", "llm", "prompt", "why do event-driven systems scale?"))
                .setTimeoutSeconds(15);
        PostOffice po = PostOffice.trackable("unit.test", Utility.getInstance().getUuid(), "POST /api/produce");
        EventEnvelope accepted = po.request(new EventEnvelope()
                .setTo("async.http.request").setBody(produce.toMap()), 15000).get();
        assertEquals(200, accepted.getStatus());
        Map<String, Object> ack = (Map<String, Object>) accepted.getBody();
        assertEquals(true, ack.get("streaming"));
        assertEquals("emulated-model", ack.get("model"));

        // the render: the stub's token batches arrive in generation order, then done with usage, then eof
        assertTrue(collector.ended.await(15, TimeUnit.SECONDS), "the emulated stream should complete");
        assertEquals(List.of("Event-driven systems scale", " because components decouple."),
                collector.dataBodies(), "token order preserved through the whole circle");
        List<Frame> done = collector.named("done");
        assertEquals(1, done.size(), "the recovered usage metadata rides the terminal done event");
        String usage = String.valueOf(done.getFirst().body());
        assertTrue(usage.contains("\"provider\":\"gemini\""), usage);
        assertTrue(usage.contains("\"model\":\"gemini-2.5-flash\""), usage);
        assertTrue(usage.contains("\"finishReason\":\"STOP\""), usage);
        assertTrue(usage.contains("\"totalTokenCount\":31"), usage);
        assertEquals(0, SyncRuntime.activeStreams(), "rendezvous closed by its terminal");
    }
}
