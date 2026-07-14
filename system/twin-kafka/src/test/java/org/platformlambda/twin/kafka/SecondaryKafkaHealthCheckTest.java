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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.util.Utility;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class SecondaryKafkaHealthCheckTest {

    private static final Map<String, String> INFO = Map.of("type", "info");
    private static final Map<String, String> HEALTH = Map.of("type", "health");
    private static EmbeddedKafka secondaryCluster;

    @BeforeAll
    static void boot() {
        secondaryCluster = new EmbeddedKafka(17092, 17093, "/tmp/twin-kafka-health");
    }

    @AfterAll
    static void shutdown() {
        if (secondaryCluster != null) {
            secondaryCluster.close();
        }
    }

    private static Properties consumerProps(String bootstrapServers) {
        Properties p = new Properties();
        p.setProperty("bootstrap.servers", bootstrapServers);
        p.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        p.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return p;
    }

    @SuppressWarnings("unchecked")
    @Test
    void infoDescribesTheSecondaryKafkaDependency() throws Exception {
        var health = new SecondaryKafkaHealthCheck(consumerProps(secondaryCluster.bootstrapServers()), 0);
        var result = health.handleEvent(INFO, null, 1);
        assertInstanceOf(Map.class, result);
        Map<String, Object> map = (Map<String, Object>) result;
        // the /health dependency list distinguishes the two clusters by service name
        assertEquals("secondary.kafka", map.get("service"));
        assertEquals(secondaryCluster.bootstrapServers(), map.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void startupReturnsPlaceholderThenLiveStatus() throws Exception {
        var health = new SecondaryKafkaHealthCheck(consumerProps(secondaryCluster.bootstrapServers()), 60000);
        var first = health.handleEvent(HEALTH, null, 1);
        assertInstanceOf(Map.class, first);
        assertEquals("Kafka client is starting up", ((Map<String, Object>) first).get("status"));
        Map<String, Object> live = null;
        // generous for busy CI executors - the poll returns as soon as warm-up completes
        long deadline = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> result = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
            if ("Kafka cluster is reachable".equals(result.get("status"))) {
                live = result;
                break;
            }
            Utility.getInstance().sleep(200);
        }
        assertNotNull(live, "warm-up should complete and report live secondary cluster status");
        assertEquals(secondaryCluster.bootstrapServers(), live.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void unreachableSecondaryFailsWhileLiveInstanceStaysHealthy() throws Exception {
        // the two probes are independent: a dead secondary must not poison a healthy one
        var healthy = new SecondaryKafkaHealthCheck(consumerProps(secondaryCluster.bootstrapServers()), 0);
        Properties bad = consumerProps("127.0.0.1:65530");
        bad.setProperty("request.timeout.ms", "1000");
        bad.setProperty("default.api.timeout.ms", "2000");
        var offline = new SecondaryKafkaHealthCheck(bad, 0);
        AppException down = assertThrows(AppException.class,
                () -> offline.handleEvent(HEALTH, null, 1));
        assertEquals(503, down.getStatus());
        assertTrue(down.getMessage().contains("not reachable"));
        Map<String, Object> alive = (Map<String, Object>) healthy.handleEvent(HEALTH, null, 1);
        assertEquals("Kafka cluster is reachable", alive.get("status"));
    }
}
