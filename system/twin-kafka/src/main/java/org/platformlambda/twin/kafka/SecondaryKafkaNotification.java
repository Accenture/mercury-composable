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

package org.platformlambda.twin.kafka;

import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.mini.kafka.KafkaRequestPublisher;
import org.platformlambda.mini.kafka.SimpleKafkaNotification;
import org.platformlambda.mini.kafka.schema.SchemaCodec;

/**
 * The SECONDARY-cluster counterpart of {@code simple.kafka.notification}: identical routing, header
 * propagation, trace stamping, and optional subject-driven Confluent serialization - publishing to
 * the SECOND Kafka cluster through {@link SecondaryKafkaRuntime}. A flow task bridges two clusters by
 * consuming from one adapter and publishing through the other cluster's notification function.
 *
 * <p>Outbound header names may be tuned per cluster: {@code secondary.kafka.correlation.id.header}
 * and {@code secondary.kafka.trace.id.header} override the shared globals
 * ({@code kafka.correlation.id.header}, default {@code cid} / {@code kafka.trace.id.header}, unset)
 * when the two clusters follow different conventions.</p>
 *
 * <p>Same threading model as the base class: {@code @KernelThreadRunner} with a small single-flight
 * worker pool (see {@link SimpleKafkaNotification} for the full rationale).</p>
 */
@KernelThreadRunner
@PreLoad(route = "secondary.kafka.notification", instances = 5)
public class SecondaryKafkaNotification extends SimpleKafkaNotification {

    // Outbound header names for the secondary cluster: cluster-specific keys, falling back to the globals.
    private static final String CORRELATION_ID_HEADER =
            secondaryOrGlobal("secondary.kafka.correlation.id.header", "kafka.correlation.id.header", "cid");
    private static final String TRACE_ID_HEADER =
            secondaryOrGlobal("secondary.kafka.trace.id.header", "kafka.trace.id.header", null);

    /** The secondary-cluster key when set, else the global key, else the hard default (may be null). */
    private static String secondaryOrGlobal(String secondaryKey, String globalKey, String hardDefault) {
        AppConfigReader config = AppConfigReader.getInstance();
        String value = config.getProperty(secondaryKey);
        if (value == null) {
            value = config.getProperty(globalKey);
        }
        return value != null ? value : hardDefault;
    }

    @Override
    protected KafkaRequestPublisher publisher() {
        return SecondaryKafkaRuntime.publisher();
    }

    @Override
    protected SchemaCodec schemaCodec() {
        return SecondaryKafkaRuntime.schemaCodec();
    }

    @Override
    protected String correlationIdHeader() {
        return CORRELATION_ID_HEADER;
    }

    @Override
    protected String traceIdHeader() {
        return TRACE_ID_HEADER;
    }

    @Override
    protected String registryUrlKey() {
        return "secondary.schema.registry.url";
    }
}
