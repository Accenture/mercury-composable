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
import org.platformlambda.core.util.common.ConfigBase;
import org.platformlambda.core.system.Platform;
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
 *
 * <p>Schema Registry: by default ONE {@link SchemaCodec} - one registry identity, from
 * {@code schema-registry.properties} - serves both directions: {@code simple.kafka.notification} encodes and the
 * flow adapter decodes with it. Some Confluent installations grant a service's registry access per direction -
 * most visibly for CSFLE, where key (KEK) access comes through separate produce and consume identity pools - so
 * no single identity can decrypt everything the service consumes. Setting {@code schema.registry.consumer.properties}
 * gives the flow adapter its own codec, built under the {@code schema.registry.consumer} key prefix against the
 * same {@code schema.registry.url} (see {@link #resolveConsumerSchemaCodec}); the producer keeps
 * {@code schema.registry.*}. Unset or blank, nothing changes.</p>
 */
@MainApplication
public class KafkaFlowAutoStart implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowAutoStart.class);
    private static final String ADAPTER_CONFIG = "yaml.kafka.flow.adapter";
    // The flow's own ttl is the deadline for processing; only the dead-letter confirm-write needs a timeout.
    private static final String DLQ_TIMEOUT = "kafka.dlq.timeout.ms";
    private static final String MAX_RETRIES = "kafka.flow.max.retries";
    private static final String RETRY_BACKOFF = "kafka.flow.retry.backoff.ms";
    private static final String REGISTRY_URL = "schema.registry.url";
    /*
     * Opt-in, by presence (like ADAPTER_CONFIG): when this names a registry client template, the flow adapter
     * decodes with its own SchemaCodec built under CONSUMER_REGISTRY_PREFIX - the same prefix seam twin-kafka
     * uses for a second cluster's registry, applied here to one registry with two identities. Unset or blank
     * (the ${ENV_VAR:} idiom), the adapter shares the producer's codec, exactly as before.
     */
    static final String CONSUMER_REGISTRY_LOCATION = "schema.registry.consumer.properties";
    static final String CONSUMER_REGISTRY_PREFIX = "schema.registry.consumer";

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
         * owner-confined encoder/decoder from it, since the Confluent serdes are not thread-safe. The consume
         * side may instead get a codec of its own - see resolveConsumerSchemaCodec.
         */
        SchemaCodec producerSchemaCodec = SchemaCodec.fromConfig(config);
        KafkaRuntime.setSchemaCodec(producerSchemaCodec);
        SchemaCodec consumerSchemaCodec =
                resolveConsumerSchemaCodec(config, config.getProperty(REGISTRY_URL), producerSchemaCodec);

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
                    dlqTimeout, retryPolicy, consumerSchemaCodec);
            adapter.start();
            KafkaRuntime.setAdapter(adapter);
            log.info("Kafka flow adapter started from {}", adapterConfig);
        } else {
            log.info("{} not set; Kafka flow adapter not started", ADAPTER_CONFIG);
        }
        if (publisher != null || KafkaRuntime.adapter() != null) {
            // The platform owns ONE JVM shutdown hook and runs its callbacks in reverse registration order,
            // each error-isolated. Registering here - where the clients were opened - closes every binding's
            // consumer (LeaveGroup: partitions move at once instead of after the 45 s session timeout) and
            // then flushes the producer, on SIGTERM as on Ctrl-C. See KafkaRuntime.shutdown().
            Platform.getInstance().onShutdown(KafkaRuntime::shutdown);
        }
    }

    /**
     * The codec the flow adapter decodes with. It is the producer's own codec unless
     * {@code schema.registry.consumer.properties} names a registry client template - then a second codec is built
     * under the {@code schema.registry.consumer} prefix, so the consumer's registry identity can differ from the
     * producer's: same registry URL (a consumer decodes messages whose ids were minted by the registry its
     * producers use), that template (reuse the producer's file or point at a second one),
     * {@code schema.registry.consumer.serde.*} overrides on top of it, and its own caches
     * ({@code schema.registry.consumer.cache.ttl}).
     *
     * <p>Two consequences of the prefix seam worth knowing. A {@code schema.registry.consumer.serde.*} entry reaches
     * the Confluent deserializer's configuration - and through it the DEK-registry client CSFLE builds from that
     * configuration, where key access is decided - but not the codec's own schema-by-id lookups, which
     * authenticate with the template's identity; when the consume identity must cover those too, the template
     * itself carries it. And the consumer codec reads only its own prefix: a {@code schema.registry.serde.*} KMS
     * driver credential the producer needs must be repeated under {@code schema.registry.consumer.serde.*}.</p>
     *
     * <p>Package-private (like {@code SchemaCodec.extractSerdeConfig}) so the composition is unit-testable
     * without a full {@link #start} run.</p>
     *
     * @param config              the application configuration
     * @param registryUrl         the shared {@code schema.registry.url}; null/blank means schema features are off
     * @param producerSchemaCodec the producer's codec (null when schema features are off)
     * @return the producer's codec when the consumer location is unset or blank, otherwise the consumer's own
     *         codec - null, like the producer's, when {@code schema.registry.url} is not configured
     */
    static SchemaCodec resolveConsumerSchemaCodec(ConfigBase config, String registryUrl,
                                                  SchemaCodec producerSchemaCodec) {
        String location = config.getProperty(CONSUMER_REGISTRY_LOCATION);
        if (location == null || location.isBlank()) {
            return producerSchemaCodec;
        }
        SchemaCodec consumerSchemaCodec = SchemaCodec.fromConfig(config, registryUrl, CONSUMER_REGISTRY_PREFIX);
        if (consumerSchemaCodec == null) {
            log.warn("{} is set but {} is not - schema features stay off", CONSUMER_REGISTRY_LOCATION, REGISTRY_URL);
        } else {
            log.info("Kafka flow adapter decodes with its own Schema Registry identity ({}={})",
                    CONSUMER_REGISTRY_LOCATION, location);
        }
        return consumerSchemaCodec;
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
