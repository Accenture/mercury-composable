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
 * Pins the secondary cluster's consumer-side registry identity - {@code secondary.schema.registry.consumer.properties},
 * the twin of minimalist-kafka's {@code schema.registry.consumer.properties}: {@link SecondaryKafkaAutoStart} hands the
 * secondary flow adapter {@link SchemaCodec#forConsumer} under the {@code secondary.schema.registry} prefix, which is
 * the secondary producer's codec unless the key names a template, and then a codec of its own built under
 * {@code secondary.schema.registry.consumer} against the same registry.
 *
 * <p>Plain {@link ConfigReader} fixtures, not the process-wide {@code AppConfigReader}, so nothing leaks into the
 * bridge test's real {@code start()} run. The in-JVM {@link EmbeddedSchemaRegistry} enforces no access control, so
 * this proves the wiring - a distinct codec that decodes what the producer's codec encoded - not an identity-pool
 * grant, which is the registry's concern.</p>
 */
// resource: the registry clients are owned by the codecs for the suite lifetime - tests must not close them
@SuppressWarnings("resource")
class SecondaryConsumerCodecTest {

    private static final String PREFIX = "secondary.schema.registry";
    private static final String CONSUMER_LOCATION = PREFIX + ".consumer.properties";
    private static final String CONSUMER_PREFIX = PREFIX + ".consumer";
    private static final String UNSET = "classpath:/secondary-consumer-codec-unset.properties";
    private static final String SET = "classpath:/secondary-consumer-codec-set.properties";
    private static final String TOPIC = "secondary-consumer-identity";
    private static final String JSON_SCHEMA =
            "{\"type\":\"object\",\"properties\":{\"hello\":{\"type\":\"string\"}},\"additionalProperties\":true}";

    private static EmbeddedSchemaRegistry registry;
    private static SchemaCodec producerCodec;

    @BeforeAll
    static void setup() throws Exception {
        registry = new EmbeddedSchemaRegistry();
        // the secondary producer's codec, exactly as SecondaryKafkaAutoStart builds it
        producerCodec = SchemaCodec.fromConfig(new ConfigReader(UNSET), registry.baseUrl(), PREFIX);
        assertNotNull(producerCodec);
    }

    @AfterAll
    static void teardown() {
        if (registry != null) {
            registry.close();
        }
    }

    @Test
    void unsetLocationReusesTheSecondaryProducerCodec() {
        ConfigReader config = new ConfigReader(UNSET);
        assertNull(config.getProperty(CONSUMER_LOCATION), "fixture leaves the key unset");
        SchemaCodec consumerCodec = SchemaCodec.forConsumer(config, registry.baseUrl(), PREFIX, producerCodec);
        assertSame(producerCodec, consumerCodec, "unset: the secondary adapter decodes with the producer's codec");
    }

    @Test
    void configuredLocationWithSchemaFeaturesOffStaysOff() {
        // the opt-in alone does not switch schema features on - secondary.schema.registry.url remains the switch
        assertNull(SchemaCodec.forConsumer(new ConfigReader(SET), "", PREFIX, null));
    }

    @Test
    void configuredLocationBuildsTheSecondaryConsumerCodecAgainstTheSameRegistry() throws Exception {
        ConfigReader config = new ConfigReader(SET);
        SchemaCodec consumerCodec = SchemaCodec.forConsumer(config, registry.baseUrl(), PREFIX, producerCodec);
        assertNotNull(consumerCodec);
        assertNotSame(producerCodec, consumerCodec, "set: the secondary adapter decodes with its own codec");
        // built under the secondary consumer prefix - its own id cache (global ids are per registry)
        assertNotNull(ManagedCache.getInstance(CONSUMER_PREFIX),
                "the consumer codec's caches carry the secondary.schema.registry.consumer prefix");
        // the seam works end-to-end: what the secondary producer's codec encodes, the consumer's codec decodes
        int id = producerCodec.client().register(TOPIC + "-value", new JsonSchema(JSON_SCHEMA));
        byte[] framed = producerCodec.newEncoder().serialize(TOPIC, SchemaType.JSON, id, Map.of("hello", "twin"));
        Object decoded = consumerCodec.newDecoder().decode(TOPIC, framed);
        assertInstanceOf(Map.class, decoded);
        assertEquals("twin", ((Map<?, ?>) decoded).get("hello"));
    }
}
