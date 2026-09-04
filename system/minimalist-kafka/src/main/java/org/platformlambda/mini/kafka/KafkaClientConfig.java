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
 * {@code ${ENV_VAR:default}} substitution) from the bundled classpath template by default, overridable with
 * the {@code kafka.producer.properties} / {@code kafka.consumer.properties} /
 * {@code schema.registry.properties} application settings.
 *
 * <p>Normally a template setting names one location. Use a comma-separated list only when you deliberately
 * want a fallback chain, e.g. a CI/CD-rendered external file first and the classpath template second.</p>
 *
 * <p>Only the parameters the library's contract depends on are <b>pinned in code</b> (and override
 * whatever the template says): the {@code String}/{@code byte[]} (de)serializers (the wire contract).
 * The per-topic {@code group.id}, plus the per-binding delivery-mode overlay ({@code enable.auto.commit}
 * and {@code max.poll.records}, driven by each binding's {@code auto-commit}/{@code max-poll-records}), are
 * applied by {@code KafkaFlowAdapter.newConsumer}.</p>
 *
 * <p>An OAuth 2.0 token endpoint URL found in any template ({@code sasl.oauthbearer.token.endpoint.url})
 * is auto-registered on the JVM allow-list - see {@link OAuthUrlAllowList}.</p>
 *
 * <p>Either client can be switched off for its cluster with {@code kafka.producer.enabled} /
 * {@code kafka.consumer.enabled} (default true) - see {@link #clientEnabled} for why the one-way leg of
 * a bridge needs it.</p>
 */
public final class KafkaClientConfig {

    private static final String PRODUCER_LOCATION = "kafka.producer.properties";
    private static final String CONSUMER_LOCATION = "kafka.consumer.properties";
    private static final String SCHEMA_REGISTRY_LOCATION = "schema.registry.properties";
    private static final String DEFAULT_PRODUCER = "classpath:/kafka-producer.properties";
    private static final String DEFAULT_CONSUMER = "classpath:/kafka-consumer.properties";
    private static final String DEFAULT_SCHEMA_REGISTRY = "classpath:/schema-registry.properties";
    /** Opt-out flag for the primary cluster's producer (default true) - see {@link #clientEnabled}. */
    public static final String PRODUCER_ENABLED = "kafka.producer.enabled";
    /** Opt-out flag for the primary cluster's consumer (default true) - see {@link #clientEnabled}. */
    public static final String CONSUMER_ENABLED = "kafka.consumer.enabled";
    private static final String DISABLED = "false";

    private KafkaClientConfig() {}

    /** Whether the primary cluster's producer is enabled ({@code kafka.producer.enabled}, default true). */
    public static boolean producerEnabled(ConfigBase appConfig) {
        return clientEnabled(appConfig, PRODUCER_ENABLED);
    }

    /** Whether the primary cluster's consumer is enabled ({@code kafka.consumer.enabled}, default true). */
    public static boolean consumerEnabled(ConfigBase appConfig) {
        return clientEnabled(appConfig, CONSUMER_ENABLED);
    }

    /**
     * Whether a Kafka client is enabled for its cluster - the reuse seam for a library (e.g. twin-kafka)
     * whose second cluster has its own {@code secondary.kafka.*.enabled} keys.
     *
     * <p>The flag is a <b>veto, not a trigger</b>: the default is enabled, and only the literal
     * {@code false} switches a client off. Leaving it at the default therefore starts nothing that is
     * not otherwise configured - an inbound adapter still requires its {@code yaml.*.flow.adapter}
     * setting. Its purpose is the one-way leg of a bridge, where the cluster grants credentials for a
     * producer OR a consumer but not both, and building the unused client fails the deployment.</p>
     *
     * @param appConfig the application configuration
     * @param key       the application property naming the flag (e.g. "kafka.producer.enabled")
     * @return false only when the key is explicitly set to {@code false}
     */
    public static boolean clientEnabled(ConfigBase appConfig, String key) {
        return !DISABLED.equalsIgnoreCase(appConfig.getProperty(key, "true").trim());
    }

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
     * delivery-mode overlay ({@code enable.auto.commit} / {@code max.poll.records}). A template
     * {@code group.protocol=auto} is resolved here to {@code consumer} or {@code classic} from the
     * cluster's finalized {@code group.version} feature - see {@link GroupProtocolResolver}.
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
        // a template's group.protocol=auto becomes consumer|classic from the cluster's own
        // finalized group.version feature (KIP-848) - one probe per cluster, stated in the log
        GroupProtocolResolver.resolve(p);
        return p;
    }

    /**
     * Client config for the {@code kafka.health} Metadata probe: the consumer template normally, or the
     * PRODUCER template on a produce-only leg (consumer disabled, producer enabled).
     *
     * <p>A one-way bridge leg holds credentials for one client only, yet a bridge is healthy only when
     * BOTH clusters are reachable - so the probe follows whichever client the deployment actually
     * configured. The producer template is filtered to {@link ConsumerConfig#configNames()} before use,
     * so producer-only settings ({@code acks}, {@code partitioner.class}, the serializers) are dropped
     * rather than logged as unknown-config warnings; every connection and security parameter
     * ({@code bootstrap.servers}, {@code security.protocol}, {@code sasl.*}, {@code ssl.*}) is named
     * identically in both client surfaces and survives the filter. The probe joins no consumer group, so
     * nothing consumer-specific is required. Same filtering idiom as
     * {@code GroupProtocolResolver.probe}, which reduces a consumer template to the Admin surface.</p>
     *
     * <p>When both clients are disabled the consumer template is used unchanged: the module is inert and
     * an application in that state should not be listing the health dependency at all.</p>
     */
    public static Properties healthProbeProperties(ConfigBase appConfig) {
        return healthProbeProperties(appConfig, CONSUMER_ENABLED, CONSUMER_LOCATION, DEFAULT_CONSUMER,
                PRODUCER_ENABLED, PRODUCER_LOCATION, DEFAULT_PRODUCER);
    }

    /**
     * Health-probe config from caller-selected keys and template locations - the reuse seam for a library
     * (e.g. twin-kafka) probing an additional cluster. See {@link #healthProbeProperties(ConfigBase)} for
     * the fallback rule.
     *
     * @param appConfig          the application configuration
     * @param consumerEnabledKey the flag naming whether this cluster's consumer is enabled
     * @param consumerLocationKey the application property naming the consumer template location(s)
     * @param defaultConsumer    the consumer template fallback used when that key is unset
     * @param producerEnabledKey the flag naming whether this cluster's producer is enabled
     * @param producerLocationKey the application property naming the producer template location(s)
     * @param defaultProducer    the producer template fallback used when that key is unset
     * @return the probe's consumer properties
     */
    public static Properties healthProbeProperties(ConfigBase appConfig, String consumerEnabledKey,
                                                   String consumerLocationKey, String defaultConsumer,
                                                   String producerEnabledKey, String producerLocationKey,
                                                   String defaultProducer) {
        if (clientEnabled(appConfig, consumerEnabledKey) || !clientEnabled(appConfig, producerEnabledKey)) {
            return consumerProperties(appConfig, consumerLocationKey, defaultConsumer);
        }
        Properties producer = load(appConfig.getProperty(producerLocationKey, defaultProducer));
        Properties p = new Properties();
        producer.stringPropertyNames().stream()
                .filter(ConsumerConfig.configNames()::contains)
                .forEach(k -> p.setProperty(k, producer.getProperty(k)));
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
