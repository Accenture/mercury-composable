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
 * {@code kafka.flow.max.retries} (default 3), {@code kafka.flow.retry.backoff.ms} (default 500),
 * {@code kafka.flow.dlq.suffix} (default {@code .dlq}) - appended to each source topic to form its
 * <b>per-topic</b> DLQ ({@code orders} → {@code orders.dlq}) - and {@code kafka.dlq.timeout.ms} (default
 * 10000), the confirm-write timeout for the dead-letter publish. Only the suffix is configurable, since a
 * shared/global DLQ is an anti-pattern for reprocessing; DLQ topics must be pre-provisioned. There is no
 * flow-processing timeout knob: a flow's own {@code ttl} is its deadline (Kafka is asynchronous, with no
 * inherent request timeout).</p>
 */
@MainApplication
public class KafkaFlowAutoStart implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowAutoStart.class);
    private static final String ADAPTER_CONFIG = "yaml.kafka.flow.adapter";
    // The flow's own ttl is the deadline for processing; only the dead-letter confirm-write needs a timeout.
    private static final String DLQ_TIMEOUT = "kafka.dlq.timeout.ms";
    private static final String MAX_RETRIES = "kafka.flow.max.retries";
    private static final String RETRY_BACKOFF = "kafka.flow.retry.backoff.ms";
    private static final String DLQ_SUFFIX = "kafka.flow.dlq.suffix";

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        KafkaRequestPublisher publisher =
                new KafkaRequestPublisher(new KafkaProducer<>(KafkaClientConfig.producerProperties(config)));
        KafkaRuntime.setPublisher(publisher);

        /*
         * Optional Confluent Schema Registry codec (null when schema.registry.url is not configured). A shared
         * factory: simple.kafka.notification (produce) and the flow adapter (consume) each mint their own
         * owner-confined encoder/decoder from it, since the Confluent serdes are not thread-safe.
         */
        SchemaCodec schemaCodec = SchemaCodec.fromConfig(config);
        KafkaRuntime.setSchemaCodec(schemaCodec);

        String adapterConfig = config.getProperty(ADAPTER_CONFIG);
        if (adapterConfig != null) {
            // confirm-write timeout for the dead-letter publish (broker ack); the flow wait uses flow.ttl
            long dlqTimeout = Long.parseLong(config.getProperty(DLQ_TIMEOUT, "10000"));
            int maxRetries = Integer.parseInt(config.getProperty(MAX_RETRIES, "3"));
            long retryBackoffMs = Long.parseLong(config.getProperty(RETRY_BACKOFF, "500"));
            // per-topic DLQ: <topic><suffix>; only the suffix is configurable (no global DLQ - anti-pattern)
            String dlqSuffix = config.getProperty(DLQ_SUFFIX, ".dlq");
            // failed messages are dead-lettered through the same shared producer
            RetryPolicy retryPolicy = new RetryPolicy(maxRetries, retryBackoffMs, dlqSuffix, publisher);
            Properties consumerProps = KafkaClientConfig.consumerProperties(config);
            KafkaFlowAdapter adapter = new KafkaFlowAdapter(consumerProps, new ConfigReader(adapterConfig),
                    dlqTimeout, retryPolicy, schemaCodec);
            adapter.start();
            KafkaRuntime.setAdapter(adapter);
            log.info("Kafka flow adapter started from {}", adapterConfig);
        } else {
            log.info("{} not set; Kafka flow adapter not started", ADAPTER_CONFIG);
        }
    }
}
