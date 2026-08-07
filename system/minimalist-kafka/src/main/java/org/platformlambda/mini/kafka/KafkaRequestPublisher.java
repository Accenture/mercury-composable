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
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thread-safe wrapper around a Kafka producer, shared as a singleton by {@code
 * simple.kafka.notification}. The record is sent <b>eagerly</b> (send is issued before the returned
 * {@link Mono} is subscribed), and the {@code Mono<Void>} completes when the broker acknowledges the
 * record or errors when the send fails. This lets a caller that cares - e.g. a synchronous REST facade -
 * await delivery and fail-fast on a publishing failure, while a drop-n-forget caller can simply ignore the
 * Mono (the record is still sent).
 *
 * <p>Delivery failures (e.g. broker unreachable past {@code delivery.timeout.ms}) are always logged via
 * {@code doOnError}, so a failed publishing is visible even when no subscriber observes the Mono.</p>
 */
public class KafkaRequestPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaRequestPublisher.class);

    private final Producer<String, byte[]> producer;

    public KafkaRequestPublisher(Producer<String, byte[]> producer) {
        this.producer = producer;
    }

    /**
     * Send a message eagerly and return a {@link Mono} that completes on broker acknowledgement (or errors
     * on a delivery failure). Send is issued immediately, so a caller that ignores the Mono still
     * publishes; a caller that subscribes (e.g. via the composable function machinery) observes success or
     * failure and can react - the basis for fail-fast on the synchronous request path.
     *
     * @param topic destination topic (required).
     * @param partition target partition, or {@code null} to let the configured partitioner choose
     *                  (by default {@link SimpleRandomPartitioner} - uniform random distribution).
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
     * Obtain partition metadata for a topic from the producer's own metadata view - no AdminClient
     * involved. The first call for a topic not yet in the client's metadata cache blocks up to
     * {@code max.block.ms} fetching it; subsequent calls are served from the cache, which the client
     * refreshes in the background (every {@code metadata.max.age.ms}).
     *
     * <p>Partition ids are contiguous {@code 0..size-1}, so the returned list's size bounds an
     * application-supplied partition number - e.g. a routing function validating an encoded partition
     * header before publishing with an explicit partition. Validating up front matters: an
     * out-of-range explicit partition does not fail fast on send - the producer blocks up to
     * {@code max.block.ms} waiting for that partition to appear in metadata, then errors. A stale
     * cache is conservative for this check, because a partition count only ever grows.</p>
     *
     * <p>Cautions: throws Kafka's unchecked {@link org.apache.kafka.common.errors.TimeoutException}
     * when metadata cannot be obtained within {@code max.block.ms} (e.g. the topic does not exist and
     * auto-creation is off); and on brokers with {@code auto.create.topics.enable=true}, requesting
     * metadata for a nonexistent topic may <b>create</b> it - mind topic-name typos in routing code.</p>
     *
     * @param topic name
     * @return partition metadata for the topic
     */
    public List<PartitionInfo> partitions(String topic) {
        return producer.partitionsFor(topic);
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
