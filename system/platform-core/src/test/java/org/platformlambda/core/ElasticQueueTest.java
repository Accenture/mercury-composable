/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.util.ElasticQueue;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ElasticQueueTest {

    @Test
    void peeking() throws IOException {
        String firstItem = "hello world 1";
        String secondItem = "hello world 2";
        ElasticQueue spooler = new ElasticQueue("unit.test");
        spooler.write(new EventEnvelope().setBody(firstItem).toBytes());
        spooler.write(new EventEnvelope().setBody(secondItem).toBytes());

        byte[] b = spooler.peek();
        EventEnvelope first = new EventEnvelope();
        first.load(b);
        assertEquals(firstItem, first.getBody());

        b = spooler.read();
        EventEnvelope firstAgain = new EventEnvelope();
        firstAgain.load(b);
        assertEquals(firstItem, firstAgain.getBody());

        b = spooler.read();
        EventEnvelope second = new EventEnvelope();
        second.load(b);
        assertEquals(secondItem, second.getBody());
        assertEquals(0, spooler.peek().length);
        assertEquals(0, spooler.read().length);
        // elastic queue should be automatically closed when all messages are consumed
        assertTrue(spooler.isClosed());
        // close elastic queue
        spooler.close();
    }

    @Test
    void normalPayload() throws IOException {
        readWrite("normal.payload.test", 10);
    }

    @Test
    void largePayload() throws IOException {
        readWrite("large.payload.test", 90000);
    }

    private void readWrite(String path, int size) throws IOException {
        String target = "hello.world";
        // create input
        String baseText = "0123456789".repeat(Math.max(0, size)) + ": ";
        ElasticQueue spooler = new ElasticQueue(path);
        // immediate read after write
        for (int i = 0; i < ElasticQueue.MEMORY_BUFFER * 3; i++) {
            String input = baseText + i;
            EventEnvelope event = new EventEnvelope();
            event.setTo(target);
            event.setBody(input);
            spooler.write(event.toBytes());
            byte[] b = spooler.read();
            assertNotEquals(0, b.length);
            EventEnvelope data = new EventEnvelope();
            data.load(b);
            assertEquals(input, data.getBody());
        }
        /*
         * Test overflow to temporary storage
         * by writing a larger number of messages to force buffering to disk
         */
        for (int i = 0; i < ElasticQueue.MEMORY_BUFFER * 5; i++) {
            String input = baseText+i;
            PoJo pojo = new PoJo();
            EventEnvelope event = new EventEnvelope();
            event.setTo(target);
            pojo.setName(input);
            event.setBody(pojo);
            spooler.write(event.toBytes());
        }
        // then 4/5 of the messages
        for (int i = 0; i < ElasticQueue.MEMORY_BUFFER * 4; i++) {
            String input = baseText+i;
            byte[] b = spooler.read();
            EventEnvelope data = new EventEnvelope();
            data.load(b);
            assertNotNull(data);
            assertInstanceOf(Map.class, data.getBody());
            PoJo o = data.getBody(PoJo.class);
            assertEquals(input, o.getName());
        }
        // read one more
        byte[] someData = spooler.read();
        assertNotNull(someData);
        spooler.close();
        // it should return null when there are no more messages to be read
        byte[] nothing = spooler.read();
        assertEquals(0, nothing.length);
        assertTrue(spooler.isClosed());
        // closing again has no effect
        spooler.close();
    }

    @Test
    void cleanupTest() throws IOException {
        String HELLO_WORLD = "hello world ";
        try (ElasticQueue spooler = new ElasticQueue("unread.test")) {
            for (int i = 0; i < ElasticQueue.MEMORY_BUFFER * 3; i++) {
                String input = HELLO_WORLD + i;
                EventEnvelope event = new EventEnvelope();
                event.setTo("hello.world");
                event.setBody(input);
                spooler.write(event.toBytes());
                if (i < ElasticQueue.MEMORY_BUFFER) {
                    byte[] b = spooler.read();
                    assertNotNull(b);
                    EventEnvelope data = new EventEnvelope();
                    data.load(b);
                    assertEquals(input, data.getBody());
                }
            }
            assertFalse(spooler.isClosed());
        }
    }

}
