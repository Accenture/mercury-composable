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

package org.platformlambda.system;

import kafka.server.KafkaConfig;
import kafka.server.KafkaRaftServer;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.metadata.storage.Formatter;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public class EmbeddedKafka extends Thread {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedKafka.class);

    private KafkaRaftServer kafka;
    private final String serverPropPath;

    public EmbeddedKafka(boolean first) {
        serverPropPath = first? "/server.properties" : "/server2.properties";
    }

    @Override
    public void run() {
        try {
            startServer();
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } catch (Exception e) {
            log.error("Unable to start Kafka kafka - {}", e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Format storage and start the embedded broker, blocking until it is ready. Package-visible so a
     * unit test can drive the lifecycle directly; {@link #run()} wraps this and exits the JVM on failure
     * (a standalone dev server has nothing to fall back to).
     */
    void startServer() throws Exception {
        try (InputStream stream = EmbeddedKafka.class.getResourceAsStream(serverPropPath)) {
            if (stream == null) {
                throw new IllegalArgumentException(serverPropPath+" is not available as resource");
            }
            Properties p = new Properties();
            p.load(stream);
            formatStorage(p);
            kafka = new KafkaRaftServer(new KafkaConfig(p), Time.SYSTEM);
            kafka.startup();
        }
    }

    /**
     * Wipe and format the KRaft storage directory before startup. Each standalone instance is an
     * independent single-node cluster, so it is formatted with a freshly generated cluster-id on every
     * run (the log directory is recreated each start). Kafka 4.x requires the official {@link Formatter};
     * the legacy {@code meta.properties} text format (version=0/broker.id/cluster.id) is no longer valid,
     * which is why the {@code meta*.properties} resources have been removed.
     */
    private void formatStorage(Properties config) throws Exception {
        String dir = config.getProperty("log.dirs");
        if (dir == null) {
            throw new IllegalArgumentException("log.dirs is required in "+serverPropPath);
        }
        Utility util = Utility.getInstance();
        File kafkaLogs = new File(dir);
        if (kafkaLogs.exists()) {
            util.cleanupDir(kafkaLogs);
        }
        if (!kafkaLogs.exists() && !kafkaLogs.mkdirs()) {
            throw new IllegalArgumentException("Unable to create "+kafkaLogs);
        }
        int nodeId = Integer.parseInt(config.getProperty("node.id", "1"));
        String controllerListener = config.getProperty("controller.listener.names", "CONTROLLER");
        try (PrintStream quiet = new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8)) {
            new Formatter()
                    .setNodeId(nodeId)
                    .setClusterId(Uuid.randomUuid().toString())
                    .setDirectories(List.of(kafkaLogs.getAbsolutePath()))
                    .setControllerListenerName(controllerListener)
                    .setPrintStream(quiet)
                    .run();
        }
        log.info("Formatted KRaft storage at {} (node.id={})", kafkaLogs, nodeId);
    }

    /** Orderly shutdown; invoked by the JVM shutdown hook and (by an embedding process or test) directly. */
    public void shutdown() {
        log.info("Shutting down");
        if (kafka != null) {
            kafka.shutdown();
        }
    }
}
