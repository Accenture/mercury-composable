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
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Autoloads the Kafka building blocks at startup. It runs as a {@link MainApplication} - i.e. AFTER the
 * platform-core engine has registered every composable function, which the flow adapter routes messages
 * into (a {@code @BeforeApplication} would run too early, before the engine exists).
 *
 * <p>It builds the shared {@link KafkaRequestPublisher} singleton from {@code kafka.bootstrap.servers},
 * and, when {@code yaml.kafka.flow.adapter} is configured, starts the {@link KafkaFlowAdapter} from that
 * file. Both settings support {@code ${ENV_VAR:default}} substitution and system-property override.</p>
 */
@MainApplication
public class KafkaFlowAutoStart implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(KafkaFlowAutoStart.class);
    private static final String BOOTSTRAP = "kafka.bootstrap.servers";
    private static final String ADAPTER_CONFIG = "yaml.kafka.flow.adapter";
    private static final String FLOW_TIMEOUT = "kafka.flow.timeout.ms";

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        String bootstrap = config.getProperty(BOOTSTRAP, "127.0.0.1:9092");
        KafkaRuntime.setPublisher(new KafkaRequestPublisher(newProducer(bootstrap)));

        String adapterConfig = config.getProperty(ADAPTER_CONFIG);
        if (adapterConfig != null) {
            long flowTimeoutMs = Long.parseLong(config.getProperty(FLOW_TIMEOUT, "30000"));
            KafkaFlowAdapter adapter = new KafkaFlowAdapter(bootstrap, new ConfigReader(adapterConfig), flowTimeoutMs);
            adapter.start();
            KafkaRuntime.setAdapter(adapter);
            log.info("Kafka flow adapter started from {}", adapterConfig);
        } else {
            log.info("{} not set; Kafka flow adapter not started", ADAPTER_CONFIG);
        }
    }

    private static Producer<String, byte[]> newProducer(String bootstrapServers) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        p.put(ProducerConfig.ACKS_CONFIG, "1");
        return new KafkaProducer<>(p);
    }
}
