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
 * Parity gate for the two {@link ElasticStore} implementations: the same contract the original
 * ElasticQueueTest exercised, run against BOTH {@link BdbElasticStore} and {@link FileElasticStore}.
 * Same assertions, both impls → proves the new file store behaves identically to the proven BDB one.
 * (In-package so it can reach the package-private stores directly, independent of config selection.)
 */
class ElasticStoreParityTest {

    private static final String[] IMPLS = {"bdb", "file"};

    private static ElasticStore store(String type, String id) {
        return "file".equals(type) ? new FileElasticStore(id) : new BdbElasticStore(id);
    }

    private static String body(byte[] b) {
        EventEnvelope e = new EventEnvelope();
        e.load(b);
        return (String) e.getBody();
    }

    @Test
    void peeking() {
        for (String type : IMPLS) {
            String firstItem = "hello world 1";
            String secondItem = "hello world 2";
            ElasticStore spooler = store(type, "parity." + type + ".peek");
            spooler.write(new EventEnvelope().setBody(firstItem).toBytes());
            spooler.write(new EventEnvelope().setBody(secondItem).toBytes());
            assertEquals(firstItem, body(spooler.peek()), type);
            assertEquals(firstItem, body(spooler.read()), type);
            assertEquals(secondItem, body(spooler.read()), type);
            assertEquals(0, spooler.peek().length, type);
            assertEquals(0, spooler.read().length, type);
            // elastic queue should be automatically closed when all messages are consumed
            assertTrue(spooler.isClosed(), type);
            spooler.close();
        }
    }

    @Test
    void closeAfterPeekReuseClearsCachedEvent() {
        for (String type : IMPLS) {
            ElasticStore spooler = store(type, "parity." + type + ".peek.close");
            spooler.write(new EventEnvelope().setBody("a").toBytes());
            assertEquals("a", body(spooler.peek()), type);
            spooler.close();
            spooler.write(new EventEnvelope().setBody("b").toBytes());
            assertEquals("b", body(spooler.read()), type);
            assertEquals(0, spooler.read().length, type);
            spooler.destroy();
        }
    }

    @Test
    void normalPayload() {
        for (String type : IMPLS) {
            readWrite(type, "parity." + type + ".normal", 10);
        }
    }

    @Test
    void largePayload() {
        for (String type : IMPLS) {
            readWrite(type, "parity." + type + ".large", 90000);
        }
    }

    @SuppressWarnings("unchecked")
    private void readWrite(String type, String path, int size) {
        String target = "hello.world";
        String baseText = "0123456789".repeat(Math.max(0, size)) + ": ";
        ElasticStore spooler = store(type, path);
        // immediate read after write (stays within the memory tier)
        for (int i = 0; i < ElasticStore.MEMORY_BUFFER * 3; i++) {
            String input = baseText + i;
            EventEnvelope event = new EventEnvelope().setTo(target).setBody(input);
            spooler.write(event.toBytes());
            byte[] b = spooler.read();
            assertNotEquals(0, b.length, type);
            EventEnvelope data = new EventEnvelope();
            data.load(b);
            assertEquals(input, data.getBody(), type);
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
            assertInstanceOf(Map.class, data.getBody(), type);
            PoJo o = data.getBody(PoJo.class);
            assertEquals(input, o.getName(), type);
        }
        byte[] someData = spooler.read();
        assertNotNull(someData, type);
        spooler.close();
        byte[] nothing = spooler.read();
        assertEquals(0, nothing.length, type);
        assertTrue(spooler.isClosed(), type);
        spooler.close();
    }

    @Test
    void cleanup() {
        String helloWorld = "hello world ";
        for (String type : IMPLS) {
            try (ElasticStore spooler = store(type, "parity." + type + ".unread")) {
                for (int i = 0; i < ElasticStore.MEMORY_BUFFER * 3; i++) {
                    String input = helloWorld + i;
                    EventEnvelope event = new EventEnvelope().setTo("hello.world").setBody(input);
                    spooler.write(event.toBytes());
                    if (i < ElasticStore.MEMORY_BUFFER) {
                        byte[] b = spooler.read();
                        EventEnvelope data = new EventEnvelope();
                        data.load(b);
                        assertEquals(input, data.getBody(), type);
                    }
                }
                // half the events are unread on disk → close() (via try-with-resources) must reclaim cleanly
                assertFalse(spooler.isClosed(), type);
            }
        }
    }
}
