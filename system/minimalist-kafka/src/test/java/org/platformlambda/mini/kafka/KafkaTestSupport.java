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
import org.apache.kafka.clients.admin.NewTopic;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Shared Kafka admin helper for the integration tests (the producer/consumer go through the library's own
 * publisher + flow adapter, so the wire contract - {@code key=String}, {@code value=byte[]} - is exercised
 * there, not here).
 */
final class KafkaTestSupport {

    private KafkaTestSupport() {}

    static void createTopic(String bootstrapServers, String topic)
            throws InterruptedException, ExecutionException, TimeoutException {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (Admin admin = Admin.create(p)) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get(20, TimeUnit.SECONDS);
        }
    }
}
