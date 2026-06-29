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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Builds Kafka client {@link Properties} from external <b>template</b> files, so the wide variety of
 * enterprise Kafka installations (on-prem / cloud / SaaS / Confluent; SASL, OAuth2, mTLS) is handled by
 * configuration rather than code. Templates are read via {@link ConfigReader} (which applies
 * {@code ${ENV_VAR:default}} substitution) from a file-then-classpath fallback location, overridable with
 * the {@code kafka.producer.properties} / {@code kafka.consumer.properties} application settings.
 *
 * <p>Only the parameters the library's contract depends on are <b>pinned in code</b> (and override
 * whatever the template says): the {@code String}/{@code byte[]} (de)serializers (the wire contract), and -
 * for the consumer - {@code enable.auto.commit=false} and {@code max.poll.records=1} (the at-least-once,
 * commit-after-process contract). The per-topic {@code group.id} is set by {@link KafkaFlowAdapter}.</p>
 */
public final class KafkaClientConfig {

    private static final String PRODUCER_LOCATION = "kafka.producer.properties";
    private static final String CONSUMER_LOCATION = "kafka.consumer.properties";
    private static final String DEFAULT_PRODUCER =
            "file:/tmp/config/kafka-producer.properties,classpath:/kafka-producer.properties";
    private static final String DEFAULT_CONSUMER =
            "file:/tmp/config/kafka-consumer.properties,classpath:/kafka-consumer.properties";

    private KafkaClientConfig() {}

    /** Producer config from the template, with the byte[] wire-contract serializers pinned. */
    public static Properties producerProperties(ConfigBase appConfig) {
        Properties p = load(appConfig.getProperty(PRODUCER_LOCATION, DEFAULT_PRODUCER));
        p.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        return p;
    }

    /**
     * Base consumer config from the template, with the wire-contract deserializers and the at-least-once
     * settings pinned. The caller ({@link KafkaFlowAdapter}) adds a per-topic {@code group.id}.
     */
    public static Properties consumerProperties(ConfigBase appConfig) {
        Properties p = load(appConfig.getProperty(CONSUMER_LOCATION, DEFAULT_CONSUMER));
        p.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        // commit-after-process, one message at a time -> at-least-once (see KafkaFlowConsumer)
        p.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        p.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1");
        return p;
    }

    private static Properties load(String locations) {
        Map<String, Object> resolved = loadFirst(locations).getCompositeKeyValues();
        Properties p = new Properties();
        resolved.forEach((key, value) -> p.setProperty(key, String.valueOf(value)));
        return p;
    }

    /** Try each comma-separated location in order (file path then classpath), returning the first found. */
    private static ConfigReader loadFirst(String locations) {
        List<String> paths = Utility.getInstance().split(locations, ", ");
        for (String path : paths) {
            try {
                return new ConfigReader(path);
            } catch (IllegalArgumentException notFound) {
                // fall through to the next location (file -> classpath fallback)
            }
        }
        throw new IllegalArgumentException("No Kafka client config found at any of: " + locations);
    }
}
