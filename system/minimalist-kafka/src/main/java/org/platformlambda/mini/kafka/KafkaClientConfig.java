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
import org.apache.kafka.common.config.SaslConfigs;
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
 * the {@code kafka.producer.properties} / {@code kafka.consumer.properties} /
 * {@code schema.registry.properties} application settings.
 *
 * <p>Only the parameters the library's contract depends on are <b>pinned in code</b> (and override
 * whatever the template says): the {@code String}/{@code byte[]} (de)serializers (the wire contract).
 * The per-topic {@code group.id}, plus the per-binding delivery-mode overlay ({@code enable.auto.commit}
 * and {@code max.poll.records}, driven by each binding's {@code auto-commit}/{@code max-poll-records}), are
 * applied by {@code KafkaFlowAdapter.newConsumer}.</p>
 *
 * <p>An OAuth 2.0 token endpoint URL found in any template ({@code sasl.oauthbearer.token.endpoint.url})
 * is auto-registered on the JVM allow-list - see {@link OAuthUrlAllowList}.</p>
 */
public final class KafkaClientConfig {

    private static final String PRODUCER_LOCATION = "kafka.producer.properties";
    private static final String CONSUMER_LOCATION = "kafka.consumer.properties";
    private static final String SCHEMA_REGISTRY_LOCATION = "schema.registry.properties";
    private static final String DEFAULT_PRODUCER =
            "file:/tmp/config/kafka-producer.properties,classpath:/kafka-producer.properties";
    private static final String DEFAULT_CONSUMER =
            "file:/tmp/config/kafka-consumer.properties,classpath:/kafka-consumer.properties";
    private static final String DEFAULT_SCHEMA_REGISTRY =
            "file:/tmp/config/schema-registry.properties,classpath:/schema-registry.properties";

    private KafkaClientConfig() {}

    /** Producer config from the template, with the byte[] wire-contract serializers pinned. */
    public static Properties producerProperties(ConfigBase appConfig) {
        return producerProperties(appConfig, PRODUCER_LOCATION, DEFAULT_PRODUCER);
    }

    /**
     * Producer config from a caller-selected template location - the reuse seam for a library (e.g.
     * twin-kafka) that connects to an additional Kafka cluster with its own template. Same wire-contract
     * pinning as {@link #producerProperties(ConfigBase)}.
     *
     * @param appConfig        the application configuration
     * @param locationKey      the application property naming the template location(s)
     * @param defaultLocations the file-then-classpath fallback used when the key is unset
     * @return the producer properties
     */
    public static Properties producerProperties(ConfigBase appConfig, String locationKey, String defaultLocations) {
        Properties p = load(appConfig.getProperty(locationKey, defaultLocations));
        p.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        // default (not pinned): random distribution instead of Kafka's sticky default, which skews
        // low-volume traffic onto one partition; a template that sets partitioner.class wins
        p.putIfAbsent(ProducerConfig.PARTITIONER_CLASS_CONFIG, SimpleRandomPartitioner.class.getName());
        return p;
    }

    /**
     * Base consumer config from the template, with only the wire-contract deserializers pinned. The caller
     * ({@code KafkaFlowAdapter.newConsumer}) adds a per-topic {@code group.id} and the binding's
     * delivery-mode overlay ({@code enable.auto.commit} / {@code max.poll.records}).
     */
    public static Properties consumerProperties(ConfigBase appConfig) {
        return consumerProperties(appConfig, CONSUMER_LOCATION, DEFAULT_CONSUMER);
    }

    /**
     * Consumer config from a caller-selected template location - the reuse seam for a library (e.g.
     * twin-kafka) that connects to an additional Kafka cluster with its own template. Same wire-contract
     * pinning as {@link #consumerProperties(ConfigBase)}.
     *
     * @param appConfig        the application configuration
     * @param locationKey      the application property naming the template location(s)
     * @param defaultLocations the file-then-classpath fallback used when the key is unset
     * @return the base consumer properties
     */
    public static Properties consumerProperties(ConfigBase appConfig, String locationKey, String defaultLocations) {
        Properties p = load(appConfig.getProperty(locationKey, defaultLocations));
        p.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return p;
    }

    /**
     * Confluent Schema Registry client config from the template, passed through <b>verbatim</b> by
     * {@code SchemaCodec} so any Confluent client parameter (bearer/basic auth, SSL, optional settings
     * such as {@code bearer.auth.logical.cluster}) works without a library change. The registry URL
     * itself stays in {@code application.properties} ({@code schema.registry.url} - the feature switch).
     *
     * @param appConfig the application configuration (read for the template location override)
     * @return the resolved template key-values; empty when the template has no active entries
     */
    public static Map<String, Object> schemaRegistryProperties(ConfigBase appConfig) {
        return schemaRegistryProperties(appConfig, SCHEMA_REGISTRY_LOCATION, DEFAULT_SCHEMA_REGISTRY);
    }

    /**
     * Schema Registry client config from a caller-selected template location - the reuse seam for a
     * library (e.g. twin-kafka) whose second cluster has its own registry. Same verbatim pass-through
     * and OAuth allow-list registration as {@link #schemaRegistryProperties(ConfigBase)}.
     *
     * @param appConfig        the application configuration
     * @param locationKey      the application property naming the template location(s)
     * @param defaultLocations the file-then-classpath fallback used when the key is unset
     * @return the resolved template key-values; empty when the template has no active entries
     */
    public static Map<String, Object> schemaRegistryProperties(ConfigBase appConfig,
                                                               String locationKey, String defaultLocations) {
        Map<String, Object> resolved =
                loadFirst(appConfig.getProperty(locationKey, defaultLocations)).getCompositeKeyValues();
        registerOAuthTokenUrl(resolved.get("bearer.auth.issuer.endpoint.url"));
        registerOAuthTokenUrl(resolved.get(SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL));
        return resolved;
    }

    private static Properties load(String locations) {
        Map<String, Object> resolved = loadFirst(locations).getCompositeKeyValues();
        Properties p = new Properties();
        resolved.forEach((key, value) -> p.setProperty(key, String.valueOf(value)));
        registerOAuthTokenUrl(p.getProperty(SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL));
        return p;
    }

    /** Allow-list an OAuth token endpoint URL found in a template (no-op for null/blank). */
    private static void registerOAuthTokenUrl(Object url) {
        if (url instanceof String value) {
            OAuthUrlAllowList.register(value);
        }
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
