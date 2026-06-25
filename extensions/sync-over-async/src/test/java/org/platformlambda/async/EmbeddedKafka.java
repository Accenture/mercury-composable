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

package org.platformlambda.async;

import kafka.server.KafkaConfig;
import kafka.server.KafkaRaftServer;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.metadata.storage.Formatter;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Embedded single-node KRaft Kafka broker for integration tests, adapted from the {@code
 * kafka-standalone} module's {@code EmbeddedKafka}. Two changes make it safe to run inside the reactor
 * build:
 * <ul>
 *   <li><b>Dynamic ports</b> - the standalone hardcodes {@code 9092}/{@code 9093}, which would collide
 *       with a developer's local Kafka or a parallel module; this allocates free ports instead.</li>
 *   <li><b>KRaft storage formatting</b> - Kafka 4.x rejects the legacy {@code meta.properties} format, so
 *       storage is formatted with the official {@link Formatter} before {@link KafkaRaftServer#startup()}.</li>
 * </ul>
 *
 * <p>Storage lives under the fixed, cloud-native-friendly {@code /tmp/soa-kafka} (the writable transient
 * area the app owns in cloud-native deployments), distinct from the standalone's {@code /tmp/kafka-logs}.
 * It is wiped on start and removed on {@link #close()}.</p>
 */
final class EmbeddedKafka implements AutoCloseable {

    private static final int NODE_ID = 1;
    private static final String CONTROLLER_LISTENER = "CONTROLLER";
    private static final String LOG_DIR = "/tmp/soa-kafka";

    private final KafkaRaftServer server;
    private final String bootstrapServers;
    private final Path logDir;

    EmbeddedKafka() {
        try {
            int brokerPort = freePort();
            int controllerPort = freePort();
            this.bootstrapServers = "127.0.0.1:" + brokerPort;
            this.logDir = prepareLogDir();
            formatStorage(logDir);
            this.server = new KafkaRaftServer(
                    new KafkaConfig(brokerConfig(brokerPort, controllerPort, logDir)), Time.SYSTEM);
            this.server.startup();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to start embedded Kafka", e);
        }
    }

    String bootstrapServers() {
        return bootstrapServers;
    }

    /**
     * Wipe and (re)create the fixed {@code /tmp/soa-kafka} log directory so the KRaft {@link Formatter}
     * always starts from an empty, unformatted directory (a leftover from a crashed previous run would
     * otherwise make formatting fail).
     */
    private static Path prepareLogDir() throws IOException {
        Path dir = Path.of(LOG_DIR);
        cleanup(dir);
        Files.createDirectories(dir);
        return dir;
    }

    private static Properties brokerConfig(int brokerPort, int controllerPort, Path logDir) {
        Properties p = new Properties();
        p.setProperty("process.roles", "broker,controller");
        p.setProperty("node.id", Integer.toString(NODE_ID));
        p.setProperty("controller.quorum.voters", NODE_ID + "@127.0.0.1:" + controllerPort);
        p.setProperty("listeners",
                "PLAINTEXT://127.0.0.1:" + brokerPort + ",CONTROLLER://127.0.0.1:" + controllerPort);
        p.setProperty("advertised.listeners", "PLAINTEXT://127.0.0.1:" + brokerPort);
        p.setProperty("inter.broker.listener.name", "PLAINTEXT");
        p.setProperty("controller.listener.names", CONTROLLER_LISTENER);
        p.setProperty("listener.security.protocol.map", "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
        p.setProperty("log.dirs", logDir.toString());
        p.setProperty("num.partitions", "1");
        p.setProperty("offsets.topic.replication.factor", "1");
        p.setProperty("transaction.state.log.replication.factor", "1");
        p.setProperty("transaction.state.log.min.isr", "1");
        p.setProperty("share.coordinator.state.topic.replication.factor", "1");
        p.setProperty("share.coordinator.state.topic.min.isr", "1");
        p.setProperty("auto.create.topics.enable", "true");
        return p;
    }

    private static void formatStorage(Path logDir) {
        try (PrintStream quiet = new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8)) {
            new Formatter()
                    .setNodeId(NODE_ID)
                    .setClusterId(Uuid.randomUuid().toString())
                    .setDirectories(List.of(logDir.toString()))
                    .setControllerListenerName(CONTROLLER_LISTENER)
                    .setPrintStream(quiet)
                    .run();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to format embedded Kafka storage", e);
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Override
    public void close() {
        try {
            server.shutdown();
            server.awaitShutdown();
        } finally {
            cleanup(logDir);
        }
    }

    /** Recursively delete the directory and itself via Mercury's {@link Utility#cleanupDir(File)}. */
    private static void cleanup(Path dir) {
        File dirFile = dir.toFile();
        if (dirFile.exists()) {
            Utility.getInstance().cleanupDir(dirFile);
        }
    }
}
