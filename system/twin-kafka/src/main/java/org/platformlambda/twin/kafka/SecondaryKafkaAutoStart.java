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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.mini.kafka.KafkaClientConfig;
import org.platformlambda.mini.kafka.KafkaFlowAdapter;
import org.platformlambda.mini.kafka.KafkaRequestPublisher;
import org.platformlambda.mini.kafka.RetryPolicy;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Starts the SECONDARY Kafka cluster connectivity, mirroring minimalist-kafka's
 * {@code KafkaFlowAutoStart} with the {@code secondary-*} configuration surface:
 *
 * <ul>
 *   <li>publisher from the {@code secondary-kafka-producer.properties} template
 *       (location overridable with {@code secondary.kafka.producer.properties})</li>
 *   <li>optional schema codec when {@code secondary.schema.registry.url} is set - the Schema Registry
 *       is per-cluster and OPTIONAL, so an on-prem Apache Kafka without a registry can bridge to a
 *       cloud Confluent cluster with one (or vice versa)</li>
 *   <li>optional flow adapter when {@code yaml.secondary.kafka.flow.adapter} names a binding file -
 *       consumer config from the {@code secondary-kafka-consumer.properties} template</li>
 * </ul>
 *
 * <p>The retry/dead-letter tuning keys ({@code kafka.dlq.timeout.ms}, {@code kafka.flow.max.retries},
 * {@code kafka.flow.retry.backoff.ms}) are application-level policy shared with the primary adapter.
 * The secondary adapter's dead letters are written through the SECONDARY publisher, so they land on
 * the secondary cluster where the failed message originated.</p>
 */
@MainApplication
public class SecondaryKafkaAutoStart implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(SecondaryKafkaAutoStart.class);
    private static final String ADAPTER_CONFIG = "yaml.secondary.kafka.flow.adapter";
    private static final String PRODUCER_LOCATION = "secondary.kafka.producer.properties";
    private static final String CONSUMER_LOCATION = "secondary.kafka.consumer.properties";
    private static final String DEFAULT_PRODUCER =
            "file:/tmp/config/secondary-kafka-producer.properties,classpath:/secondary-kafka-producer.properties";
    private static final String DEFAULT_CONSUMER =
            "file:/tmp/config/secondary-kafka-consumer.properties,classpath:/secondary-kafka-consumer.properties";
    private static final String REGISTRY_URL = "secondary.schema.registry.url";
    private static final String REGISTRY_PREFIX = "secondary.schema.registry";
    // application-level retry/DLQ policy, shared with the primary adapter
    private static final String DLQ_TIMEOUT = "kafka.dlq.timeout.ms";
    private static final String MAX_RETRIES = "kafka.flow.max.retries";
    private static final String RETRY_BACKOFF = "kafka.flow.retry.backoff.ms";

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        KafkaRequestPublisher publisher = new KafkaRequestPublisher(new KafkaProducer<>(
                KafkaClientConfig.producerProperties(config, PRODUCER_LOCATION, DEFAULT_PRODUCER)));
        SecondaryKafkaRuntime.setPublisher(publisher);

        // per-cluster and optional: unset means raw byte[] on the secondary cluster
        SchemaCodec schemaCodec = SchemaCodec.fromConfig(config, config.getProperty(REGISTRY_URL), REGISTRY_PREFIX);
        SecondaryKafkaRuntime.setSchemaCodec(schemaCodec);

        String adapterConfig = config.getProperty(ADAPTER_CONFIG);
        if (adapterConfig != null) {
            long dlqTimeout = Long.parseLong(config.getProperty(DLQ_TIMEOUT, "10000"));
            int maxRetries = Integer.parseInt(config.getProperty(MAX_RETRIES, "3"));
            long retryBackoffMs = Long.parseLong(config.getProperty(RETRY_BACKOFF, "500"));
            // dead letters from secondary bindings go through the SECONDARY publisher (same cluster)
            RetryPolicy retryPolicy = new RetryPolicy(maxRetries, retryBackoffMs, publisher);
            Properties consumerProps = KafkaClientConfig.consumerProperties(config, CONSUMER_LOCATION, DEFAULT_CONSUMER);
            KafkaFlowAdapter adapter = new KafkaFlowAdapter(consumerProps, new ConfigReader(adapterConfig),
                    dlqTimeout, retryPolicy, schemaCodec);
            adapter.start();
            SecondaryKafkaRuntime.setAdapter(adapter);
            log.info("Secondary Kafka flow adapter started from {}", adapterConfig);
        } else {
            log.info("{} not set; secondary Kafka flow adapter not started", ADAPTER_CONFIG);
        }
    }
}
