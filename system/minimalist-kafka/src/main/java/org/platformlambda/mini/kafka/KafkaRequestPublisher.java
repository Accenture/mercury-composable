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

package org.platformlambda.mini.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thread-safe wrapper around a Kafka producer, shared as a singleton by {@code
 * simple.kafka.notification}. The record is sent <b>eagerly</b> (the send is issued before the returned
 * {@link Mono} is subscribed), and the {@code Mono<Void>} completes when the broker acknowledges the
 * record or errors when the send fails. This lets a caller that cares - e.g. a synchronous REST facade -
 * await delivery and fail-fast on a publish failure, while a drop-n-forget caller can simply ignore the
 * Mono (the record is still sent).
 *
 * <p>Delivery failures (e.g. broker unreachable past {@code delivery.timeout.ms}) are always logged via
 * {@code doOnError}, so a failed publish is visible even when no subscriber observes the Mono.</p>
 */
public class KafkaRequestPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaRequestPublisher.class);

    private final Producer<String, byte[]> producer;

    public KafkaRequestPublisher(Producer<String, byte[]> producer) {
        this.producer = producer;
    }

    /**
     * Send a message eagerly and return a {@link Mono} that completes on broker acknowledgement (or errors
     * on a delivery failure). The send is issued immediately, so a caller that ignores the Mono still
     * publishes; a caller that subscribes (e.g. via the composable function machinery) observes success or
     * failure and can react - the basis for fail-fast on the synchronous request path.
     *
     * @param topic destination topic (required).
     * @param partition target partition, or {@code null} to let Kafka's default partitioner choose.
     * @param headers Kafka record headers, already byte[]-encoded; may be {@code null}.
     * @param body message body.
     * @return a {@code Mono<Void>} that completes when the broker acknowledges, or errors on failure.
     */
    public Mono<Void> publish(String topic, Integer partition, Map<String, byte[]> headers, byte[] body) {
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, partition, null, body);
        if (headers != null) {
            headers.forEach((key, value) -> producerRecord.headers().add(key, value));
        }
        // send eagerly; bridge the delivery callback to a future the Mono observes
        CompletableFuture<Void> ack = new CompletableFuture<>();
        producer.send(producerRecord, (metadata, exception) -> {
            if (exception != null) {
                ack.completeExceptionally(exception);
            } else {
                ack.complete(null);
            }
        });
        return Mono.fromFuture(ack)
                .doOnError(e -> log.error("Failed to publish to topic {}: {}", topic, e.getMessage()));
    }

    /**
     * Publish a message and block until the broker acknowledges it (or the timeout elapses). Unlike
     * {@link #publish}, this confirms durability and <b>throws on failure</b> - used for the dead-letter
     * path, where the caller must know the message is safely stored before committing the consumer offset.
     *
     * @throws ExecutionException if the broker rejected the record (e.g. the topic does not exist)
     * @throws TimeoutException   if no acknowledgement arrived within {@code timeoutMs}
     */
    public void publishSync(String topic, Integer partition, Map<String, byte[]> headers, byte[] body,
                            long timeoutMs) throws ExecutionException, InterruptedException, TimeoutException {
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, partition, null, body);
        if (headers != null) {
            headers.forEach((key, value) -> producerRecord.headers().add(key, value));
        }
        producer.send(producerRecord).get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        producer.close();
    }
}
