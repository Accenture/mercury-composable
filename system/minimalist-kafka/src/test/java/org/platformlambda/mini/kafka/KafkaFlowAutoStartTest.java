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

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.ManagedCache;
import org.platformlambda.mini.kafka.schema.SchemaCodec;
import org.platformlambda.mini.kafka.schema.SchemaType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pins {@link KafkaFlowAutoStart#resolveConsumerSchemaCodec}: the flow adapter shares the producer's
 * {@link SchemaCodec} unless {@code schema.registry.consumer.properties} names a registry client template, in
 * which case it decodes with its own codec, built under the {@code schema.registry.consumer} prefix against the
 * same registry (a field installation whose CSFLE key access is granted per direction - a produce identity pool
 * and a consume identity pool - needs the two directions to carry different identities).
 *
 * <p>Plain {@link ConfigReader} fixtures, not the process-wide {@code AppConfigReader}, so the opt-in key can
 * neither leak into nor be affected by {@code KafkaFlowAdapterTest}'s real {@code start()} run. The in-JVM
 * {@link EmbeddedSchemaRegistry} enforces no access control, so this proves the wiring - a distinct codec that
 * decodes what the producer's codec encoded - not an identity-pool grant, which is the registry's concern.</p>
 */
// resource: the registry clients are owned by the codecs for the suite lifetime - tests must not close them
@SuppressWarnings("resource")
class KafkaFlowAutoStartTest {

    private static final String UNSET = "classpath:/consumer-codec-unset.properties";
    private static final String BLANK = "classpath:/consumer-codec-blank.properties";
    private static final String SET = "classpath:/consumer-codec-set.properties";
    private static final String TOPIC = "consumer-identity";
    private static final String JSON_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}},\"additionalProperties\":true}";

    private static EmbeddedSchemaRegistry registry;
    private static SchemaCodec producerCodec;

    @BeforeAll
    static void setup() throws Exception {
        registry = new EmbeddedSchemaRegistry();
        // the producer's codec, exactly as start() builds it: the default schema.registry prefix
        producerCodec = SchemaCodec.fromConfig(new ConfigReader(UNSET), registry.baseUrl());
        assertNotNull(producerCodec);
    }

    @AfterAll
    static void teardown() {
        if (registry != null) {
            registry.close();
        }
    }

    @Test
    void unsetLocationReusesTheProducerCodec() {
        ConfigReader config = new ConfigReader(UNSET);
        assertNull(config.getProperty(KafkaFlowAutoStart.CONSUMER_REGISTRY_LOCATION), "fixture leaves the key unset");
        SchemaCodec consumerCodec =
                KafkaFlowAutoStart.resolveConsumerSchemaCodec(config, registry.baseUrl(), producerCodec);
        assertSame(producerCodec, consumerCodec, "unset: the adapter decodes with the producer's codec, as before");
    }

    @Test
    void blankLocationReusesTheProducerCodec() {
        ConfigReader config = new ConfigReader(BLANK);
        assertEquals("", config.getProperty(KafkaFlowAutoStart.CONSUMER_REGISTRY_LOCATION), "fixture sets a blank");
        SchemaCodec consumerCodec =
                KafkaFlowAutoStart.resolveConsumerSchemaCodec(config, registry.baseUrl(), producerCodec);
        assertSame(producerCodec, consumerCodec, "blank (the ${ENV_VAR:} idiom) means off, like schema.registry.url");
    }

    @Test
    void unsetLocationWithSchemaFeaturesOffStaysOff() {
        // schema.registry.url unset: the producer has no codec, and neither has the consumer
        SchemaCodec consumerCodec = KafkaFlowAutoStart.resolveConsumerSchemaCodec(new ConfigReader(UNSET), null, null);
        assertNull(consumerCodec);
    }

    @Test
    void configuredLocationWithSchemaFeaturesOffStaysOff() {
        // the opt-in key alone does not switch schema features on - schema.registry.url remains the switch
        SchemaCodec consumerCodec = KafkaFlowAutoStart.resolveConsumerSchemaCodec(new ConfigReader(SET), "", null);
        assertNull(consumerCodec);
    }

    @Test
    void configuredLocationBuildsAnIndependentCodecThatDecodesFromTheSameRegistry() throws Exception {
        ConfigReader config = new ConfigReader(SET);
        SchemaCodec consumerCodec =
                KafkaFlowAutoStart.resolveConsumerSchemaCodec(config, registry.baseUrl(), producerCodec);
        assertNotNull(consumerCodec);
        assertNotSame(producerCodec, consumerCodec, "set: the adapter decodes with its own codec");
        // built under the consumer prefix - its own id cache, never the producer's (global ids are per registry)
        assertNotNull(ManagedCache.getInstance(KafkaFlowAutoStart.CONSUMER_REGISTRY_PREFIX),
                "the consumer codec's caches carry the schema.registry.consumer prefix");
        // the seam works end-to-end: what the producer's codec encodes, the consumer's codec decodes - one registry
        int id = producerCodec.client().register(TOPIC + "-value", new JsonSchema(JSON_SCHEMA));
        byte[] framed = producerCodec.newEncoder().serialize(TOPIC, SchemaType.JSON, id, Map.of("hello", "world"));
        Object decoded = consumerCodec.newDecoder().decode(TOPIC, framed);
        assertInstanceOf(Map.class, decoded);
        assertEquals("world", ((Map<?, ?>) decoded).get("hello"));
    }
}
