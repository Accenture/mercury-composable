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

package org.platformlambda.async;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.platformlambda.support.SyncOverAsyncHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Test double for the asynchronous System-of-Record: consumes from the request topic and emits a
 * response to the response topic, echoing the correlation-id and {@code traceparent} so the integration
 * test can prove they survive the full async round-trip.
 */
final class MockSystemOfRecord implements AutoCloseable {

    private final Consumer<String, byte[]> consumer;
    private final Producer<String, byte[]> producer;
    private final String requestTopic;
    private final String responseTopic;
    private final ExecutorService loop = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mock-system-of-record");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running;

    MockSystemOfRecord(String bootstrapServers, String requestTopic, String responseTopic) {
        this.consumer = KafkaTestSupport.newConsumer(bootstrapServers, "mock-system-of-record");
        this.producer = KafkaTestSupport.newProducer(bootstrapServers);
        this.requestTopic = requestTopic;
        this.responseTopic = responseTopic;
    }

    void start() {
        running = true;
        loop.submit(this::run);
    }

    private void run() {
        try {
            consumer.subscribe(List.of(requestTopic));
            while (running) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(300));
                for (ConsumerRecord<String, byte[]> request : records) {
                    respondTo(request);
                }
            }
        } catch (WakeupException e) {
            // expected on close()
        } finally {
            consumer.close();
            producer.close();
        }
    }

    private void respondTo(ConsumerRecord<String, byte[]> request) {
        String cid = header(request, SyncOverAsyncHeaders.CORRELATION_ID);
        if (cid == null) {
            cid = request.key();
        }
        String traceparent = header(request, SyncOverAsyncHeaders.TRACE_PARENT);
        String body = request.value() == null ? "" : new String(request.value(), StandardCharsets.UTF_8);
        String response = "{\"cid\":\"" + cid + "\",\"traceparent\":\"" + traceparent + "\",\"echo\":" + body + "}";

        ProducerRecord<String, byte[]> out =
                new ProducerRecord<>(responseTopic, cid, response.getBytes(StandardCharsets.UTF_8));
        out.headers().add(SyncOverAsyncHeaders.CORRELATION_ID, cid.getBytes(StandardCharsets.UTF_8));
        if (traceparent != null) {
            out.headers().add(SyncOverAsyncHeaders.TRACE_PARENT, traceparent.getBytes(StandardCharsets.UTF_8));
        }
        producer.send(out);
    }

    private static String header(ConsumerRecord<String, byte[]> consumerRecord, String name) {
        Header header = consumerRecord.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        running = false;
        consumer.wakeup();
        loop.shutdown();
        try {
            if (!loop.awaitTermination(10, TimeUnit.SECONDS)) {
                loop.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            loop.shutdownNow();
        }
    }
}
