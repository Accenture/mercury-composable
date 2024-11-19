/*

    Copyright 2018-2024 Accenture Technology

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

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectStreamTest {
    private static final Logger log = LoggerFactory.getLogger(ObjectStreamTest.class);

    @SuppressWarnings("unchecked")
    @Test
    public void fluxProducerConsumerTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 3000;
        final String FIRST_MESSAGE = "first message";
        final String SECOND_MESSAGE = "second message";
        final String TAG = "data";
        // Step 1 - user function create a Flux or pass the Flux object from a database
        //          (note that emitter will not start until FluxPublish begins publishing)
        Flux<String> source = Flux.create(emitter -> {
            emitter.next(FIRST_MESSAGE);
            emitter.next(SECOND_MESSAGE);
            emitter.complete();
        });
        // Step 2 - do data transformation
        Flux<Map<String, Object>> filtered = source.map(d -> Map.of(TAG, d));
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
                    bench.offer(false);
                }, () -> bench.offer(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(true, signal);
        assertEquals(2, messages.size());
        assertInstanceOf(Map.class, messages.get(0));
        assertInstanceOf(Map.class, messages.get(1));
        Object message1 = ((Map<String, Object>) messages.get(0)).get(TAG);
        Object message2 = ((Map<String, Object>) messages.get(1)).get(TAG);
        assertEquals(FIRST_MESSAGE, message1);
        assertEquals(SECOND_MESSAGE, message2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fluxProducerConsumerExceptionTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 3000;
        final String FIRST_MESSAGE = "first message";
        final String SECOND_MESSAGE = "second message";
        final String DEMO_EXCEPTION = "demo exception";
        final String TAG = "data";
        // Step 1 - user function create a Flux or pass the Flux object from a database
        //          (note that emitter will not start until FluxPublish begins publishing)
        Flux<String> source = Flux.create(emitter -> {
            emitter.next(FIRST_MESSAGE);
            emitter.next(SECOND_MESSAGE);
            emitter.error(new AppException(400, DEMO_EXCEPTION));
        });
        // Step 2 - do data transformation
        Flux<Map<String, Object>> filtered = source.map(d -> Map.of(TAG, d));
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
            bench.offer(false);
        }, () -> bench.offer(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(3, messages.size());
        assertInstanceOf(Map.class, messages.get(0));
        assertInstanceOf(Map.class, messages.get(1));
        assertInstanceOf(AppException.class, messages.get(2));
        Object message1 = ((Map<String, Object>) messages.get(0)).get(TAG);
        Object message2 = ((Map<String, Object>) messages.get(1)).get(TAG);
        var ex = (AppException) messages.get(2);
        assertEquals(FIRST_MESSAGE, message1);
        assertEquals(SECOND_MESSAGE, message2);
        assertEquals(400, ex.getStatus());
        assertEquals(DEMO_EXCEPTION, ex.getMessage());
    }

    @Test
    public void eventPublisherFluxConsumerCompatibilityTest() throws IOException, InterruptedException {
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
            bench.offer(false);
        }, () -> bench.offer(true));
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
    public void eventPublisherExpiryTest() throws IOException, InterruptedException {
        final long TIME_TO_LIVE = 1000;
        EventPublisher publisher = new EventPublisher(TIME_TO_LIVE);
        Thread.sleep(1100);
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final List<Object> messages = new ArrayList<>();
        FluxConsumer<String> fc = new FluxConsumer<>(publisher.getStreamId(), TIME_TO_LIVE);
        fc.consume(messages::add, e -> {
            messages.add(e);
            bench.offer(false);
        }, () -> bench.offer(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(1, messages.size());
        assertInstanceOf(AppException.class, messages.getFirst());
        AppException ex = (AppException) messages.getFirst();
        assertEquals(408, ex.getStatus());
        assertEquals("Event stream expired", ex.getMessage());
    }

    @Test
    public void fluxPublisherExpiryTest() throws IOException, InterruptedException {
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
            bench.offer(false);
        }, () -> bench.offer(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(1, messages.size());
        assertInstanceOf(AppException.class, messages.getFirst());
        AppException ex = (AppException) messages.getFirst();
        assertEquals(408, ex.getStatus());
        assertEquals("Event stream expired", ex.getMessage());
    }

    @Test
    public void expiryTest() throws IOException, InterruptedException {
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        String TEXT = "hello world";
        // The minimum timeout is one second if you set it to a smaller value
        ObjectStreamIO unused = new ObjectStreamIO(0);
        assertEquals(1, unused.getExpirySeconds());
        String unusedStream = unused.getInputStreamId().substring(0, unused.getInputStreamId().indexOf('@'));
        // create a stream with 5 second expiry
        ObjectStreamIO stream = new ObjectStreamIO(5);
        ObjectStreamWriter out = new ObjectStreamWriter(stream.getOutputStreamId());
        out.write(TEXT);
        Map<String, Object> info = ObjectStreamIO.getStreamInfo();
        assertNotNull(info.get("count"));
        int count = util.str2int(info.get("count").toString());
        assertTrue(count > 0);
        String id = stream.getInputStreamId().substring(0, stream.getInputStreamId().indexOf('@'));
        assertTrue(info.containsKey(id));
        AsyncObjectStreamReader in = new AsyncObjectStreamReader(stream.getInputStreamId(), 3000);
        in.getNext().onSuccess(data -> {
            assertEquals(TEXT, data);
            bench.offer(true);
        });
        bench.poll(10, TimeUnit.SECONDS);
        // The stream is intentionally left open
        Thread.sleep(1100);
        /*
         * The system will check expired streams every 30 seconds
         * To avoid waiting it in a unit test, we force it to remove expired streams
         */
        ObjectStreamIO.checkExpiredStreams();
        Map<String, Object> allStreams = ObjectStreamIO.getStreamInfo();
        // the unused stream has already expired
        assertFalse(allStreams.containsKey(unusedStream));
        log.info("{} has expired", unusedStream);
        // The stream has not yet expired
        assertTrue(allStreams.containsKey(id));
        log.info("{} is still active", id);
        // Sleep past the expiry
        Thread.sleep(4000);
        ObjectStreamIO.checkExpiredStreams();
        allStreams = ObjectStreamIO.getStreamInfo();
        // It should be gone at this point
        assertFalse(allStreams.containsKey(id));
    }

    @Test
    public void asyncReadWrite() throws IOException, InterruptedException {
        int CYCLES = 10;
        String TEXT = "hello world";
        /*
         * Producer creates a new stream with 60 seconds inactivity expiry
         */
        ObjectStreamIO stream = new ObjectStreamIO(1);
        log.info("Using {}", stream.getInputStreamId());
        // Closing an output stream will send an EOF signal. The try auto-close will do this.
        try (ObjectStreamWriter out = new ObjectStreamWriter(stream.getOutputStreamId())) {
            for (int i = 0; i < CYCLES; i++) {
                out.write(TEXT + " " + i);
            }
        }
        BlockingQueue<Integer> bench = new ArrayBlockingQueue<>(1);
        AsyncObjectStreamReader in = new AsyncObjectStreamReader(stream.getInputStreamId(), 8000);
        // AsyncObjectStreamReader is non-blocking. Therefore, we must use a blocking queue in a unit test.
        log.info("Beginning of Stream");
        fetchNextBlock(in, 0, bench);
        Integer count = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(count);
        assertEquals(CYCLES, count.intValue());
        assertTrue(in.isStreamEnd());
        assertTrue(in.isClosed());
    }

    private void fetchNextBlock(AsyncObjectStreamReader in, int count, BlockingQueue<Integer> bench) {
        Future<Object> block = in.getNext();
        block.onSuccess(b -> {
            if (b != null) {
                log.info("{}", b);
                fetchNextBlock(in, count+1, bench);
            } else {
                try {
                    in.close();
                    log.info("End of Stream");
                } catch (IOException e) {
                    log.error("Unable to close stream - {}", e.getMessage());
                } finally {
                    bench.offer(count);
                }
            }
        });
    }

}
