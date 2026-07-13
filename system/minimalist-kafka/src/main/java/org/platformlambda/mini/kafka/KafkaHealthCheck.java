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

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Kafka health-check function for the platform's health endpoint.
 *
 * <p>Add {@code kafka.health} to {@code mandatory.health.dependencies} (or
 * {@code optional.health.dependencies}) in application.properties and the {@code /health}
 * endpoint will include the Kafka cluster status. The function follows the standard health
 * contract: {@code type=info} describes the dependency, {@code type=health} returns a status
 * map when the cluster is reachable and throws {@code AppException} when it is not.
 *
 * <p>The probe is a single Kafka <b>Metadata</b> request ({@code KafkaConsumer.listTopics})
 * issued from a dedicated consumer built from the module's consumer template - the most
 * lightweight cluster round-trip the client offers. It joins no consumer group, commits no
 * offsets, and needs no admin privileges, so it works under the most restrictive ACLs.
 *
 * <p>During application start-up the function returns a <b>placeholder healthy</b> status and
 * warms up the client in the background, so {@code /health} does not fail (or block) while the
 * Kafka client and the rest of the start-up sequence are still coming up. After the first
 * successful probe - or once the grace period ({@code kafka.health.startup.grace}, default
 * {@code 30s}) has elapsed - every check is a live probe and an unreachable cluster fails
 * {@code /health} with HTTP 503.
 */
@PreLoad(route = "kafka.health", instances = 1)
public class KafkaHealthCheck implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(KafkaHealthCheck.class);

    private static final String TYPE = "type";
    private static final String INFO = "info";
    private static final String HEALTH = "health";
    private static final String SERVICE = "service";
    private static final String HREF = "href";
    private static final String STATUS = "status";
    private static final String TOPICS = "topics";
    private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String TIMEOUT_KEY = "kafka.health.timeout";
    private static final String GRACE_KEY = "kafka.health.startup.grace";
    private static final String DEFAULT_TIMEOUT = "5s";
    private static final String DEFAULT_GRACE = "30s";
    private static final String PLACEHOLDER = "Kafka client is starting up";
    private static final String REACHABLE = "Kafka cluster is reachable";

    // virtual-thread friendly: a ReentrantLock does not pin the carrier thread like 'synchronized'.
    // The lock also serializes access to the KafkaConsumer, which is not thread-safe (the health
    // worker and the background warm-up thread would otherwise race).
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean warmingUp = new AtomicBoolean(false);
    private final Properties consumerProperties;
    private final long timeoutMs;
    private final long graceDeadline;
    private KafkaConsumer<String, byte[]> consumer;
    private volatile boolean ready = false;

    public KafkaHealthCheck() {
        this(KafkaClientConfig.consumerProperties(AppConfigReader.getInstance()),
             graceMsFromConfig());
    }

    /**
     * Constructor seam for tests and for reuse against another cluster's template.
     *
     * @param consumerProperties the Kafka consumer client configuration to probe with
     * @param graceMs            start-up grace period in milliseconds (0 = probe immediately)
     */
    KafkaHealthCheck(Properties consumerProperties, long graceMs) {
        this.consumerProperties = consumerProperties;
        var util = Utility.getInstance();
        var config = AppConfigReader.getInstance();
        this.timeoutMs = util.getDurationInSeconds(config.getProperty(TIMEOUT_KEY, DEFAULT_TIMEOUT)) * 1000L;
        this.graceDeadline = System.currentTimeMillis() + graceMs;
    }

    private static long graceMsFromConfig() {
        var util = Utility.getInstance();
        var config = AppConfigReader.getInstance();
        return util.getDurationInSeconds(config.getProperty(GRACE_KEY, DEFAULT_GRACE)) * 1000L;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
        if (INFO.equals(headers.get(TYPE))) {
            Map<String, Object> result = new HashMap<>();
            result.put(SERVICE, "kafka");
            result.put(HREF, consumerProperties.getProperty(BOOTSTRAP_SERVERS, "kafka"));
            return result;
        }
        if (HEALTH.equals(headers.get(TYPE))) {
            if (!ready && System.currentTimeMillis() < graceDeadline) {
                // let the Kafka client and the application start-up sequence complete first:
                // warm up in the background and report a placeholder healthy status meanwhile
                warmUp();
                Map<String, Object> result = new HashMap<>();
                result.put(STATUS, PLACEHOLDER);
                return result;
            }
            return probe();
        }
        throw new IllegalArgumentException("type must be info or health");
    }

    private void warmUp() {
        if (warmingUp.compareAndSet(false, true)) {
            Thread.startVirtualThread(() -> {
                try {
                    probe();
                    log.info("Kafka health check is ready");
                } catch (Exception e) {
                    // stay in placeholder mode until the grace period ends
                    warmingUp.set(false);
                    log.warn("Kafka health check warm-up pending - {}", e.getMessage());
                }
            });
        }
    }

    private Map<String, Object> probe() throws AppException {
        lock.lock();
        try {
            if (consumer == null) {
                consumer = new KafkaConsumer<>(consumerProperties);
            }
            var topics = consumer.listTopics(Duration.ofMillis(timeoutMs));
            ready = true;
            Map<String, Object> result = new HashMap<>();
            result.put(STATUS, REACHABLE);
            result.put(TOPICS, topics.size());
            result.put(HREF, consumerProperties.getProperty(BOOTSTRAP_SERVERS, "kafka"));
            return result;
        } catch (Exception e) {
            closeQuietly();
            throw new AppException(503, "Kafka cluster is not reachable - " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void closeQuietly() {
        if (consumer != null) {
            try {
                consumer.close(Duration.ofSeconds(2));
            } catch (Exception e) {
                log.debug("Ignorable error while closing Kafka health-check consumer - {}", e.getMessage());
            }
            consumer = null;
        }
    }
}
