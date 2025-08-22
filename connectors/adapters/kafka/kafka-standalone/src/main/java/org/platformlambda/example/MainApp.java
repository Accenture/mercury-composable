/*

    Copyright 2018-2025 Accenture Technology

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

import org.apache.kafka.clients.admin.AdminClient;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.system.EmbeddedKafka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) throws ExecutionException, InterruptedException {
        AppConfigReader config = AppConfigReader.getInstance();
        boolean twoServers = "true".equalsIgnoreCase(config.getProperty("dual.servers", "false"));
        if (twoServers) {
            log.info("Starting 1st standalone Kafka server");
            EmbeddedKafka kafka = new EmbeddedKafka(true);
            kafka.start();
            // wait for first server to be ready
            Properties props = new Properties();
            props.put("bootstrap.servers", "127.0.0.1:9092");
            props.put("client.id", "admin-1");
            try (AdminClient client = AdminClient.create(props)) {
                var nodes = client.describeCluster().nodes().get();
                if (nodes != null && !nodes.isEmpty()) {
                    for (var node: nodes) {
                        log.info("First Kafka server {}", node);
                    }
                    log.info("Starting 2nd standalone Kafka server");
                    EmbeddedKafka secondKafka = new EmbeddedKafka(false);
                    secondKafka.start();
                }
            }
        } else {
            log.info("Starting standalone Kafka server");
            EmbeddedKafka kafka = new EmbeddedKafka(true);
            kafka.start();
        }
    }
}
