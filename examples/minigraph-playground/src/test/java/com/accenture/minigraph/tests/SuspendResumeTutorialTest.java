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
 * End-to-end drive of the tutorial-14 approval workflow against a real (embedded) Redis:
 * the first request suspends at the human checkpoint and the second request, carrying the
 * same X-Correlation-Id, resumes and completes with the state captured in the first run.
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
    void approvalWorkflowSuspendsAndResumes() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        // step 1: submit the request - the workflow captures it and suspends for approval
        var submitted = runGraph(cid, Map.of("item", "laptop", "amount", 2000));
        assertEquals(200, submitted.getStatus());
        var first = new MultiLevelMap((Map<String, Object>) submitted.getBody());
        assertEquals("suspended", first.getElement("type"));
        assertEquals(cid, first.getElement("cid"));
        // step 2: the approver decides - same correlation ID resumes the workflow
        var approved = runGraph(cid, Map.of("decision", "approved"));
        assertEquals(200, approved.getStatus());
        var completed = new MultiLevelMap((Map<String, Object>) approved.getBody());
        assertEquals("completed", completed.getElement("stage"));
        assertEquals("approved", completed.getElement("decision"));
        // the original request from run 1 crossed the suspension into run 2
        assertEquals("laptop", completed.getElement("request.item"));
        assertEquals(2000, completed.getElement("request.amount"));
        log.info("tutorial-14 approval workflow completed for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void freshCorrelationIdRunsFromTheStart() throws TimeoutException {
        // a transaction that never suspended simply flows through the resume node
        var response = runGraph(Utility.getInstance().getUuid(), Map.of("item", "mouse"));
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("suspended", body.getElement("type"));
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
