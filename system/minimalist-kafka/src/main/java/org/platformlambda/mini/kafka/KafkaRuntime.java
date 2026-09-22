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

import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Holds the process-wide singletons the minimalist Kafka building blocks share: the thread-safe
 * {@link KafkaRequestPublisher} used by {@code simple.kafka.notification}, the running
 * {@link KafkaFlowAdapter}, and (when {@code schema.registry.url} is configured) the {@link SchemaCodec}
 * for Confluent Schema Registry (de)serialization. Populated once at startup by {@link KafkaFlowAutoStart}.
 */
public final class KafkaRuntime {
    private static final Logger log = LoggerFactory.getLogger(KafkaRuntime.class);
    // a ReentrantLock, not synchronized: a virtual thread blocking in a synchronized block pins its carrier on Java 21
    private static final ReentrantLock SHUTDOWN_LOCK = new ReentrantLock();

    private static KafkaRequestPublisher publisher;
    private static KafkaFlowAdapter adapter;
    private static SchemaCodec schemaCodec;

    private KafkaRuntime() {}

    static void setPublisher(KafkaRequestPublisher instance) {
        publisher = instance;
    }

    public static KafkaRequestPublisher publisher() {
        return publisher;
    }

    static void setAdapter(KafkaFlowAdapter instance) {
        adapter = instance;
    }

    public static KafkaFlowAdapter adapter() {
        return adapter;
    }

    static void setSchemaCodec(SchemaCodec instance) {
        schemaCodec = instance;
    }

    /** The schema codec, or {@code null} when {@code schema.registry.url} is not configured. */
    public static SchemaCodec schemaCodec() {
        return schemaCodec;
    }

    /**
     * Release the Kafka clients in the right order - the flow adapter's consumers first, then the shared
     * producer - and forget them, so a late caller sees "not started" rather than a closed client.
     * <p>
     * Registered on {@code Platform.onShutdown} by the module's auto-start, so a {@code SIGTERM} (a Kubernetes
     * rolling restart) closes every binding's consumer while the JVM is still up. That close is what sends the
     * group coordinator a <b>LeaveGroup</b>: the member's partitions are reassigned to the surviving members at
     * once. Without it the broker only notices the dead member when its session expires - 45 seconds by default
     * under the KIP-848 consumer protocol - and every partition it held sits unread for that long (observed live
     * 2026-09-22: the Rust port's rdkafka consumers left cleanly while this engine's were fenced by session
     * expiry). Closing the producer afterwards flushes any buffered records.
     * <p>
     * Idempotent and safe to call when nothing was started; each close is isolated so one failing client
     * cannot keep the other open.
     */
    public static void shutdown() {
        SHUTDOWN_LOCK.lock();
        try {
            KafkaFlowAdapter runningAdapter = adapter;
            adapter = null;
            if (runningAdapter != null) {
                try {
                    runningAdapter.close();
                    log.info("Kafka flow adapter closed - its consumers have left their groups");
                } catch (RuntimeException e) {
                    log.warn("Kafka flow adapter did not close cleanly - {}", e.toString());
                }
            }
            KafkaRequestPublisher runningPublisher = publisher;
            publisher = null;
            if (runningPublisher != null) {
                try {
                    runningPublisher.close();
                    log.info("Kafka producer closed");
                } catch (RuntimeException e) {
                    log.warn("Kafka producer did not close cleanly - {}", e.toString());
                }
            }
        } finally {
            SHUTDOWN_LOCK.unlock();
        }
    }
}
