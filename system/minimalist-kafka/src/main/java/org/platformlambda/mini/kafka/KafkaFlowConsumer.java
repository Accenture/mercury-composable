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

import com.accenture.automation.EventScriptManager;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.W3cTrace;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * One Kafka consumer thread for the {@link KafkaFlowAdapter}: it polls a topic and routes each message
 * into a configured Event Script flow - the same way {@code http.flow.adapter} routes an HTTP request
 * into a flow - so the flow's tasks (not this I/O layer) emit the telemetry traces.
 *
 * <p><b>Trace continuity.</b> The {@code traceparent} carried in the Kafka headers is parsed into its
 * trace-id and parent span-id; the request to the flow engine is sent with {@code setSpanId(parentSpanId)}
 * so the flow chains onto the upstream span (the receiver adopts the event's span-id as its parent). The
 * low-level {@link PostOffice} API is used directly rather than the FlowExecutor convenience methods,
 * which do not expose the inbound span-id.</p>
 *
 * <p><b>At-least-once delivery.</b> Offsets are committed manually, only AFTER the flow finishes
 * processing a message (the request blocks the poll loop), and {@code max.poll.records=1} processes one
 * message at a time. If the instance crashes before the commit (e.g. a Kubernetes rolling restart), the
 * offset stays uncommitted and Kafka redelivers the message to a surviving instance in the group - the
 * deliberate resilience-over-throughput tradeoff for the consume side.</p>
 *
 * <p><b>Flow-failure handling.</b> A flow-level failure is retried up to {@link RetryPolicy#maxRetries()}
 * times; if it still fails, the original message is routed to the per-topic dead-letter topic
 * ({@code <topic><dlqSuffix>}, suffix default {@code .dlq}). The DLQ write is <b>confirmed</b> (the broker
 * must acknowledge it) and the offset is committed <b>only if that write succeeds</b>; if the DLQ write
 * fails - e.g. the DLQ topic does not exist (Kafka auto-creation is off in production) - the offset is
 * <b>not</b> committed, so the message redelivers rather than being silently lost. Operationally this means
 * <b>DLQ topics must be pre-provisioned</b>; a missing one stalls the partition loudly instead of dropping
 * data, which is the correct trade-off for a holding area whose purpose is reprocessing.</p>
 *
 * <p><b>Partition pinning (opt-in).</b> When a {@code partition} is supplied for the binding, the consumer
 * <b>manually assigns</b> that one topic-partition ({@code assign}) instead of joining the consumer group
 * ({@code subscribe}). This bypasses group rebalancing - the pinned consumer reads exactly that partition -
 * so the operator owns the deployment model (e.g. one consumer per partition, or each pod pinning a distinct
 * partition via {@code partition: ${POD_PARTITION}}). Offsets still commit under the configured group id.</p>
 */
public class KafkaFlowConsumer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowConsumer.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final String ADAPTER_ROUTE = "kafka.flow.adapter";
    private static final String FLOW_ID = "flow_id";   // header key read by event.script.manager
    private static final String HEADER = "header";
    private static final String BODY = "body";
    private static final String DLQ_SUFFIX = ".dlq";
    private static final String DLQ_ERROR_HEADER = "dlq.error";
    private static final String DLQ_ORIGIN_TOPIC_HEADER = "dlq.origin.topic";

    private final Consumer<String, byte[]> consumer;
    private final String topic;
    private final String flowId;
    private final long flowTimeoutMs;
    private final RetryPolicy retryPolicy;
    private final Integer partition;   // null = group-managed subscribe; non-null = pin this partition
    private final SchemaCodec schemaCodec;   // null = raw byte[] body; non-null = decode Confluent-framed value
    private final String deadLetterTopic;
    private final ExecutorService loop;

    private volatile boolean running;

    public KafkaFlowConsumer(Consumer<String, byte[]> consumer, String topic, String flowId,
                             long flowTimeoutMs, RetryPolicy retryPolicy, Integer partition,
                             SchemaCodec schemaCodec) {
        this.consumer = consumer;
        this.topic = topic;
        this.flowId = flowId;
        this.flowTimeoutMs = flowTimeoutMs;
        this.retryPolicy = retryPolicy;
        this.partition = partition;
        this.schemaCodec = schemaCodec;
        // per-topic DLQ; a blank suffix falls back to .dlq so the DLQ can never equal the source topic
        String suffix = retryPolicy.dlqSuffix();
        this.deadLetterTopic = topic + (suffix == null || suffix.isBlank() ? DLQ_SUFFIX : suffix);
        this.loop = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kafka-flow-" + topic);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        running = true;
        loop.submit(this::pollLoop);
    }

    private void pollLoop() {
        try {
            subscribeOrAssign(consumer);
            while (running) {
                ConsumerRecords<String, byte[]> records = consumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<String, byte[]> consumerRecord : records) {
                    if (routeToFlow(consumerRecord)) {
                        commit(consumerRecord);   // commit only after the flow finished -> at-least-once
                    }
                }
            }
        } catch (WakeupException e) {
            // expected: close() called wakeup() to break the poll
        } catch (RuntimeException e) {
            log.error("Kafka flow consumer for topic {} stopped unexpectedly", topic, e);
        } finally {
            consumer.close();
        }
    }

    /**
     * Group-managed {@code subscribe} by default; manual {@code assign} of the single pinned topic-partition
     * when a {@code partition} was configured. Visible for testing.
     */
    void subscribeOrAssign(Consumer<String, byte[]> consumer) {
        if (partition != null) {
            consumer.assign(List.of(new TopicPartition(topic, partition)));
        } else {
            consumer.subscribe(List.of(topic));
        }
    }

    /**
     * Route the record into the flow, blocking until it finishes (so the offset is committed only after
     * the message is processed). On flow failure it retries per the {@link RetryPolicy}, then dead-letters
     * the message before allowing the commit.
     *
     * @return true if the offset may be committed (flow completed, or message durably dead-lettered),
     *         false if it must NOT be committed (interrupted, or the dead-letter write failed) so the
     *         message redelivers rather than being skipped.
     */
    boolean routeToFlow(ConsumerRecord<String, byte[]> consumerRecord) {
        Map<String, Object> dataset = toDataset(consumerRecord);
        if (schemaCodec != null) {
            // Decode the Confluent-framed value to a Map for the flow. A decode failure is a poison
            // message (retrying won't help), so dead-letter the RAW record immediately.
            try {
                dataset.put(BODY, schemaCodec.decode(topic, consumerRecord.value()));
            } catch (RuntimeException e) {
                log.warn("Failed to decode schema-framed message on '{}'; routing to {}",
                        topic, deadLetterTopic, e);
                return writeToDeadLetter(consumerRecord, e);
            }
        }
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) dataset.get(HEADER);
        String[] trace = W3cTrace.parse(headers.get(W3cTrace.TRACEPARENT));
        String traceId = trace != null ? trace[0] : Utility.getInstance().getUuid();
        String parentSpanId = trace != null ? trace[1] : null;
        String tracePath = "KAFKA /" + topic;
        String correlationId = headers.getOrDefault(KafkaHeaders.CORRELATION_ID, traceId);

        EventEnvelope forward = new EventEnvelope();
        forward.setTo(EventScriptManager.SERVICE_NAME).setHeader(FLOW_ID, flowId)
                .setCorrelationId(correlationId).setBody(dataset)
                .setTraceId(traceId).setTracePath(tracePath);
        if (parentSpanId != null) {
            // The flow chains onto the upstream span carried in the Kafka traceparent.
            forward.setSpanId(parentSpanId);
        }
        int attempt = 0;
        while (true) {
            try {
                invokeFlow(forward, traceId, tracePath);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                return false;   // do not commit a message that was interrupted mid-processing
            } catch (ExecutionException e) {
                if (attempt >= retryPolicy.maxRetries()) {
                    log.warn("Flow {} failed for a '{}' message after {} attempt(s); routing to {}",
                            flowId, topic, attempt + 1, deadLetterTopic, e.getCause());
                    // commit only if the message is durably dead-lettered; otherwise redeliver (no loss)
                    return writeToDeadLetter(consumerRecord, e.getCause());
                }
                attempt++;
                log.warn("Flow {} failed for a '{}' message (attempt {}/{}); retrying",
                        flowId, topic, attempt, retryPolicy.maxRetries(), e.getCause());
                if (!backoff()) {
                    return false;   // interrupted during backoff -> redeliver, do not commit
                }
            }
        }
    }

    /**
     * Send the event into the flow engine, blocking until it completes. Visible (package-private and
     * non-final) so the retry/dead-letter orchestration can be unit-tested without a running engine.
     */
    void invokeFlow(EventEnvelope forward, String traceId, String tracePath)
            throws InterruptedException, ExecutionException {
        PostOffice po = PostOffice.trackable(ADAPTER_ROUTE, traceId, tracePath);
        po.request(forward, flowTimeoutMs).get();
    }

    /** Pause between retry attempts. @return false if interrupted while waiting (caller must not commit). */
    private boolean backoff() {
        if (retryPolicy.backoffMs() <= 0) {
            return true;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(retryPolicy.backoffMs());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
            return false;
        }
    }

    /**
     * Park an un-processable message on {@code <topic><dlqSuffix>} with a <b>confirmed</b> write, preserving
     * its headers + body. Visible (package-private, non-final) so the commit-gating can be unit-tested.
     *
     * @return true if the broker acknowledged the dead-letter write (safe to commit); false if it could not
     *         be stored (no publisher, write failed, or interrupted) so the caller must not commit.
     */
    boolean writeToDeadLetter(ConsumerRecord<String, byte[]> consumerRecord, Throwable cause) {
        KafkaRequestPublisher publisher = retryPolicy.deadLetterPublisher();
        if (publisher == null) {
            log.error("No dead-letter publisher; NOT committing '{}' offset {} (will redeliver)",
                    topic, consumerRecord.offset());
            return false;
        }
        Map<String, byte[]> deadLetterHeaders = new HashMap<>();
        consumerRecord.headers().forEach(h -> deadLetterHeaders.put(h.key(), h.value()));
        deadLetterHeaders.put(DLQ_ORIGIN_TOPIC_HEADER, topic.getBytes(StandardCharsets.UTF_8));
        deadLetterHeaders.put(DLQ_ERROR_HEADER,
                (cause != null ? cause.toString() : "unknown").getBytes(StandardCharsets.UTF_8));
        try {
            publisher.publishSync(deadLetterTopic, null, deadLetterHeaders, consumerRecord.value(), flowTimeoutMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
            return false;
        } catch (ExecutionException | TimeoutException e) {
            log.error("Dead-letter write to {} failed; NOT committing '{}' offset {} (will redeliver). "
                            + "Ensure the DLQ topic exists (Kafka auto-creation is off in production): {}",
                    deadLetterTopic, topic, consumerRecord.offset(), e.getMessage());
            return false;
        }
    }

    /** Decode a Kafka record into a flow dataset ({@code header} + {@code body}). Visible for testing. */
    static Map<String, Object> toDataset(ConsumerRecord<String, byte[]> consumerRecord) {
        Map<String, String> headers = new HashMap<>();
        consumerRecord.headers().forEach(h -> headers.put(h.key(), new String(h.value(), StandardCharsets.UTF_8)));
        Map<String, Object> dataset = new HashMap<>();
        dataset.put(HEADER, headers);
        dataset.put(BODY, consumerRecord.value());
        return dataset;
    }

    private void commit(ConsumerRecord<String, byte[]> consumerRecord) {
        consumer.commitSync(Map.of(
                new TopicPartition(consumerRecord.topic(), consumerRecord.partition()),
                new OffsetAndMetadata(consumerRecord.offset() + 1)));
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
