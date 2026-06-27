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

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thread-safe wrapper around a Kafka producer, shared as a singleton by {@code
 * simple.kafka.notification}. Publishing is <b>drop-n-forget</b>: Kafka's commit-log journaling is the
 * durable, high-performance buffer, so the notification side does not block on broker acknowledgement
 * (the consumer side carries the at-least-once guarantee instead).
 *
 * <p>Drop-n-forget does <b>not</b> mean silent: a send-completion callback logs any asynchronous
 * delivery failure (e.g. broker unreachable past {@code delivery.timeout.ms}) so a dropped publish is
 * visible in the logs rather than masked, while the caller still returns immediately.</p>
 */
public class KafkaRequestPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaRequestPublisher.class);

    private final Producer<String, byte[]> producer;

    public KafkaRequestPublisher(Producer<String, byte[]> producer) {
        this.producer = producer;
    }

    /**
     * Publish a message and return immediately (drop-n-forget).
     *
     * @param topic destination topic (required).
     * @param partition target partition, or {@code null} to let Kafka's default partitioner choose.
     * @param headers Kafka record headers, already byte[]-encoded; may be {@code null}.
     * @param body message body.
     */
    public void publish(String topic, Integer partition, Map<String, byte[]> headers, byte[] body) {
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, partition, null, body);
        if (headers != null) {
            headers.forEach((key, value) -> producerRecord.headers().add(key, value));
        }
        // drop-n-forget, but surface async delivery failures instead of masking them
        producer.send(producerRecord, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to publish to topic {}: {}", topic, exception.getMessage());
            }
        });
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
