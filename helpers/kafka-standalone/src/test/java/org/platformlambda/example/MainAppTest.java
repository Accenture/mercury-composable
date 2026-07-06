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

package org.platformlambda.example;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end test of the standalone Kafka helper as the field would run it: {@link MainApp#start} boots the
 * embedded broker(s), then a real producer/consumer round-trips messages through {@code 127.0.0.1:9092}.
 * <p>
 * The test {@code application.properties} sets {@code dual.servers=true}, so {@code start()} exercises the
 * two-broker path (9092 via {@code /server.properties} and 8092 via {@code /server2.properties}). Both
 * brokers are awaited before the tests run and shut down explicitly afterwards, so the forked test JVM
 * terminates cleanly (KRaft threads are non-daemon and would otherwise linger until JVM-exit hooks fire).
 */
class MainAppTest {

    private static final String BOOTSTRAP = "127.0.0.1:9092";
    private static final String BOOTSTRAP_2 = "127.0.0.1:8092";
    private static MainApp app;

    @BeforeAll
    static void startViaMainApp() throws Exception {
        app = new MainApp();
        app.start(new String[0]);
        // ensure BOTH brokers are fully up (the 2nd is started asynchronously) so shutdown is deterministic
        awaitBroker(BOOTSTRAP);
        awaitBroker(BOOTSTRAP_2);
    }

    @AfterAll
    static void stopBrokers() {
        if (app != null) {
            app.brokers.forEach(broker -> {
                try {
                    broker.shutdown();
                } catch (Exception e) {
                    // best effort - the JVM shutdown hook is a backstop
                }
            });
        }
        Utility util = Utility.getInstance();
        for (String dir : List.of("/tmp/kafka-logs", "/tmp/kafka2-logs")) {
            File f = new File(dir);
            if (f.exists()) {
                try {
                    util.cleanupDir(f);
                } catch (Exception e) {
                    // best effort
                }
            }
        }
    }

    @Test
    void publishSubscribeRoundTrip() throws Exception {
        String topic = "standalone-pubsub-test";
        createTopic(topic);

        try (Producer<String, byte[]> producer = newProducer()) {
            for (int i = 0; i < 3; i++) {
                producer.send(new ProducerRecord<>(topic, "k" + i, ("msg-" + i).getBytes(UTF_8)))
                        .get(10, TimeUnit.SECONDS);
            }
        }

        try (Consumer<String, byte[]> consumer = newConsumer("standalone-test-group")) {
            consumer.subscribe(List.of(topic));
            List<String> received = drain(consumer, 3, 20_000);
            assertEquals(List.of("msg-0", "msg-1", "msg-2"), received,
                    "all three messages should round-trip in order on a single partition");
        }
    }

    /** Block until the broker answers an AdminClient metadata request (or the API timeout elapses). */
    private static void awaitBroker(String bootstrap) throws Exception {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 30000);
        try (Admin admin = Admin.create(p)) {
            admin.describeCluster().nodes().get(30, TimeUnit.SECONDS);
        }
    }

    private static void createTopic(String topic) throws Exception {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        try (Admin admin = Admin.create(p)) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get(20, TimeUnit.SECONDS);
        }
    }

    private static Producer<String, byte[]> newProducer() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(p);
    }

    private static Consumer<String, byte[]> newConsumer(String group) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(p);
    }

    private static List<String> drain(Consumer<String, byte[]> consumer, int expected, long timeoutMillis) {
        List<String> values = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (values.size() < expected && System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, byte[]> consumerRecord : records) {
                values.add(new String(consumerRecord.value(), UTF_8));
            }
        }
        return values;
    }
}
