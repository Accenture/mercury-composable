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

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.platformlambda.support.SyncOverAsyncHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Outbound leg: publishes a request to the Kafka request topic, keyed by correlation-id and carrying the
 * {@code cid} and {@code traceparent} headers so the asynchronous backend can echo them on the response
 * topic. The send is <b>confirmed</b> (blocking with a timeout) so the caller knows the request is
 * durably accepted before it begins awaiting the response - a publish failure must surface as an error,
 * not a silent timeout later.
 */
public class KafkaRequestPublisher implements AutoCloseable {

    private static final Duration DEFAULT_SEND_TIMEOUT = Duration.ofSeconds(10);

    private final Producer<String, byte[]> producer;
    private final String requestTopic;
    private final Duration sendTimeout;

    public KafkaRequestPublisher(Producer<String, byte[]> producer, String requestTopic) {
        this(producer, requestTopic, DEFAULT_SEND_TIMEOUT);
    }

    public KafkaRequestPublisher(Producer<String, byte[]> producer, String requestTopic, Duration sendTimeout) {
        this.producer = producer;
        this.requestTopic = requestTopic;
        this.sendTimeout = sendTimeout;
    }

    /**
     * Publish a request and block until the broker acknowledges it.
     *
     * @param correlationId partition key and {@code cid} header (must not be {@code null}).
     * @param traceparent W3C trace context to propagate; omitted when {@code null}/blank.
     * @param payload request body.
     * @throws IllegalStateException if the send fails or times out.
     */
    public void publish(String correlationId, String traceparent, byte[] payload) {
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(requestTopic, correlationId, payload);
        producerRecord.headers().add(SyncOverAsyncHeaders.CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
        if (traceparent != null && !traceparent.isBlank()) {
            producerRecord.headers().add(SyncOverAsyncHeaders.TRACE_PARENT, traceparent.getBytes(StandardCharsets.UTF_8));
        }
        try {
            producer.send(producerRecord).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing request " + correlationId, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Unable to publish request " + correlationId, e);
        }
    }

    @Override
    public void close() {
        producer.close();
    }
}
