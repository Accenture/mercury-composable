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
import org.platformlambda.common.TestBase;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.services.DistributedTrace;
import org.platformlambda.core.services.TemporaryInbox;
import org.platformlambda.core.system.*;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ObjectStreamTest extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(ObjectStreamTest.class);
    private static final String TYPE = "type";
    private static final String DATA = "data";
    private static final String READ = "read";
    private static final String END_OF_STREAM = "eof";

    @SuppressWarnings("unchecked")
    @Test
    void streamConsumerTest() throws IOException, InterruptedException, ExecutionException {
        final long TIME_TO_LIVE = 3000;
        final String FIRST_MESSAGE = "first message";
        final String SECOND_MESSAGE = "second message";
        final String THIRD_MESSAGE = "third message";
        // Step 1 - user function create a Flux or pass the Flux object from a database
        //          (note that emitter will not start until FluxPublish begins publishing)
        Flux<String> source = Flux.create(emitter -> {
            emitter.next(FIRST_MESSAGE);
            emitter.next(SECOND_MESSAGE);
            emitter.next(THIRD_MESSAGE);
            emitter.complete();
        });
        // Step 2 - do data transformation
        Flux<Map<String, Object>> filtered = source.map(d -> Map.of(DATA, d));
        // Step 3 - called function creates publisher from a Flux object
        FluxPublisher<Map<String, Object>> fluxRelay = new FluxPublisher<>(filtered, TIME_TO_LIVE);
        String streamId = fluxRelay.publish();
        // Blocking queue is used in Unit Test for demo purpose only
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final List<Object> messages = new ArrayList<>();
        // Step 4 - calling function creates consumer from a streamId
        var po = new PostOffice("unit.test", "202", "STREAM /low/level");
        Platform platform = Platform.getInstance();
        platform.registerPrivate(TemporaryInbox.TEMPORARY_INBOX, new TemporaryInbox(), 1);
        platform.registerPrivate(DistributedTrace.DISTRIBUTED_TRACING, new DistributedTrace(), 1);
        // use low-level stream READ protocol. This demonstrates compatibility with Composable Node.js version.
        var req = new EventEnvelope().setTo(streamId).setCorrelationId("101").setHeader(TYPE, READ)
                        .setTrace("202", "STREAM /low/level");
        while (true) {
            var res = po.request(req, 5000).get();
            // replyTo is set to the request event but input event is guaranteed to be immutable, thus null value.
            assertNull(req.getReplyTo());
            String type = res.getHeader(TYPE);
            if (END_OF_STREAM.equals(type)) {
                log.info("END OF STREAM");
                break;
            }
            if (DATA.equals(type) && res.getBody() != null) {
                log.info("correlationId={}, message={}", res.getCorrelationId(), res.getBody());
                messages.add(res.getBody());
                assertEquals("101", res.getCorrelationId());
            }
        }
        assertEquals(3, messages.size());
        assertInstanceOf(Map.class, messages.get(0));
        assertInstanceOf(Map.class, messages.get(1));
        assertInstanceOf(Map.class, messages.get(2));
        Object message1 = ((Map<String, Object>) messages.get(0)).get(DATA);
        Object message2 = ((Map<String, Object>) messages.get(1)).get(DATA);
        Object message3 = ((Map<String, Object>) messages.get(2)).get(DATA);
        assertEquals(FIRST_MESSAGE, message1);
        assertEquals(SECOND_MESSAGE, message2);
        assertEquals(THIRD_MESSAGE, message3);
    }

    @SuppressWarnings("unchecked")
    @Test
    void fluxProducerConsumerTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 3000;
        final String FIRST_MESSAGE = "first message";
        final String SECOND_MESSAGE = "second message";
        // Step 1 - user function create a Flux or pass the Flux object from a database
        //          (note that emitter will not start until FluxPublish begins publishing)
        Flux<String> source = Flux.create(emitter -> {
            emitter.next(FIRST_MESSAGE);
            emitter.next(SECOND_MESSAGE);
            emitter.complete();
        });
        // Step 2 - do data transformation
        Flux<Map<String, Object>> filtered = source.map(d -> Map.of(DATA, d));
        // Step 3 - called function creates publisher from a Flux object
        FluxPublisher<Map<String, Object>> fluxRelay = new FluxPublisher<>(filtered, TIME_TO_LIVE);
        String streamId = fluxRelay.publish();
        // Blocking queue is used in Unit Test for demo purpose only
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final List<Object> messages = new ArrayList<>();
        // Step 4 - calling function creates consumer from a streamId
        FluxConsumer<Map<String, Object>> fc = new FluxConsumer<>(streamId, TIME_TO_LIVE);
        fc.consume(messages::add, e -> {
                    messages.add(e);
                    bench.add(false);
                }, () -> bench.add(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(true, signal);
        assertEquals(2, messages.size());
        assertInstanceOf(Map.class, messages.get(0));
        assertInstanceOf(Map.class, messages.get(1));
        Object message1 = ((Map<String, Object>) messages.get(0)).get(DATA);
        Object message2 = ((Map<String, Object>) messages.get(1)).get(DATA);
        assertEquals(FIRST_MESSAGE, message1);
        assertEquals(SECOND_MESSAGE, message2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void fluxProducerConsumerExceptionTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 3000;
        final String FIRST_MESSAGE = "first message";
        final String SECOND_MESSAGE = "second message";
        final String DEMO_EXCEPTION = "demo exception";
        // Step 1 - user function create a Flux or pass the Flux object from a database
        //          (note that emitter will not start until FluxPublish begins publishing)
        Flux<String> source = Flux.create(emitter -> {
            emitter.next(FIRST_MESSAGE);
            emitter.next(SECOND_MESSAGE);
            emitter.error(new AppException(400, DEMO_EXCEPTION));
        });
        // Step 2 - do data transformation
        Flux<Map<String, Object>> filtered = source.map(d -> Map.of(DATA, d));
        // Step 3 - called function creates publisher from a Flux object
        FluxPublisher<Map<String, Object>> fluxRelay = new FluxPublisher<>(filtered, TIME_TO_LIVE);
        String streamId = fluxRelay.publish();
        // Blocking queue is used in Unit Test for demo purpose only
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final List<Object> messages = new ArrayList<>();
        // Step 4 - calling function creates consumer from a streamId
        FluxConsumer<Map<String, Object>> fc = new FluxConsumer<>(streamId, TIME_TO_LIVE);
        fc.consume(messages::add, e -> {
            messages.add(e);
            bench.add(false);
        }, () -> bench.add(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(3, messages.size());
        assertInstanceOf(Map.class, messages.get(0));
        assertInstanceOf(Map.class, messages.get(1));
        assertInstanceOf(AppException.class, messages.get(2));
        Object message1 = ((Map<String, Object>) messages.get(0)).get(DATA);
        Object message2 = ((Map<String, Object>) messages.get(1)).get(DATA);
        var ex = (AppException) messages.get(2);
        assertEquals(FIRST_MESSAGE, message1);
        assertEquals(SECOND_MESSAGE, message2);
        assertEquals(400, ex.getStatus());
        assertEquals(DEMO_EXCEPTION, ex.getMessage());
    }

    @Test
    void eventPublisherFluxConsumerCompatibilityTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 3000;
        final String FIRST_MESSAGE = "first message";
        final String SECOND_MESSAGE = "second message";
        final String DEMO_EXCEPTION = "demo exception";
        EventPublisher publisher = new EventPublisher(TIME_TO_LIVE);
        publisher.publish(FIRST_MESSAGE);
        publisher.publish(SECOND_MESSAGE);
        publisher.publishException(new AppException(400, DEMO_EXCEPTION));
        // Blocking queue is used in Unit Test for demo purpose only
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final List<Object> messages = new ArrayList<>();
        // Step 4 - calling function creates consumer from a streamId
        FluxConsumer<String> fc = new FluxConsumer<>(publisher.getStreamId(), TIME_TO_LIVE);
        fc.consume(messages::add, e -> {
            messages.add(e);
            bench.add(false);
        }, () -> bench.add(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(3, messages.size());
        assertInstanceOf(String.class, messages.get(0));
        assertInstanceOf(String.class, messages.get(1));
        assertInstanceOf(AppException.class, messages.get(2));
        assertEquals(FIRST_MESSAGE, messages.get(0));
        assertEquals(SECOND_MESSAGE, messages.get(1));
        AppException ex = (AppException) messages.get(2);
        assertEquals(400, ex.getStatus());
        assertEquals(DEMO_EXCEPTION, ex.getMessage());
    }

    @Test
    void eventPublisherExpiryTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 1000;
        EventPublisher publisher = new EventPublisher(TIME_TO_LIVE);
        Thread.sleep(1100);
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final List<Object> messages = new ArrayList<>();
        FluxConsumer<String> fc = new FluxConsumer<>(publisher.getStreamId(), TIME_TO_LIVE);
        fc.consume(messages::add, e -> {
            messages.add(e);
            bench.add(false);
        }, () -> bench.add(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(1, messages.size());
        assertInstanceOf(AppException.class, messages.getFirst());
        AppException ex = (AppException) messages.getFirst();
        assertEquals(408, ex.getStatus());
        assertEquals("Event stream expired", ex.getMessage());
    }

    @Test
    void fluxPublisherExpiryTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 1000;
        Flux<String> source = Flux.create(emitter -> {
            emitter.next("hello world");
            emitter.complete();
        });
        FluxPublisher<String> publisher = new FluxPublisher<>(source, TIME_TO_LIVE);
        Thread.sleep(1100);
        String streamId = publisher.publish();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final List<Object> messages = new ArrayList<>();
        FluxConsumer<String> fc = new FluxConsumer<>(streamId, TIME_TO_LIVE);
        fc.consume(messages::add, e -> {
            messages.add(e);
            bench.add(false);
        }, () -> bench.add(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(1, messages.size());
        assertInstanceOf(AppException.class, messages.getFirst());
        AppException ex = (AppException) messages.getFirst();
        assertEquals(408, ex.getStatus());
        assertEquals("Event stream expired", ex.getMessage());
    }

    @Test
    void expiryTest() throws InterruptedException, IOException {
        final BlockingQueue<String> dataBench = new ArrayBlockingQueue<>(1);
        final BlockingQueue<Throwable> exceptionBench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        String TEXT = "hello world";
        // The minimum timeout is one second if you set it to a smaller value
        ObjectStreamIO unused = new ObjectStreamIO(0);
        assertEquals(1, unused.getExpirySeconds());
        String unusedStream = unused.getInputStreamId().substring(0, unused.getInputStreamId().indexOf('@'));
        // create a stream with 3 second expiry
        EventPublisher publisher = new EventPublisher(3000);
        publisher.publish(TEXT);
        Map<String, Object> info = ObjectStreamIO.getStreamInfo();
        assertNotNull(info.get("count"));
        int count = util.str2int(info.get("count").toString());
        assertTrue(count > 0);
        String id = publisher.getStreamId().substring(0, publisher.getStreamId().indexOf('@'));
        assertTrue(info.containsKey(id));
        FluxConsumer<String> flux = new FluxConsumer<>(publisher.getStreamId(), 2000);
        flux.consume(dataBench::add, exceptionBench::add, null);
        String result = dataBench.poll(10, TimeUnit.SECONDS);
        assertEquals(TEXT, result);
        // The stream is intentionally left open so the consumer will time out
        Throwable e = exceptionBench.poll(5, TimeUnit.SECONDS);
        assertInstanceOf(AppException.class, e);
        var exception = (AppException) e;
        assertEquals(408, exception.getStatus());
        // consume has expiry time of 2 seconds and publisher would expire in 3 seconds
        // we want to see FluxConsumer expiry here so it is set to a smaller value than the publisher
        assertEquals("Consumer expired", exception.getMessage());
        /*
         * The system will check expired streams every 30 seconds
         * To avoid waiting it in a unit test, we force it to remove expired streams
         */
        ObjectStreamIO.removeExpiredStreams();
    }

    @Test
    void asyncReadWrite() throws InterruptedException, IOException {
        int CYCLES = 10;
        String TEXT = "hello world";
        EventPublisher publisher = new EventPublisher(10000);
        log.info("Using {}", publisher.getStreamId());
        for (int i = 0; i < CYCLES; i++) {
            publisher.publish(TEXT + " " + i);
        }
        publisher.publishCompletion();
        List<String> result = new ArrayList<>();
        BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        FluxConsumer<String> flux = new FluxConsumer<>(publisher.getStreamId(), 8000);
        flux.consume(result::add, null, () -> bench.add(true));
        Boolean done = bench.poll(10, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertEquals(CYCLES, result.size());
        for (String s: result) {
            assertTrue(s.startsWith(TEXT));
        }
    }
}
