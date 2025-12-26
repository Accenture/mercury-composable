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
import org.apache.kafka.common.utils.Time;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

public class EmbeddedKafka extends Thread {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedKafka.class);

    private KafkaRaftServer kafka;
    private final String serverPropPath;
    private final String metaPropPath;

    public EmbeddedKafka(boolean first) {
        serverPropPath = first? "/server.properties" : "/server2.properties";
        metaPropPath = first? "/meta.properties" : "/meta2.properties";
    }

    @Override
    public void run() {
        try (InputStream stream = EmbeddedKafka.class.getResourceAsStream(serverPropPath)) {
            if (stream == null) {
                throw new IllegalArgumentException(serverPropPath+" is not available as resource");
            }
            InputStream md = EmbeddedKafka.class.getResourceAsStream(metaPropPath);
            if (md == null) {
                throw new IllegalArgumentException(metaPropPath+" is not available as resource");
            }
            Utility util = Utility.getInstance();
            String metadata = util.stream2str(md);
            Properties p = new Properties();
            p.load(stream);
            String dir = p.getProperty("log.dirs");
            if (dir != null) {
                File kafkaLogs = new File(dir);
                if (kafkaLogs.exists() && kafkaLogs.isDirectory()) {
                    util.cleanupDir(kafkaLogs);
                }
                if (kafkaLogs.mkdirs()) {
                    File mdFile = new File(kafkaLogs, "meta.properties");
                    util.str2file(mdFile, metadata);
                    log.info("Initialize {}", mdFile);
                }
            }
            kafka = new KafkaRaftServer(new KafkaConfig(p), Time.SYSTEM);
            kafka.startup();
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } catch (Exception e) {
            log.error("Unable to start Kafka kafka - {}", e.getMessage());
            System.exit(-1);
        }
    }

    private void shutdown() {
        // orderly shutdown kafka
        log.info("Shutting down");
        kafka.shutdown();
    }
}
