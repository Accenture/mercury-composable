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

import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.common.ConfigBase;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * An empty {@link ConfigReader} is a no-op {@link ConfigBase} (every {@code getProperty(key, default)}
 * returns the default), so these load the default classpath templates (file:/tmp/... falls back to
 * classpath) and assert that the library pins the wire contract over whatever the template says. The
 * delivery-mode overlay ({@code enable.auto.commit} / {@code max.poll.records}) is applied per-binding by
 * {@link KafkaFlowAdapter#applyDeliveryMode}, not here - see {@code KafkaFlowAdapterConfigTest}.
 */
class KafkaClientConfigTest {

    private static final ConfigBase EMPTY = new ConfigReader();

    @Test
    void producerLoadsTemplateAndPinsSerializers() {
        Properties p = KafkaClientConfig.producerProperties(EMPTY);
        assertEquals(StringSerializer.class.getName(), p.getProperty("key.serializer"));
        assertEquals(ByteArraySerializer.class.getName(), p.getProperty("value.serializer"));
        assertEquals("all", p.getProperty("acks"), "non-pinned value comes from the template");
        assertNotNull(p.getProperty("bootstrap.servers"), "connection comes from the template");
    }

    @Test
    void consumerLoadsTemplateAndPinsDeserializers() {
        Properties p = KafkaClientConfig.consumerProperties(EMPTY);
        assertEquals(StringDeserializer.class.getName(), p.getProperty("key.deserializer"));
        assertEquals(ByteArrayDeserializer.class.getName(), p.getProperty("value.deserializer"));
        assertNotNull(p.getProperty("bootstrap.servers"));
    }
}
