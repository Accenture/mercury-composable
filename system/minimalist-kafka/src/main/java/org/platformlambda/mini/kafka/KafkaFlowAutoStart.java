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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Autoloads the Kafka building blocks at startup. It runs as a {@link MainApplication} - i.e. AFTER the
 * platform-core engine has registered every composable function, which the flow adapter routes messages
 * into (a {@code @BeforeApplication} would run too early, before the engine exists).
 *
 * <p>It builds the shared {@link KafkaRequestPublisher} singleton and, when {@code yaml.kafka.flow.adapter}
 * is configured, starts the {@link KafkaFlowAdapter} from that file. The Kafka client connection/security
 * settings come from the external {@code kafka-producer.properties} / {@code kafka-consumer.properties}
 * templates (see {@link KafkaClientConfig}) - not hard-coded - so any enterprise installation can be
 * configured without code changes.</p>
 *
 * <p>Flow-failure handling is tunable via {@code application.properties}:
 * {@code kafka.flow.max.retries} (default 3), {@code kafka.flow.retry.backoff.ms} (default 500), and
 * {@code kafka.dlq.timeout.ms} (default 10000), the confirm-write timeout for the dead-letter publish. The
 * DLQ topic itself is a per-binding {@code dlq-topic} in {@code kafka-flow-adapter.yaml} (see
 * {@link KafkaFlowAdapter}), not a global setting - a binding without one drops an exhausted message with a
 * logged {@code ERROR} instead of dead-lettering it. There is no flow-processing timeout knob: a flow's own
 * {@code ttl} is its deadline (Kafka is asynchronous, with no inherent request timeout).</p>
 */
@MainApplication
public class KafkaFlowAutoStart implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowAutoStart.class);
    private static final String ADAPTER_CONFIG = "yaml.kafka.flow.adapter";
    // The flow's own ttl is the deadline for processing; only the dead-letter confirm-write needs a timeout.
    private static final String DLQ_TIMEOUT = "kafka.dlq.timeout.ms";
    private static final String MAX_RETRIES = "kafka.flow.max.retries";
    private static final String RETRY_BACKOFF = "kafka.flow.retry.backoff.ms";

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        boolean producerEnabled = KafkaClientConfig.producerEnabled(config);
        boolean consumerEnabled = KafkaClientConfig.consumerEnabled(config);
        if (!producerEnabled && !consumerEnabled) {
            // a legitimate "Kafka off in this profile" switch - stated loudly because the module is inert
            log.warn("Kafka is inert - both {} and {} are false",
                    KafkaClientConfig.PRODUCER_ENABLED, KafkaClientConfig.CONSUMER_ENABLED);
        }
        KafkaRequestPublisher publisher = startPublisher(config, producerEnabled);

        /*
         * Optional Confluent Schema Registry codec (null when schema.registry.url is not configured). A shared
         * factory: simple.kafka.notification (produce) and the flow adapter (consume) each mint their own
         * owner-confined encoder/decoder from it, since the Confluent serdes are not thread-safe.
         */
        SchemaCodec schemaCodec = SchemaCodec.fromConfig(config);
        KafkaRuntime.setSchemaCodec(schemaCodec);

        String adapterConfig = config.getProperty(ADAPTER_CONFIG);
        if (!consumerEnabled) {
            log.info("{}=false; Kafka flow adapter not started", KafkaClientConfig.CONSUMER_ENABLED);
        } else if (adapterConfig != null) {
            // confirm-write timeout for the dead-letter publish (broker ack); the flow wait uses flow.ttl
            long dlqTimeout = Long.parseLong(config.getProperty(DLQ_TIMEOUT, "10000"));
            int maxRetries = Integer.parseInt(config.getProperty(MAX_RETRIES, "3"));
            long retryBackoffMs = Long.parseLong(config.getProperty(RETRY_BACKOFF, "500"));
            // failed messages are dead-lettered through the same shared producer, to each binding's dlq-topic
            RetryPolicy retryPolicy = new RetryPolicy(maxRetries, retryBackoffMs, publisher);
            ConfigReader adapterReader = new ConfigReader(adapterConfig);
            if (publisher == null) {
                // no producer to dead-letter through: a binding's dlq-topic would silently drop messages
                KafkaFlowAdapter.rejectDeadLetterWithoutProducer(adapterReader,
                        KafkaClientConfig.PRODUCER_ENABLED);
            }
            Properties consumerProps = KafkaClientConfig.consumerProperties(config);
            KafkaFlowAdapter adapter = new KafkaFlowAdapter(consumerProps, adapterReader,
                    dlqTimeout, retryPolicy, schemaCodec);
            adapter.start();
            KafkaRuntime.setAdapter(adapter);
            log.info("Kafka flow adapter started from {}", adapterConfig);
        } else {
            log.info("{} not set; Kafka flow adapter not started", ADAPTER_CONFIG);
        }
    }

    /**
     * Build the shared publisher, or none when the producer is switched off - the produce-side of a
     * one-way bridge leg, where this cluster grants no producer credentials. A null publisher leaves
     * {@code simple.kafka.notification} registered but unusable (it fails with a message naming the
     * flag) and makes dead-lettering impossible, which is why a {@code dlq-topic} is rejected above.
     */
    private static KafkaRequestPublisher startPublisher(AppConfigReader config, boolean producerEnabled) {
        if (!producerEnabled) {
            log.info("{}=false; Kafka producer not started - [{}] is unavailable",
                    KafkaClientConfig.PRODUCER_ENABLED, SimpleKafkaNotification.ROUTE);
            return null;
        }
        KafkaRequestPublisher publisher =
                new KafkaRequestPublisher(new KafkaProducer<>(KafkaClientConfig.producerProperties(config)));
        KafkaRuntime.setPublisher(publisher);
        return publisher;
    }
}
