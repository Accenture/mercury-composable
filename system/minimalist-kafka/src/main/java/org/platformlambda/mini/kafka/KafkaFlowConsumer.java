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
import com.accenture.models.Flows;
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
 * <p><b>Flow outcome.</b> A flow <b>succeeds</b> when it replies with a status below 400 (a 2xx/3xx); any
 * 4xx/5xx status - or a thrown exception, including a timeout when it does not reply within its {@code ttl}
 * (the flow's {@code ttl} is the deadline, since Kafka has no inherent request timeout) - is a <b>failure</b>.</p>
 *
 * <p><b>Flow-failure handling.</b> A failure is retried up to {@link RetryPolicy#maxRetries()} times; if it
 * still fails, the original message is routed to the per-topic dead-letter topic ({@code <topic><dlqSuffix>},
 * suffix default {@code .dlq}) with a <b>confirmed</b> write. <b>DLQ topics must be pre-provisioned</b> (Kafka
 * auto-creation is off in production). If the DLQ write itself fails, the message is dropped with a loud
 * {@code ERROR} and the offset committed - see {@link #writeToDeadLetter} for why (avoiding a recovery storm),
 * the resulting data-loss caveat, and the planned alternative-path improvement.</p>
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
    private final long dlqTimeout;   // confirm-write timeout for the dead-letter publish (broker ack)
    private final RetryPolicy retryPolicy;
    private final Integer partition;   // null = group-managed subscribe; non-null = pin this partition
    // null = raw byte[] body; non-null = decode Confluent-framed value. Owned by this consumer's single poll
    // thread (the Confluent deserializers are not thread-safe), minted from the shared SchemaCodec factory.
    private final SchemaCodec.Decoder decoder;
    private final String deadLetterTopic;
    private final ExecutorService loop;

    private volatile boolean running;

    public KafkaFlowConsumer(Consumer<String, byte[]> consumer, String topic, String flowId,
                             long dlqTimeout, RetryPolicy retryPolicy, Integer partition,
                             SchemaCodec schemaCodec) {
        this.consumer = consumer;
        this.topic = topic;
        this.flowId = flowId;
        this.dlqTimeout = dlqTimeout;
        this.retryPolicy = retryPolicy;
        this.partition = partition;
        this.decoder = schemaCodec == null ? null : schemaCodec.newDecoder();
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
        if (decoder != null) {
            // Decode the Confluent-framed value to a Map for the flow. A decode failure is a poison
            // message (retrying won't help), so dead-letter the RAW record immediately.
            try {
                dataset.put(BODY, decoder.decode(topic, consumerRecord.value()));
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
        String tracePath = "KAFKA /" + topic;
        EventEnvelope forward = toFlowRequest(dataset, headers, trace, traceId, tracePath);
        return deliver(consumerRecord, forward, traceId, tracePath);
    }

    /** Build the flow-engine request from the decoded dataset, chaining onto the inbound trace/span. */
    private EventEnvelope toFlowRequest(Map<String, Object> dataset, Map<String, String> headers,
                                        String[] trace, String traceId, String tracePath) {
        String correlationId = headers.getOrDefault(KafkaHeaders.CORRELATION_ID, traceId);
        EventEnvelope forward = new EventEnvelope();
        forward.setTo(EventScriptManager.SERVICE_NAME).setHeader(FLOW_ID, flowId)
                .setCorrelationId(correlationId).setBody(dataset)
                .setTraceId(traceId).setTracePath(tracePath);
        if (trace != null) {
            forward.setSpanId(trace[1]);   // chain onto the upstream span carried in the Kafka traceparent
        }
        return forward;
    }

    /**
     * Invoke the flow with bounded retry, then dead-letter. A failure is a 4xx/5xx reply or a thrown
     * exception (flow/transport error, or a timeout when the flow does not reply within its ttl).
     *
     * @return whether the offset may be committed (see {@link #writeToDeadLetter}); false only on shutdown.
     */
    private boolean deliver(ConsumerRecord<String, byte[]> consumerRecord, EventEnvelope forward,
                            String traceId, String tracePath) {
        int attempt = 0;
        while (true) {
            Throwable cause;
            try {
                EventEnvelope response = invokeFlow(forward, traceId, tracePath);
                if (response.getStatus() < 400) {
                    return true;   // the flow finished normally (2xx/3xx) -> acknowledge (commit) and move on
                }
                cause = new IllegalStateException("flow " + flowId + " returned status " + response.getStatus());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                return false;   // do not commit a message that was interrupted mid-processing
            } catch (ExecutionException e) {
                cause = e.getCause() != null ? e.getCause() : e;
            }
            if (attempt >= retryPolicy.maxRetries()) {
                log.warn("Flow {} failed for a '{}' message after {} attempt(s); routing to {}",
                        flowId, topic, attempt + 1, deadLetterTopic, cause);
                return writeToDeadLetter(consumerRecord, cause);
            }
            attempt++;
            log.warn("Flow {} failed for a '{}' message (attempt {}/{}); retrying",
                    flowId, topic, attempt, retryPolicy.maxRetries(), cause);
            if (!backoff()) {
                return false;   // interrupted during backoff -> redeliver, do not commit
            }
        }
    }

    /**
     * Send the event into the flow engine, blocking until it replies, and return that reply. Visible
     * (package-private and non-final) so the retry/dead-letter orchestration can be unit-tested without a
     * running engine.
     *
     * <p>The deadline is the flow's own {@code ttl} - Kafka is asynchronous, so unlike an HTTP entry it has
     * no inherent request timeout; the flow's {@code ttl} is the authoritative deadline. If the flow does
     * not reply within {@code ttl}, {@code po.request} throws (the 2-arg overload uses timeout-as-exception
     * semantics), which the caller treats as a failure.</p>
     */
    EventEnvelope invokeFlow(EventEnvelope forward, String traceId, String tracePath)
            throws InterruptedException, ExecutionException {
        PostOffice po = PostOffice.trackable(ADAPTER_ROUTE, traceId, tracePath);
        long ttl = Flows.getFlow(flowId).ttl;
        return po.request(forward, ttl).get();
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
     * <p>The DLQ is the last line of defense. If the write to it <b>fails</b>, there is no further fallback -
     * an "exception of an exception". Refusing to commit would redeliver the same poison message, which would
     * fail the flow and the DLQ write again, indefinitely - a self-sustaining retry/recovery storm, a known
     * cause of prolonged outages. So instead we log a loud {@code ERROR} and commit, deliberately accepting
     * the loss of this one message in exchange for partition liveness.</p>
     *
     * <p><b>Data loss:</b> when the DLQ write fails, the message is <b>dropped</b> - only the ERROR log
     * records it. A future improvement is the classic resilience <i>alternative path</i>: on DLQ failure,
     * persist the record to a durable store (file/object store/DB) for later replay, rather than dropping it.</p>
     *
     * @return true once the offset may be committed - either the message was durably dead-lettered, or it
     *         could not be and was dropped-with-an-ERROR to avoid a recovery storm. Returns false only on a
     *         shutdown interruption, so the in-flight message redelivers cleanly on the next start.
     */
    // S2095: the publisher is the process-wide shared singleton owned by KafkaRuntime, NOT a resource this
    // method opens - closing it here (try-with-resources) would tear down the shared producer for everyone.
    @SuppressWarnings("java:S2095")
    boolean writeToDeadLetter(ConsumerRecord<String, byte[]> consumerRecord, Throwable cause) {
        KafkaRequestPublisher publisher = retryPolicy.deadLetterPublisher();
        if (publisher == null) {
            log.error("DATA LOSS: no dead-letter publisher; dropping '{}' offset {} after flow failure "
                    + "(configure a DLQ to retain failed messages)", topic, consumerRecord.offset(), cause);
            return true;
        }
        Map<String, byte[]> deadLetterHeaders = new HashMap<>();
        consumerRecord.headers().forEach(h -> deadLetterHeaders.put(h.key(), h.value()));
        deadLetterHeaders.put(DLQ_ORIGIN_TOPIC_HEADER, topic.getBytes(StandardCharsets.UTF_8));
        deadLetterHeaders.put(DLQ_ERROR_HEADER,
                (cause != null ? cause.toString() : "unknown").getBytes(StandardCharsets.UTF_8));
        try {
            publisher.publishSync(deadLetterTopic, null, deadLetterHeaders, consumerRecord.value(), dlqTimeout);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
            return false;   // shutdown in progress: redeliver on the next start, do not commit
        } catch (ExecutionException | TimeoutException e) {
            // an "exception of an exception": the last line of defense failed. Drop loudly rather than
            // block the partition retrying forever (a recovery storm). Future: an alternative-path store.
            log.error("DATA LOSS: dead-letter write to {} failed; dropping '{}' offset {} to avoid a "
                            + "redelivery storm (ensure the DLQ topic exists). Cause: {}",
                    deadLetterTopic, topic, consumerRecord.offset(), e.getMessage(), cause);
            return true;
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
