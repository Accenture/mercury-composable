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

import com.accenture.minigraph.start.MainApp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end drive of the tutorial-14 purchase workflow against a real (embedded) Redis:
 * three human checkpoints (order, approval, delivery release) expressed as four short
 * graph runs sharing one X-Correlation-Id - each run resumes past the previous
 * checkpoint and the final response carries the state captured across all of them.
 */
class SuspendResumeTutorialTest {
    private static final Logger log = LoggerFactory.getLogger(SuspendResumeTutorialTest.class);
    private static final int REDIS_PORT = 16381;
    private static final long TIMEOUT = 8000;
    private static RedisServer redisServer;
    private static String target;

    @BeforeAll
    static void beforeAll() throws Exception {
        // the state-store connection is lazy and reads configuration at first use,
        // so a system property set before the first suspension is honored even when
        // another test class boots the application first
        System.setProperty("redis.port", String.valueOf(REDIS_PORT));
        var dir = new File("/tmp/tutorial-14-redis");
        Utility.getInstance().cleanupDir(dir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Unable to create " + dir);
        }
        redisServer = RedisServer.newRedisServer()
                .port(REDIS_PORT)
                .setting("dir " + dir.getAbsolutePath())
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        redisServer.start();
        MainApp.main(new String[0]);
        var config = AppConfigReader.getInstance();
        target = "http://127.0.0.1:" + config.getProperty("rest.server.port");
    }

    @SuppressWarnings("unchecked")
    @Test
    void purchaseWorkflowWithThreeCheckpoints() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        // run 1: the customer orders a laptop - suspend for the store manager
        var ordered = stage(runGraph(cid, Map.of("item", "laptop", "amount", 2000)), cid);
        assertTrue(String.valueOf(ordered.getElement("stage")).startsWith("order-submitted"));
        // run 2: the store manager approves - suspend for the delivery department
        var approved = stage(runGraph(cid, Map.of("decision", "approved", "manager", "store-88")), cid);
        assertTrue(String.valueOf(approved.getElement("stage")).startsWith("approved"));
        // run 3: the delivery department releases the shipment - suspend for confirmation
        var released = stage(runGraph(cid, Map.of("release", true, "courier", "express")), cid);
        assertTrue(String.valueOf(released.getElement("stage")).startsWith("released"));
        // run 4: shipment confirmation - the workflow completes with the full history
        var shipped = stage(runGraph(cid, Map.of("tracking", "TRK-12345")), cid);
        assertEquals("shipped", shipped.getElement("stage"));
        // state captured across all four runs survived every suspension
        assertEquals("laptop", shipped.getElement("order.item"));
        assertEquals(2000, shipped.getElement("order.amount"));
        assertEquals("approved", shipped.getElement("approval.decision"));
        assertEquals("store-88", shipped.getElement("approval.manager"));
        assertEquals(true, shipped.getElement("delivery.release"));
        assertEquals("express", shipped.getElement("delivery.courier"));
        assertEquals("TRK-12345", shipped.getElement("shipment.tracking"));
        log.info("tutorial-14 purchase workflow shipped for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    private MultiLevelMap stage(EventEnvelope response, String cid) {
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals(cid, body.getElement("cid"));
        return body;
    }

    @SuppressWarnings("unchecked")
    @Test
    void freshCorrelationIdRunsFromTheStart() throws TimeoutException {
        // a transaction that never suspended simply flows through the resume node
        var response = runGraph(Utility.getInstance().getUuid(), Map.of("item", "mouse"));
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertTrue(String.valueOf(body.getElement("stage")).startsWith("order-submitted"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void decisionWithoutSubmissionIsRejected() throws TimeoutException {
        // input validation: an approval decision for a transaction that was never
        // submitted (or has expired) must be rejected - the submission comes first
        var response = runGraph(Utility.getInstance().getUuid(), Map.of("decision", "approved"));
        assertEquals(404, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("rejected", body.getElement("type"));
        assertTrue(String.valueOf(body.getElement("message")).contains("Submit the order"),
                "unexpected rejection message: " + response.getBody());
    }

    private EventEnvelope runGraph(String cid, Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/tutorial-14")
                .setBody(body)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .setHeader("X-Correlation-Id", cid);
        var event = new EventEnvelope().setTo("async.http.request").setBody(request);
        var po = PostOffice.trackable("unit.test",
                String.format("%032x", Math.abs(cid.hashCode())), "TEST /graph/tutorial-14");
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (response.hasError()) {
            log.warn("HTTP-{} - {}", response.getStatus(), response.getBody());
        }
        return response;
    }
}
