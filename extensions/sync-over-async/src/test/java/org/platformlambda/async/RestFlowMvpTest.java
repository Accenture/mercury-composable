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

package org.platformlambda.async;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.kafka.KafkaRuntime;
import org.platformlambda.support.TestRuntimeConstants;
import org.platformlambda.sync.RedisTestBase;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end proof of the whole sync-over-async MVP, fully composable:
 *
 * <pre>
 *   HTTP POST /api/sync-to-async -> http.flow.adapter -> flow sync-to-async
 *     sync.prepare: begin(cid) [Redis] -> simple.kafka.notification -> Kafka topic-1 -> sync.await (blocks)
 *       -> Kafka Flow Adapter -> flow system-of-record (echo + notify topic-2)
 *         -> Kafka topic-2 -> Kafka Flow Adapter -> flow soa-reply -> coordinator.deliver
 *           -> Redis return route wakes sync.await -> HTTP 200 + body
 * </pre>
 *
 * Runs against embedded Redis ({@link RedisTestBase}) and an embedded KRaft Kafka broker. The Kafka
 * publisher and the per-topic flow-routing consumers are built by the minimalist-kafka library's
 * {@code KafkaFlowAutoStart} {@code @MainApplication} autoloader; the embedded broker address is
 * injected via a system property.
 * Because every hop runs as a flow task carrying the {@code traceparent}, the whole path is one trace.
 */
class RestFlowMvpTest extends RedisTestBase {

    private static final int REST_PORT = 8305;   // matches rest.server.port in test application.properties
    private static final String TRACE_PARENT = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";

    private static EmbeddedKafka kafka;

    @BeforeAll
    static void boot() throws Exception {
        kafka = new EmbeddedKafka();
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), TestRuntimeConstants.REQUEST_TOPIC);
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), TestRuntimeConstants.RESPONSE_TOPIC);
        // Point the autoloaders at the embedded infra: KAFKA_BOOTSTRAP_SERVERS feeds the
        // ${KAFKA_BOOTSTRAP_SERVERS:...} placeholder in the Kafka client templates; redis.port overrides the
        // file value (ConfigReader resolves a system property before the file). sync.over.async.enabled=true
        // is in the test application.properties.
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", kafka.bootstrapServers());
        System.setProperty("redis.port", String.valueOf(redisPort));

        // Platform start registers all functions, then the autoloaders run: SyncOverAsyncAutoStart builds the
        // Redis client + return-route coordinator from config; KafkaFlowAutoStart builds the Kafka publisher
        // and starts the flow adapter (topic-1 -> system-of-record, topic-2 -> soa-reply).
        AutoStart.main(new String[0]);
        BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(AsyncHttpClient.ASYNC_HTTP_RESPONSE, 20).onSuccess(ready::add);
        if (!Boolean.TRUE.equals(ready.poll(20, TimeUnit.SECONDS))) {
            throw new IllegalStateException("REST automation HTTP server did not become ready");
        }
    }

    @AfterAll
    static void cleanup() {
        if (KafkaRuntime.adapter() != null) {
            KafkaRuntime.adapter().close();
        }
        if (KafkaRuntime.publisher() != null) {
            KafkaRuntime.publisher().close();
        }
        org.platformlambda.sync.SyncRuntime.shutdown();
        if (kafka != null) {
            kafka.close();
        }
        System.clearProperty("KAFKA_BOOTSTRAP_SERVERS");
        System.clearProperty("redis.port");
    }

    @Test
    @SuppressWarnings("unchecked")
    void synchronousRoundTripReturnsTheAsyncResponse() throws Exception {
        EventEnvelope response = post(Map.of("action", "create"), null);

        assertEquals(200, response.getStatus(), "the facade should return the async response synchronously");
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body.get("cid"), "response carries the correlation-id");
        // The System-of-Record flow echoes the request it received over Kafka; getting it back proves the
        // full request -> Kafka -> SoR flow -> Kafka -> reply flow -> Redis return route -> await round-trip.
        assertEquals(Map.of("action", "create"), body.get("echo"), "the request round-tripped through Kafka");
        // span-ids change per hop; the trace-id (32-hex of TRACE_PARENT) must stay continuous end-to-end.
        assertEquals("0af7651916cd43dd8448eb211c80319c", body.get("traceId"),
                "trace-id stayed continuous from REST through Kafka to the System-of-Record");
    }

    @Test
    void timeoutReturns408WhenBackendDropsTheRequest() throws Exception {
        // The System-of-Record drops "no-reply" requests; with a short await budget the facade returns 408.
        EventEnvelope response = post(Map.of("action", "no-reply"), "1500");

        assertEquals(408, response.getStatus(), "an unanswered request should time out as HTTP 408");
    }

    private static EventEnvelope post(Map<String, Object> requestBody, String timeoutHeader) throws Exception {
        AsyncHttpRequest httpRequest = new AsyncHttpRequest();
        httpRequest.setTargetHost("http://127.0.0.1:" + REST_PORT).setUrl("/api/sync-to-async")
                .setMethod("POST").setBody(requestBody)
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json")
                .setHeader("traceparent", TRACE_PARENT);
        if (timeoutHeader != null) {
            httpRequest.setHeader("x-sync-timeout", timeoutHeader);
        }
        PostOffice po = PostOffice.trackable("unit.test", Utility.getInstance().getUuid(), "POST /api/sync-to-async");
        return po.request(new EventEnvelope().setTo("async.http.request").setBody(httpRequest.toMap()), 20000).get();
    }
}
