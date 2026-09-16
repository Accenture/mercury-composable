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

package org.platformlambda.core.util;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.PoJo;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract gate for {@link ElasticStore}: the same assertions the original ElasticQueueTest exercised,
 * run against {@link FileElasticStore}.
 *
 * <p>This began life as a PARITY gate running every assertion against both the Berkeley DB store and the
 * then-new file store, to prove the replacement behaved identically. The Berkeley DB store was retired in
 * v4.12.10 once the file store had run clean in the field, so there is no second implementation to compare
 * against - but the assertions are exactly the behaviour the retired store had proven, so they are kept as
 * the standing contract. {@link FileElasticStoreTest} covers file-specific internals (segment rolling,
 * reclamation, stale directories); this covers the behaviour any store must have.
 * (In-package so it can reach the package-private store directly, independent of config.)</p>
 */
class ElasticStoreContractTest {

    private static ElasticStore store(String id) {
        return new FileElasticStore(id);
    }

    private static String body(byte[] b) {
        EventEnvelope e = new EventEnvelope();
        e.load(b);
        return (String) e.getBody();
    }

    @Test
    void peeking() {
        String firstItem = "hello world 1";
        String secondItem = "hello world 2";
        ElasticStore spooler = store("contract.peek");
        spooler.write(new EventEnvelope().setBody(firstItem).toBytes());
        spooler.write(new EventEnvelope().setBody(secondItem).toBytes());
        assertEquals(firstItem, body(spooler.peek()));
        assertEquals(firstItem, body(spooler.read()));
        assertEquals(secondItem, body(spooler.read()));
        assertEquals(0, spooler.peek().length);
        assertEquals(0, spooler.read().length);
        // elastic queue should be automatically closed when all messages are consumed
        assertTrue(spooler.isClosed());
        spooler.close();
    }

    @Test
    void closeAfterPeekReuseClearsCachedEvent() {
        ElasticStore spooler = store("contract.peek.close");
        spooler.write(new EventEnvelope().setBody("a").toBytes());
        assertEquals("a", body(spooler.peek()));
        spooler.close();
        spooler.write(new EventEnvelope().setBody("b").toBytes());
        assertEquals("b", body(spooler.read()));
        assertEquals(0, spooler.read().length);
        spooler.destroy();
    }

    @Test
    void normalPayload() {
        readWrite("contract.normal", 10);
    }

    @Test
    void largePayload() {
        readWrite("contract.large", 90000);
    }

    private void readWrite(String path, int size) {
        String target = "hello.world";
        String baseText = "0123456789".repeat(Math.max(0, size)) + ": ";
        ElasticStore spooler = store(path);
        // immediate read after write (stays within the memory tier)
        for (int i = 0; i < ElasticStore.MEMORY_BUFFER * 3; i++) {
            String input = baseText + i;
            EventEnvelope event = new EventEnvelope().setTo(target).setBody(input);
            spooler.write(event.toBytes());
            byte[] b = spooler.read();
            assertNotEquals(0, b.length);
            EventEnvelope data = new EventEnvelope();
            data.load(b);
            assertEquals(input, data.getBody());
        }
        // force overflow to the disk tier
        for (int i = 0; i < ElasticStore.MEMORY_BUFFER * 5; i++) {
            String input = baseText + i;
            PoJo pojo = new PoJo();
            pojo.setName(input);
            EventEnvelope event = new EventEnvelope().setTo(target).setBody(pojo);
            spooler.write(event.toBytes());
        }
        // then read 4/5 of the messages back in order
        for (int i = 0; i < ElasticStore.MEMORY_BUFFER * 4; i++) {
            String input = baseText + i;
            byte[] b = spooler.read();
            EventEnvelope data = new EventEnvelope();
            data.load(b);
            assertInstanceOf(Map.class, data.getBody());
            PoJo o = data.getBody(PoJo.class);
            assertEquals(input, o.getName());
        }
        byte[] someData = spooler.read();
        assertNotNull(someData);
        spooler.close();
        byte[] nothing = spooler.read();
        assertEquals(0, nothing.length);
        assertTrue(spooler.isClosed());
        spooler.close();
    }

    @Test
    void cleanup() {
        String helloWorld = "hello world ";
        try (ElasticStore spooler = store("contract.unread")) {
            for (int i = 0; i < ElasticStore.MEMORY_BUFFER * 3; i++) {
                String input = helloWorld + i;
                EventEnvelope event = new EventEnvelope().setTo("hello.world").setBody(input);
                spooler.write(event.toBytes());
                if (i < ElasticStore.MEMORY_BUFFER) {
                    byte[] b = spooler.read();
                    EventEnvelope data = new EventEnvelope();
                    data.load(b);
                    assertEquals(input, data.getBody());
                }
            }
            // half the events are unread on disk → close() (via try-with-resources) must reclaim cleanly
            assertFalse(spooler.isClosed());
        }
    }
}
