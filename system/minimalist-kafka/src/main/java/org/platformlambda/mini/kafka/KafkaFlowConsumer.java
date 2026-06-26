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
 */
public class KafkaFlowConsumer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowConsumer.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final String ADAPTER_ROUTE = "kafka.flow.adapter";
    private static final String FLOW_ID = "flow_id";   // header key read by event.script.manager
    private static final String HEADER = "header";
    private static final String BODY = "body";

    private final Consumer<String, byte[]> consumer;
    private final String topic;
    private final String flowId;
    private final long flowTimeoutMs;
    private final ExecutorService loop;

    private volatile boolean running;

    public KafkaFlowConsumer(Consumer<String, byte[]> consumer, String topic, String flowId, long flowTimeoutMs) {
        this.consumer = consumer;
        this.topic = topic;
        this.flowId = flowId;
        this.flowTimeoutMs = flowTimeoutMs;
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
            consumer.subscribe(List.of(topic));
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
     * Route the record into the flow, blocking until it finishes (so the offset is committed only after
     * the message is processed).
     *
     * @return true if the offset may be committed (flow delivered/completed), false on interruption.
     */
    boolean routeToFlow(ConsumerRecord<String, byte[]> consumerRecord) {
        Map<String, Object> dataset = toDataset(consumerRecord);
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
        PostOffice po = PostOffice.trackable(ADAPTER_ROUTE, traceId, tracePath);
        try {
            po.request(forward, flowTimeoutMs).get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
            return false;   // do not commit a message that was interrupted mid-processing
        } catch (ExecutionException e) {
            // The message reached the flow; a flow-level failure is logged, not retried forever.
            log.warn("Flow {} did not complete for a '{}' message: {}", flowId, topic, e.getMessage());
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
