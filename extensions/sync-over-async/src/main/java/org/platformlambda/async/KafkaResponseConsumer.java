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
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.platformlambda.support.ResponseDelivery;
import org.platformlambda.support.SyncOverAsyncHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Inbound leg ("return adapter"): a single-threaded poll loop on the Kafka response topic. For each
 * record it extracts the correlation-id and payload and hands them to a {@link ResponseDelivery} - in
 * production {@code ReturnRouteCoordinator::deliver}, which forwards the response to whichever pod is
 * holding the original HTTP connection.
 *
 * <p>A Kafka consumer is not thread-safe, so it is owned by a single daemon thread; {@link #close()}
 * uses {@link Consumer#wakeup()} to break the poll and shut the loop down cleanly.</p>
 */
public class KafkaResponseConsumer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaResponseConsumer.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);

    private final Consumer<String, byte[]> consumer;
    private final String responseTopic;
    private final ResponseDelivery delivery;
    private final ExecutorService loop = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "soa-response-consumer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running;

    public KafkaResponseConsumer(Consumer<String, byte[]> consumer, String responseTopic, ResponseDelivery delivery) {
        this.consumer = consumer;
        this.responseTopic = responseTopic;
        this.delivery = delivery;
    }

    public void start() {
        running = true;
        loop.submit(this::pollLoop);
    }

    private void pollLoop() {
        try {
            consumer.subscribe(List.of(responseTopic));
            while (running) {
                ConsumerRecords<String, byte[]> records = consumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<String, byte[]> consumerRecord : records) {
                    handleRecord(consumerRecord);
                }
            }
        } catch (WakeupException e) {
            // expected: close() called wakeup() to break the poll
        } catch (RuntimeException e) {
            log.error("Response consumer loop stopped unexpectedly", e);
        } finally {
            consumer.close();
        }
    }

    /**
     * Decode one response record and route it. The correlation-id comes from the {@code cid} header,
     * falling back to the record key. Visible for unit testing.
     */
    void handleRecord(ConsumerRecord<String, byte[]> consumerRecord) {
        String correlationId = correlationIdHeader(consumerRecord);
        if (correlationId == null) {
            correlationId = consumerRecord.key();
        }
        if (correlationId == null) {
            log.warn("Dropping response without a correlation-id (offset={})", consumerRecord.offset());
            return;
        }
        String payload = consumerRecord.value() == null ? null
                : new String(consumerRecord.value(), StandardCharsets.UTF_8);
        if (!delivery.deliver(correlationId, payload)) {
            log.info("Orphan response {} - no caller waiting on this pod", correlationId);
        }
    }

    private static String correlationIdHeader(ConsumerRecord<String, byte[]> consumerRecord) {
        Header header = consumerRecord.headers().lastHeader(SyncOverAsyncHeaders.CORRELATION_ID);
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
