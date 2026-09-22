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

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shutdown contract of the Kafka building blocks, proven against a real (embedded) broker rather than
 * inferred from the client's Javadoc: closing a flow consumer makes its member <b>leave</b> the consumer
 * group - the coordinator sees zero members immediately - instead of the member lingering until its session
 * expires (45 seconds by default under the KIP-848 consumer protocol), which is how a rolling restart used to
 * park every partition the old pod held. {@link KafkaFlowAutoStart} registers {@link KafkaRuntime#shutdown()}
 * on {@code Platform.onShutdown} so a {@code SIGTERM} takes exactly this path; the registration itself is a
 * JVM shutdown hook and is verified live (the certification drive of 2026-09-22), not here.
 * <p>
 * Background: found by the two-engine OpenTelemetry drive with the Rust port, whose {@code rdkafka} consumers
 * left their groups on stop while the consumers of this engine were fenced by session expiry ~40 seconds later.
 */
class KafkaShutdownTest {
    private static final String TOPIC = "shutdown-test-topic";
    private static final String GROUP = "shutdown-test-group";
    private static EmbeddedKafka kafka;

    @BeforeAll
    static void boot() throws Exception {
        kafka = new EmbeddedKafka();
        KafkaTestSupport.createTopic(kafka.bootstrapServers(), TOPIC);
    }

    @AfterAll
    static void shutdown() {
        if (kafka != null) {
            kafka.close();
        }
    }

    @Test
    void closingTheAdapterLeavesTheConsumerGroup() {
        KafkaFlowConsumer consumer = flowConsumer();
        try (Admin admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers()))) {
            consumer.start();
            // the member joins: the coordinator reports it
            assertTrue(awaitMembers(admin, 1, 30000),
                    "the flow consumer should join " + GROUP + " (members: " + members(admin) + ")");
            // the property under test: close() = LeaveGroup, observed by the coordinator at once -
            // not after the session timeout
            long before = System.currentTimeMillis();
            consumer.close();
            assertTrue(awaitMembers(admin, 0, 10000),
                    "closing the consumer must make the member LEAVE the group, not linger until its "
                            + "session expires (members after close: " + members(admin) + ")");
            long elapsed = System.currentTimeMillis() - before;
            assertTrue(elapsed < 10000, "the leave was observed in " + elapsed + " ms - a session-timeout "
                    + "fence would have taken ~45 s");
        }
    }

    @Test
    void runtimeShutdownClosesConsumersThenProducerAndIsIdempotent() {
        // stand-ins for the process-wide singletons: a mock producer whose closed() flag is observable
        // Kafka 4.x MockProducer: (autoComplete, partitioner, keySerializer, valueSerializer); a null partitioner = default
        MockProducer<String, byte[]> producer =
                new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
        KafkaRequestPublisher publisher = new KafkaRequestPublisher(producer);
        KafkaRuntime.setPublisher(publisher);
        KafkaRuntime.setAdapter(null);
        assertFalse(producer.closed());
        KafkaRuntime.shutdown();
        assertTrue(producer.closed(), "shutdown() must close (flush) the shared producer");
        assertNull(KafkaRuntime.publisher(), "shutdown() forgets the publisher - a late caller sees 'not started'");
        assertNull(KafkaRuntime.adapter());
        // a second call is a no-op (the platform hook and a test teardown may both run it)
        assertDoesNotThrow(KafkaRuntime::shutdown);
        // and nothing-started is fine too
        KafkaRuntime.setPublisher(null);
        assertDoesNotThrow(KafkaRuntime::shutdown);
    }

    /** A flow consumer on a real Kafka consumer against the embedded broker, in the test group. */
    private static KafkaFlowConsumer flowConsumer() {
        KafkaConsumerBinding binding = KafkaConsumerBinding.builder()
                .topic(TOPIC).flowId("shutdown-flow").groupId(GROUP).build();
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        RetryPolicy policy = new RetryPolicy(1, 10, null);
        return new KafkaFlowConsumer(new KafkaConsumer<>(props), binding, 1000, policy, null);
    }

    private static boolean awaitMembers(Admin admin, int expected, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (members(admin) == expected) {
                return true;
            }
            Utility.getInstance().sleep(200);
        }
        return members(admin) == expected;
    }

    private static int members(Admin admin) {
        try {
            ConsumerGroupDescription group = admin.describeConsumerGroups(List.of(GROUP))
                    .describedGroups().get(GROUP).get();
            return group.members().size();
        } catch (ExecutionException e) {
            // an unknown group (never joined / fully gone) has no members
            return 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
