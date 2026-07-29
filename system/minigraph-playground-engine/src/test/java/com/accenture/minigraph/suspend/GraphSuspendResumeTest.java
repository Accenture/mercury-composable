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

package com.accenture.minigraph.suspend;

import com.accenture.minigraph.mock.CountingStepTask;
import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests of the graph.suspend and graph.resume skills through the real HTTP
 * stack, using the temp-file state store (/tmp/suspend-resume) - the engine needs no
 * external store dependency to prove the whole suspend/resume loop.
 */
class GraphSuspendResumeTest {
    private static final Logger log = LoggerFactory.getLogger(GraphSuspendResumeTest.class);
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final String STORE_DIR = "/tmp/suspend-resume";
    private static final long TIMEOUT = 8000;
    private static String target;

    @BeforeAll
    static void beforeAll() {
        Utility.getInstance().cleanupDir(new File(STORE_DIR));
        PlaygroundLoader.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port");
        target = "http://localhost:" + port;
    }

    @SuppressWarnings("unchecked")
    @Test
    void suspendPersistsAndResumeContinuesWithoutReExecution() throws TimeoutException, IOException {
        var cid = Utility.getInstance().getUuid();
        // run 1: the workflow reaches the checkpoint and suspends
        var first = runGraph("unit-test-suspend-1", cid);
        assertEquals(200, first.getStatus());
        assertInstanceOf(Map.class, first.getBody());
        var suspended = new MultiLevelMap((Map<String, Object>) first.getBody());
        assertEquals("suspended", suspended.getElement("type"));
        assertEquals(cid, suspended.getElement("cid"));
        assertEquals(1, CountingStepTask.getCount("one", cid));
        assertEquals(0, CountingStepTask.getCount("two", cid));
        // the business correlation ID propagates through the walker's internal events
        // into every skill and task - not the engine's internal callback IDs
        assertEquals(cid, CountingStepTask.getBusinessCid("one", cid));
        // the persisted record has the documented envelope shape and no reserved model keys
        var record = readStoredRecord(cid);
        assertEquals("step-1", record.getElement("data.node"));
        assertEquals(cid, record.getElement("data.cid"));
        assertEquals(1, record.getElement("data.model.step1_count"));
        assertNull(record.getElement("data.model.cid"), "reserved model keys must not persist");
        assertNull(record.getElement("data.model.instance"), "reserved model keys must not persist");
        assertNull(record.getElement("data.model.flow"), "reserved model keys must not persist");
        assertNull(record.getElement("data.model.trace"), "reserved model keys must not persist");
        assertEquals(true, record.getElement("data.run.step-1"));
        // run 2 with the same correlation ID: resume continues past the checkpoint
        var second = runGraph("unit-test-suspend-1", cid);
        assertEquals(200, second.getStatus());
        var completed = new MultiLevelMap((Map<String, Object>) second.getBody());
        assertEquals("two", completed.getElement("step"));
        assertEquals(1, completed.getElement("prior"), "restored model.step1_count must reach step-2");
        assertEquals(1, CountingStepTask.getCount("one", cid), "the suspension point must not re-execute");
        assertEquals(1, CountingStepTask.getCount("two", cid));
        assertFalse(storedFile(cid).exists(), "the record must be consumed on resume");
        log.info("suspend -> resume continuation verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void workflowSuspendsAndResumesAtMultipleCheckpoints() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var r1 = runGraph("unit-test-suspend-2", cid);
        assertEquals("suspended", new MultiLevelMap((Map<String, Object>) r1.getBody()).getElement("type"));
        var r2 = runGraph("unit-test-suspend-2", cid);
        assertEquals("suspended", new MultiLevelMap((Map<String, Object>) r2.getBody()).getElement("type"));
        var r3 = runGraph("unit-test-suspend-2", cid);
        assertEquals(200, r3.getStatus());
        var completed = new MultiLevelMap((Map<String, Object>) r3.getBody());
        assertEquals("c", completed.getElement("step"));
        assertEquals(1, completed.getElement("prior"), "model.b_count must survive the second suspension");
        assertEquals(1, CountingStepTask.getCount("a", cid));
        assertEquals(1, CountingStepTask.getCount("b", cid));
        assertEquals(1, CountingStepTask.getCount("c", cid));
        log.info("multi-checkpoint workflow verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void joinBarrierStillSatisfiedAfterResume() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var r1 = runGraph("unit-test-suspend-3", cid);
        assertEquals(200, r1.getStatus());
        assertEquals("suspended", new MultiLevelMap((Map<String, Object>) r1.getBody()).getElement("type"));
        // without the restored bookkeeping, join2 would never see alpha and the run would time out
        var r2 = runGraph("unit-test-suspend-3", cid);
        assertEquals(200, r2.getStatus());
        var completed = new MultiLevelMap((Map<String, Object>) r2.getBody());
        assertEquals("final", completed.getElement("step"));
        assertEquals(1, completed.getElement("prior"));
        assertEquals(1, CountingStepTask.getCount("gamma", cid), "gamma must not re-execute after resume");
        log.info("join barrier across suspension verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingTargetHandlesFreshCorrelationId() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-suspend-4", cid);
        // the handler node staged 'int(404) -> output.status' - a graph can set the
        // response status declaratively
        assertEquals(404, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("no-record", body.getElement("reason"));
        assertEquals(404, body.getElement("status"));
        assertEquals(0, CountingStepTask.getCount("x", cid), "the normal path must not run on a missing jump");
        log.info("resume 'missing' jump verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void expiredRecordFallsBackToFreshRun() throws TimeoutException, InterruptedException {
        var cid = Utility.getInstance().getUuid();
        var r1 = runGraph("unit-test-suspend-5", cid);
        assertEquals("suspended", new MultiLevelMap((Map<String, Object>) r1.getBody()).getElement("type"));
        assertEquals(1, CountingStepTask.getCount("expiry", cid));
        Thread.sleep(1300);
        // the 1s record has expired: the resume falls back to a fresh run and suspends again
        var r2 = runGraph("unit-test-suspend-5", cid);
        assertEquals("suspended", new MultiLevelMap((Map<String, Object>) r2.getBody()).getElement("type"));
        assertEquals(2, CountingStepTask.getCount("expiry", cid), "an expired record means a fresh run");
        log.info("ttl expiry fallback verified for cid {}", cid);
    }

    @Test
    void suspendSkillRequiresTheReservedAlias() throws TimeoutException {
        var response = runGraph("unit-test-suspend-err1", Utility.getInstance().getUuid());
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("must be named 'suspend'"),
                "unexpected error response: " + response.getBody());
    }

    @Test
    void routingSkillCannotBeSuspensible() throws TimeoutException {
        var response = runGraph("unit-test-suspend-err2", Utility.getInstance().getUuid());
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("cannot use 'suspend=true'"),
                "unexpected error response: " + response.getBody());
    }

    @Test
    void ttlIsMandatoryWithNoDefault() throws TimeoutException {
        // only the workflow designer knows whether a checkpoint waits a minute or days -
        // a default expiry would silently discard someone's workflow
        var response = runGraph("unit-test-suspend-err4", Utility.getInstance().getUuid());
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("does not have a 'ttl'"),
                "unexpected error response: " + response.getBody());
    }

    @Test
    void suspensibleNodeRequiresSuspendNode() throws TimeoutException {
        var response = runGraph("unit-test-suspend-err3", Utility.getInstance().getUuid());
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("has no 'suspend' node"),
                "unexpected error response: " + response.getBody());
    }

    @Test
    void helpFilesFollowNamingConvention() {
        assertNotNull(getClass().getResourceAsStream("/help/help graph-suspend.md"),
                "help file for graph.suspend is missing");
        assertNotNull(getClass().getResourceAsStream("/help/help graph-resume.md"),
                "help file for graph.resume is missing");
    }

    private EventEnvelope runGraph(String graphId, String cid) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + graphId)
                .setBody(Map.of("start", true))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .setHeader("X-Correlation-Id", cid);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var po = PostOffice.trackable("unit.test", util32HexTraceId(graphId + cid), "TEST /graph/" + graphId);
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (response.hasError()) {
            log.warn("HTTP-{} - {}", response.getStatus(), response.getBody());
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private MultiLevelMap readStoredRecord(String cid) throws IOException {
        var wrapper = (Map<String, Object>) new MsgPack().unpack(Files.readAllBytes(storedFile(cid).toPath()));
        return new MultiLevelMap(wrapper);
    }

    private File storedFile(String cid) {
        return new File(STORE_DIR, cid);
    }

    private String util32HexTraceId(String seed) {
        return String.format("%032x", Math.abs(seed.hashCode()));
    }
}
