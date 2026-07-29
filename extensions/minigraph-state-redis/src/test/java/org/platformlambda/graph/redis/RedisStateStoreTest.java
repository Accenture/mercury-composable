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
        var record = new MultiLevelMap((Map<String, Object>) restored.getBody());
        assertEquals("step-1", record.getElement("node"));
        assertEquals(42, record.getElement("model.amount"));
        assertEquals("approval", record.getElement("model.nested.stage"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) record.getElement("model.binary"));
        assertEquals(true, record.getElement("run.step-1"));
        // the record is consumed atomically - a duplicate resume finds nothing
        var again = request(RETRIEVE, "get", Map.of("cid", cid));
        assertEquals(200, again.getStatus());
        assertTrue(((Map<String, Object>) again.getBody()).isEmpty(), "the record must be consumed on read");
        assertNull(testConnection.sync().get("graph:state:" + cid));
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
    void expiredRecordIsGone() throws TimeoutException, InterruptedException {
        var cid = Utility.getInstance().getUuid();
        assertEquals(200, request(PERSIST, "put", sampleEnvelope(cid, 1)).getStatus());
        Thread.sleep(1300);
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
