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
import com.accenture.minigraph.skills.GraphSuspend;
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
import java.util.List;
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
        var stored = readStoredRecord(cid);
        assertEquals("step-1", stored.getElement("data.node"));
        assertEquals(cid, stored.getElement("data.cid"));
        assertEquals(1, stored.getElement("data.model.step1_count"));
        assertNull(stored.getElement("data.model.cid"), "reserved model keys must not persist");
        assertNull(stored.getElement("data.model.instance"), "reserved model keys must not persist");
        assertNull(stored.getElement("data.model.flow"), "reserved model keys must not persist");
        assertNull(stored.getElement("data.model.trace"), "reserved model keys must not persist");
        assertNull(stored.getElement("data.model.run"), "the fresh/resume run flag must not persist");
        assertEquals(true, stored.getElement("data.run.step-1"));
        // run 2 with the same correlation ID: resume continues past the checkpoint
        var second = runGraph("unit-test-suspend-1", cid);
        assertEquals(200, second.getStatus());
        var completed = new MultiLevelMap((Map<String, Object>) second.getBody());
        assertEquals("two", completed.getElement("step"));
        assertEquals(1, completed.getElement("prior"), "restored model.step1_count must reach step-2");
        assertEquals(1, CountingStepTask.getCount("one", cid), "the suspension point must not re-execute");
        assertEquals(1, CountingStepTask.getCount("two", cid));
        assertEquals("resume", second.getHeader("x-run"), "graph.resume must flag the resumed condition");
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
    void freshVsExpiredIsApplicationLogicOnTheResumePath() throws TimeoutException {
        // absent and expired records are indistinguishable to the engine: graph.resume
        // continues along its forward path with model.run=fresh and the graph's own gate
        // decides - here an invalid fresh request is rejected with a declaratively
        // staged 404 (a graph can set the response status via output.status)
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-suspend-4", cid, Map.of("noise", true));
        assertEquals(404, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("no-record", body.getElement("reason"));
        assertEquals("fresh", body.getElement("run"), "graph.resume must flag the fresh condition");
        assertEquals(404, body.getElement("status"));
        assertEquals(0, CountingStepTask.getCount("x", cid), "the gate must not run the normal path");
        // a valid fresh request passes the same gate
        var accepted = runGraph("unit-test-suspend-4", Utility.getInstance().getUuid(), Map.of("start", true));
        assertEquals(200, accepted.getStatus());
        log.info("fresh-path gate verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void expiredRecordFallsBackToFreshRun() throws TimeoutException, IOException {
        var cid = Utility.getInstance().getUuid();
        var r1 = runGraph("unit-test-suspend-5", cid);
        assertEquals("suspended", new MultiLevelMap((Map<String, Object>) r1.getBody()).getElement("type"));
        assertEquals(1, CountingStepTask.getCount("expiry", cid));
        expireStoredRecord(cid);
        // the record's expiry has passed: the resume falls back to a fresh run and suspends again
        var r2 = runGraph("unit-test-suspend-5", cid);
        assertEquals("suspended", new MultiLevelMap((Map<String, Object>) r2.getBody()).getElement("type"));
        assertEquals(2, CountingStepTask.getCount("expiry", cid), "an expired record means a fresh run");
        log.info("ttl expiry fallback verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void restoredRecordCannotOverrideReservedModelKeys() throws TimeoutException, IOException {
        var cid = Utility.getInstance().getUuid();
        var first = runGraph("unit-test-suspend-1", cid);
        assertEquals(200, first.getStatus());
        // forge the persisted record: the store is pluggable, so a record is external
        // input - inject reserved keys into its model as a hostile writer would
        var file = storedFile(cid);
        var msgPack = new MsgPack();
        var wrapper = (Map<String, Object>) msgPack.unpack(Files.readAllBytes(file.toPath()));
        var forged = new MultiLevelMap(wrapper);
        forged.setElement("data.model.cid", "forged-cid");
        forged.setElement("data.model.instance", "forged-instance");
        forged.setElement("data.model.run", "resume");
        // composite-path vectors, injected as LITERAL record keys: the restore merge
        // must treat persisted keys literally (putAll) - a path-interpreting write
        // would let "cid.x" descend into and replace model.cid (the trap the Rust
        // port's consistency review caught; this pins the Java immunity)
        if (forged.getElement("data.model") instanceof Map<?, ?> forgedModel) {
            var literal = (Map<String, Object>) forgedModel;
            literal.put("cid.x", "forged-nested");
            literal.put("ttl[0]", "forged-indexed");
        }
        Files.write(file.toPath(), msgPack.pack(forged.getMap()));
        // resume with the real correlation ID: the workflow continues, but none of the
        // forged reserved keys may reach the state machine - model.cid is a capability
        var second = runGraph("unit-test-suspend-1", cid);
        assertEquals(200, second.getStatus());
        var completed = new MultiLevelMap((Map<String, Object>) second.getBody());
        assertEquals("two", completed.getElement("step"));
        assertEquals(cid, CountingStepTask.getBusinessCid("two", cid),
                "the current run's model.cid must survive a forged record");
        log.info("reserved-key strip on restore verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void jumpModeDecisionReExecutesOnEveryResume() throws TimeoutException, IOException {
        var cid = Utility.getInstance().getUuid();
        // run 1: no decision - the gate jumps to the island-anchored suspend node
        // (no drawn edge from the gate) and stages its own waiting reply
        var r1 = runGraph("unit-test-suspend-6", cid, Map.of("noise", true));
        assertEquals(200, r1.getStatus());
        var waiting1 = new MultiLevelMap((Map<String, Object>) r1.getBody());
        assertEquals("waiting", waiting1.getElement("stage"), "the decision stages the caller's reply");
        assertEquals("fresh", waiting1.getElement("run"));
        // the persisted suspension point is the DECISION that jumped, not the suspend node
        assertEquals("gate", readStoredRecord(cid).getElement("data.node"));
        // run 2: still no decision - the gate RE-EXECUTES against the new input and
        // re-suspends; before jump-mode re-execution this dead-ended (a node marked
        // seen never re-dispatches and the persisted seen marks include the gate)
        var r2 = runGraph("unit-test-suspend-6", cid, Map.of("noise", true));
        assertEquals(200, r2.getStatus());
        var waiting2 = new MultiLevelMap((Map<String, Object>) r2.getBody());
        assertEquals("waiting", waiting2.getElement("stage"));
        assertEquals("resume", waiting2.getElement("run"), "the second wait is a resumed run");
        assertEquals("gate", readStoredRecord(cid).getElement("data.node"), "re-suspension re-persists");
        assertEquals(0, CountingStepTask.getCount("go-step", cid), "the continuing path must not run yet");
        // run 3: the decision arrives - the re-executed gate routes to the continuing path
        var r3 = runGraph("unit-test-suspend-6", cid, Map.of("decision", "go"));
        assertEquals(200, r3.getStatus());
        var completed = new MultiLevelMap((Map<String, Object>) r3.getBody());
        assertEquals("go-step", completed.getElement("step"));
        assertEquals(1, CountingStepTask.getCount("go-step", cid));
        assertEquals("resume", r3.getHeader("x-run"));
        assertFalse(storedFile(cid).exists(), "the record must be consumed on the final resume");
        log.info("jump-mode re-execution wait loop verified for cid {}", cid);
    }

    @SuppressWarnings("unchecked")
    @Test
    void retiredSuspendPropertyWithoutEdgeDeploysAndNeverSuspends() throws TimeoutException {
        // deprecation compat: 'suspend=true' is ignored - with no drawn edge to the
        // suspend node this graph deploys (with a gate WARN) and runs to completion
        var cid = Utility.getInstance().getUuid();
        var response = runGraph("unit-test-suspend-compat-1", cid);
        assertEquals(200, response.getStatus());
        var body = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("finished", body.getElement("stage"));
        assertEquals(1, CountingStepTask.getCount("compat-step", cid));
        assertFalse(storedFile(cid).exists(), "the retired property must not suspend");
        log.info("retired-property compat shape verified for cid {}", cid);
    }

    @Test
    void rejectedDeployedGraphIsNotExecutable() throws TimeoutException {
        // every suspend-err graph failed the CompileGraph quality gate, so a request
        // answers 404 as if the model does not exist - deployed execution is served
        // exclusively from the compiled registry (CompileFlows parity). Notably err6
        // (suspend node without an outgoing connection) would otherwise persist the
        // record and stall the run until the HTTP timeout; the runtime guards remain
        // the enforcement floor for the playground dry-run surface only.
        for (var id : List.of("unit-test-suspend-err1", "unit-test-suspend-err2", "unit-test-suspend-err3",
                              "unit-test-suspend-err4", "unit-test-suspend-err5", "unit-test-suspend-err6",
                              "unit-test-suspend-err7", "unit-test-no-end")) {
            var response = runGraph(id, Utility.getInstance().getUuid());
            assertEquals(404, response.getStatus(), id + " must be rejected as not found");
            assertTrue(String.valueOf(response.getBody()).contains("not found"),
                    "unexpected error response: " + response.getBody());
        }
    }

    @Test
    void ttlIsMandatoryAndOverflowIsRejected() {
        // only the workflow designer knows whether a checkpoint waits a minute or days -
        // a default expiry would silently discard someone's workflow; and the int
        // computation in Utility.getDurationInSeconds wraps for absurd values, so the
        // shared long-math parser must reject them instead of silently expiring early
        assertThrows(IllegalArgumentException.class,
                () -> GraphSuspend.getValidTtlSeconds(null, "suspend"));
        assertThrows(IllegalArgumentException.class,
                () -> GraphSuspend.getValidTtlSeconds("  ", "suspend"));
        assertThrows(IllegalArgumentException.class,
                () -> GraphSuspend.getValidTtlSeconds("25000000d", "suspend"));
        assertThrows(IllegalArgumentException.class,
                () -> GraphSuspend.getValidTtlSeconds("0s", "suspend"));
        assertEquals(172800, GraphSuspend.getValidTtlSeconds("2d", "suspend"));
        assertEquals(300, GraphSuspend.getValidTtlSeconds("5m", "suspend"));
        assertEquals(20, GraphSuspend.getValidTtlSeconds("20", "suspend"));
    }

    @Test
    void helpFilesFollowNamingConvention() {
        assertNotNull(getClass().getResourceAsStream("/help/help graph-suspend.md"),
                "help file for graph.suspend is missing");
        assertNotNull(getClass().getResourceAsStream("/help/help graph-resume.md"),
                "help file for graph.resume is missing");
    }

    private EventEnvelope runGraph(String graphId, String cid) throws TimeoutException {
        return runGraph(graphId, cid, Map.of("start", true));
    }

    private EventEnvelope runGraph(String graphId, String cid, Map<String, Object> body) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(target)
                .setUrl("/api/graph/" + graphId)
                .setBody(body)
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

    /**
     * Rewrite the stored record's expiry into the past - a deterministic stand-in for waiting out
     * the ttl. The store enforces expiry at retrieval time, so an expired stamp behaves exactly
     * like elapsed wall-clock time, without a sleep in the test.
     */
    @SuppressWarnings("unchecked")
    private void expireStoredRecord(String cid) throws IOException {
        var file = storedFile(cid);
        var wrapper = (Map<String, Object>) new MsgPack().unpack(Files.readAllBytes(file.toPath()));
        wrapper.put("expires_at", System.currentTimeMillis() - 1000);
        Files.write(file.toPath(), new MsgPack().pack(wrapper));
    }

    private File storedFile(String cid) {
        return new File(STORE_DIR, cid);
    }

    private String util32HexTraceId(String seed) {
        return String.format("%032x", Math.abs(seed.hashCode()));
    }
}
