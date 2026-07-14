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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.util.Utility;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class KafkaHealthCheckTest {

    private static final Map<String, String> INFO = Map.of("type", "info");
    private static final Map<String, String> HEALTH = Map.of("type", "health");
    private static EmbeddedKafka kafka;

    @BeforeAll
    static void boot() {
        kafka = new EmbeddedKafka();
    }

    @AfterAll
    static void shutdown() {
        if (kafka != null) {
            kafka.close();
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
    void infoDescribesTheKafkaDependency() {
        var health = new KafkaHealthCheck(consumerProps(kafka.bootstrapServers()), 0);
        var result = health.handleEvent(INFO, null, 1);
        assertInstanceOf(Map.class, result);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("kafka", map.get("service"));
        assertEquals(kafka.bootstrapServers(), map.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void startupReturnsPlaceholderThenLiveStatus() {
        var health = new KafkaHealthCheck(consumerProps(kafka.bootstrapServers()), 60000);
        // within the grace period, the first check is a placeholder healthy status - the Kafka
        // client warms up in the background so /health never blocks during app start-up
        var first = health.handleEvent(HEALTH, null, 1);
        assertInstanceOf(Map.class, first);
        assertEquals("Kafka client is starting up", ((Map<String, Object>) first).get("status"));
        // once warm-up completes, checks report the live cluster status
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
        assertNotNull(live, "warm-up should complete and report live cluster status");
        assertInstanceOf(Integer.class, live.get("topics"));
        assertEquals(kafka.bootstrapServers(), live.get("href"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void probesImmediatelyWhenGraceExpired() {
        var health = new KafkaHealthCheck(consumerProps(kafka.bootstrapServers()), 0);
        Map<String, Object> result = (Map<String, Object>) health.handleEvent(HEALTH, null, 1);
        assertEquals("Kafka cluster is reachable", result.get("status"));
    }

    @Test
    void unreachableClusterFailsTheHealthCheck() {
        // closed port + short client timeouts = fast failure
        Properties bad = consumerProps("127.0.0.1:65531");
        bad.setProperty("request.timeout.ms", "1000");
        bad.setProperty("default.api.timeout.ms", "2000");
        var health = new KafkaHealthCheck(bad, 0);
        AppException offline = assertThrows(AppException.class,
                () -> health.handleEvent(HEALTH, null, 1));
        assertEquals(503, offline.getStatus());
        assertTrue(offline.getMessage().contains("not reachable"));
    }

    @Test
    void invalidTypeIsRejected() {
        var health = new KafkaHealthCheck(consumerProps(kafka.bootstrapServers()), 0);
        Map<String, String> badType = Map.of("type", "unknown");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> health.handleEvent(badType, null, 1));
        assertEquals("type must be info or health", error.getMessage());
    }
}
