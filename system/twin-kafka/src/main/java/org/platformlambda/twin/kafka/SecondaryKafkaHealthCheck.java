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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.mini.kafka.KafkaClientConfig;
import org.platformlambda.mini.kafka.KafkaHealthCheck;

import java.util.Properties;

/**
 * Health-check function for the SECONDARY Kafka cluster - the twin of minimalist-kafka's
 * {@code kafka.health}, probing the cluster configured by the secondary consumer template.
 *
 * <p>A dual-cluster bridge application lists both clusters as health dependencies:
 * <pre>
 * mandatory.health.dependencies=kafka.health, secondary.kafka.health
 * </pre>
 *
 * <p>Behavior is identical to {@code kafka.health} (single no-ACL Metadata probe, start-up
 * grace with a placeholder healthy status, HTTP 503 when unreachable). Tunables follow the
 * twin-kafka fallback convention: {@code secondary.kafka.health.timeout} and
 * {@code secondary.kafka.health.startup.grace} fall back to the {@code kafka.health.*}
 * globals, then to the built-in defaults (5s / 30s).
 */
// single instance by design (the @PreLoad default) - same KafkaConsumer confinement as kafka.health
@PreLoad(route = "secondary.kafka.health")
public class SecondaryKafkaHealthCheck extends KafkaHealthCheck {

    private static final String SERVICE_NAME = "secondary.kafka";
    private static final String CONSUMER_LOCATION = "secondary.kafka.consumer.properties";
    private static final String DEFAULT_CONSUMER = "classpath:/secondary-kafka-consumer.properties";
    private static final String TIMEOUT_KEY = "secondary.kafka.health.timeout";
    private static final String GRACE_KEY = "secondary.kafka.health.startup.grace";
    private static final String PRIMARY_TIMEOUT_KEY = "kafka.health.timeout";
    private static final String PRIMARY_GRACE_KEY = "kafka.health.startup.grace";

    public SecondaryKafkaHealthCheck() {
        this(KafkaClientConfig.consumerProperties(AppConfigReader.getInstance(),
                        CONSUMER_LOCATION, DEFAULT_CONSUMER),
             resolveDurationMs(GRACE_KEY, PRIMARY_GRACE_KEY, DEFAULT_GRACE));
    }

    /**
     * Constructor seam for tests.
     *
     * @param consumerProperties the secondary cluster's consumer client configuration
     * @param graceMs            start-up grace period in milliseconds (0 = probe immediately)
     */
    SecondaryKafkaHealthCheck(Properties consumerProperties, long graceMs) {
        super(SERVICE_NAME, consumerProperties,
              resolveDurationMs(TIMEOUT_KEY, PRIMARY_TIMEOUT_KEY, DEFAULT_TIMEOUT), graceMs);
    }
}
