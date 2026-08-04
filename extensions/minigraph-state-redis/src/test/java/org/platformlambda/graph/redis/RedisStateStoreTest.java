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

package org.platformlambda.graph.redis;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for the Redis state store, exercised THROUGH the event system so the
 * whole path is real: PreLoad auto-registration, MsgPack transit of the persistence
 * envelope, SETEX with native expiry, and atomic GETDEL consumption.
 */
class RedisStateStoreTest extends RedisStateTestBase {
    private static final String PERSIST = "v1.redis.persist.model";
    private static final String RETRIEVE = "v1.redis.retrieve.model";
    private static final long TIMEOUT = 8000;

    @Test
    void functionsRegisterAutomaticallyFromClasspath() {
        // the deployment story: include the jar and the two functions self-register
        var platform = Platform.getInstance();
        assertTrue(platform.hasRoute(PERSIST), PERSIST + " must self-register");
        assertTrue(platform.hasRoute(RETRIEVE), RETRIEVE + " must self-register");
    }

    @SuppressWarnings("unchecked")
    @Test
    void persistRetrieveRoundTripWithConsume() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        var envelope = sampleEnvelope(cid, 30);
        var stored = request(PERSIST, "put", envelope);
        assertEquals(200, stored.getStatus());
        assertEquals(true, new MultiLevelMap((Map<String, Object>) stored.getBody()).getElement("stored"));
        // native expiry is set
        var ttl = testConnection.sync().ttl("graph:state:" + cid);
        assertTrue(ttl > 0 && ttl <= 30, "unexpected ttl: " + ttl);
        // retrieve returns the record with full fidelity, including binary values
        var restored = request(RETRIEVE, "get", Map.of("cid", cid));
        assertEquals(200, restored.getStatus());
        var restoredRecord = new MultiLevelMap((Map<String, Object>) restored.getBody());
        assertEquals("step-1", restoredRecord.getElement("node"));
        assertEquals(42, restoredRecord.getElement("model.amount"));
        assertEquals("approval", restoredRecord.getElement("model.nested.stage"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) restoredRecord.getElement("model.binary"));
        assertEquals(true, restoredRecord.getElement("run.step-1"));
        // the record is consumed atomically - a duplicate resume finds nothing
        var again = request(RETRIEVE, "get", Map.of("cid", cid));
        assertEquals(200, again.getStatus());
        assertTrue(((Map<String, Object>) again.getBody()).isEmpty(), "the record must be consumed on read");
        assertNull(testConnection.sync().get("graph:state:" + cid));
    }

    @SuppressWarnings("unchecked")
    @Test
    void transactionalConsumeGivesTheSameContractOnPre62Servers() throws TimeoutException {
        // enterprise Redis versions are outside our control (managed AWS/Azure/GCP servers; the
        // redis-standalone Windows binary is 5.0.14): the pre-6.2 strategy replaces GETDEL with a
        // MULTI/EXEC GET+DEL transaction. MULTI/EXEC works on every server, so the fallback is
        // exercised for real against the embedded 6.2 server - same contract, both strategies.
        RedisStateConnection.overrideConsumeStrategy(false);
        try {
            var cid = Utility.getInstance().getUuid();
            assertEquals(200, request(PERSIST, "put", sampleEnvelope(cid, 30)).getStatus());
            var restored = request(RETRIEVE, "get", Map.of("cid", cid));
            assertEquals(200, restored.getStatus());
            var restoredRecord = new MultiLevelMap((Map<String, Object>) restored.getBody());
            assertEquals("step-1", restoredRecord.getElement("node"));
            assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) restoredRecord.getElement("model.binary"));
            // consumed atomically by the transaction - a duplicate resume finds nothing
            var again = request(RETRIEVE, "get", Map.of("cid", cid));
            assertEquals(200, again.getStatus());
            assertTrue(((Map<String, Object>) again.getBody()).isEmpty(),
                    "the record must be consumed on read by the transactional strategy");
            assertNull(testConnection.sync().get("graph:state:" + cid));
        } finally {
            RedisStateConnection.overrideConsumeStrategy(true);
        }
    }

    @Test
    void redisVersionIsExtractedFromInfoOutput() {
        assertEquals("6.2.7", RedisStateConnection.redisVersion(
                "# Server\r\nredis_version:6.2.7\r\nredis_mode:standalone\r\n"));
        assertEquals("5.0.14.1", RedisStateConnection.redisVersion("redis_version:5.0.14.1\r\n"));
        assertEquals("unknown", RedisStateConnection.redisVersion("# Server\r\nredis_mode:standalone\r\n"));
    }

    @Test
    void getdelNeedsRedis62OrLater() {
        assertTrue(RedisStateConnection.supportsGetdel("6.2.0"));
        assertTrue(RedisStateConnection.supportsGetdel("6.2.7"));
        assertTrue(RedisStateConnection.supportsGetdel("7.4.1"));
        assertTrue(RedisStateConnection.supportsGetdel("8.0"));
        assertFalse(RedisStateConnection.supportsGetdel("6.0.9"));
        assertFalse(RedisStateConnection.supportsGetdel("5.0.14.1"));   // the Windows binary
        assertFalse(RedisStateConnection.supportsGetdel("3.0.504"));
        // an unparseable version selects the transactional fallback, which works everywhere
        assertFalse(RedisStateConnection.supportsGetdel("unknown"));
        assertFalse(RedisStateConnection.supportsGetdel("x.y.z"));
        assertFalse(RedisStateConnection.supportsGetdel("7"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void absentCorrelationIdIsANormalEmptyResult() throws TimeoutException {
        var response = request(RETRIEVE, "get", Map.of("cid", Utility.getInstance().getUuid()));
        assertEquals(200, response.getStatus());
        assertTrue(((Map<String, Object>) response.getBody()).isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void expiredRecordIsGone() throws TimeoutException {
        var cid = Utility.getInstance().getUuid();
        assertEquals(200, request(PERSIST, "put", sampleEnvelope(cid, 1)).getStatus());
        Utility.getInstance().sleep(1300);
        var response = request(RETRIEVE, "get", Map.of("cid", cid));
        assertEquals(200, response.getStatus());
        assertTrue(((Map<String, Object>) response.getBody()).isEmpty(), "the record must expire natively");
    }

    @Test
    void wrongRequestTypeIsRejected() throws TimeoutException {
        var response = request(PERSIST, "get", sampleEnvelope(Utility.getInstance().getUuid(), 30));
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("Type must be put"));
    }

    @Test
    void missingCorrelationIdIsRejected() throws TimeoutException {
        var response = request(PERSIST, "put", Map.of("ttl", 30));
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("Missing cid"));
    }

    @Test
    void invalidTtlIsRejected() throws TimeoutException {
        var response = request(PERSIST, "put", Map.of("cid", Utility.getInstance().getUuid()));
        assertNotEquals(200, response.getStatus());
        assertTrue(String.valueOf(response.getBody()).contains("Invalid ttl"));
    }

    private Map<String, Object> sampleEnvelope(String cid, int ttlSeconds) {
        var model = new HashMap<String, Object>();
        model.put("amount", 42);
        model.put("binary", new byte[]{1, 2, 3});
        model.put("nested", Map.of("stage", "approval"));
        var envelope = new HashMap<String, Object>();
        envelope.put("cid", cid);
        envelope.put("node", "step-1");
        envelope.put("ttl", ttlSeconds);
        envelope.put("model", model);
        envelope.put("seen", Map.of("root", true, "resume", true, "step-1", true));
        envelope.put("run", Map.of("resume", true, "step-1", true));
        return envelope;
    }

    private EventEnvelope request(String route, String type, Map<String, Object> body) throws TimeoutException {
        var po = PostOffice.trackable("unit.test", Utility.getInstance().getUuid(), "TEST /state/store");
        var event = new EventEnvelope().setTo(route).setHeader("type", type).setBody(body);
        return po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
    }
}
