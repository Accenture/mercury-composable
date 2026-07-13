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

import org.apache.kafka.clients.consumer.CloseOptions;
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
 * offsets, and needs no admin privileges. The Metadata request itself requires <b>no ACL</b>:
 * per the KafkaConsumer contract it returns "all topics that the user is authorized to view",
 * i.e. brokers FILTER the response by Topic Describe grants rather than rejecting the request -
 * under a fully locked-down principal the call succeeds with an empty topic list (the reported
 * topic count may be 0), and the successful round trip still proves connectivity, TLS/SASL
 * authentication, and a served API request. This graceful degradation is why the probe uses
 * the consumer Metadata API instead of AdminClient.describeCluster (gated by Cluster Describe)
 * or partitionsFor (throws TopicAuthorizationException without a grant on the named topic).
 *
 * <p>During application start-up the function returns a <b>placeholder healthy</b> status and
 * warms up the client in the background, so {@code /health} does not fail (or block) while the
 * Kafka client and the rest of the start-up sequence are still coming up. After the first
 * successful probe - or once the grace period ({@code kafka.health.startup.grace}, default
 * {@code 30s}) has elapsed - every check is a live probe and an unreachable cluster fails
 * {@code /health} with HTTP 503.
 */
// multiple workers because /health is polled concurrently (operations tooling plus the container
// platform's liveness/readiness probes): info and placeholder responses run in parallel, while the
// non-thread-safe KafkaConsumer stays protected - every probe serializes on the ReentrantLock below
@PreLoad(route = "kafka.health", instances = 5)
public class KafkaHealthCheck implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(KafkaHealthCheck.class);

    private static final String PRIMARY_SERVICE_NAME = "kafka";
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
    protected static final String DEFAULT_TIMEOUT = "5s";
    protected static final String DEFAULT_GRACE = "30s";
    private static final String PLACEHOLDER = "Kafka client is starting up";
    private static final String REACHABLE = "Kafka cluster is reachable";

    // virtual-thread friendly: a ReentrantLock does not pin the carrier thread like 'synchronized'.
    // The lock also serializes access to the KafkaConsumer, which is not thread-safe (the health
    // worker and the background warm-up thread would otherwise race).
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean warmingUp = new AtomicBoolean(false);
    private final String serviceName;
    private final Properties consumerProperties;
    private final long timeoutMs;
    private final long graceDeadline;
    private KafkaConsumer<String, byte[]> consumer;
    private volatile boolean ready = false;

    public KafkaHealthCheck() {
        this(PRIMARY_SERVICE_NAME, KafkaClientConfig.consumerProperties(AppConfigReader.getInstance()),
             resolveDurationMs(TIMEOUT_KEY, DEFAULT_TIMEOUT),
             resolveDurationMs(GRACE_KEY, DEFAULT_GRACE));
    }

    /**
     * Constructor seam for tests.
     *
     * @param consumerProperties the Kafka consumer client configuration to probe with
     * @param graceMs            start-up grace period in milliseconds (0 = probe immediately)
     */
    KafkaHealthCheck(Properties consumerProperties, long graceMs) {
        this(PRIMARY_SERVICE_NAME, consumerProperties,
             resolveDurationMs(TIMEOUT_KEY, DEFAULT_TIMEOUT), graceMs);
    }

    /**
     * Reuse seam for a library probing ANOTHER Kafka cluster (e.g. twin-kafka's
     * {@code secondary.kafka.health}): subclass with the other cluster's consumer template,
     * a distinct service name for the /health dependency list, and its own tunables.
     *
     * @param serviceName        the dependency name reported by type=info (e.g. "secondary.kafka")
     * @param consumerProperties the Kafka consumer client configuration to probe with
     * @param timeoutMs          probe timeout in milliseconds
     * @param graceMs            start-up grace period in milliseconds (0 = probe immediately)
     */
    protected KafkaHealthCheck(String serviceName, Properties consumerProperties, long timeoutMs, long graceMs) {
        this.serviceName = serviceName;
        this.consumerProperties = consumerProperties;
        this.timeoutMs = timeoutMs;
        this.graceDeadline = System.currentTimeMillis() + graceMs;
    }

    /**
     * Resolve a duration configuration key to milliseconds, consulting an optional fallback key
     * before the built-in default - the twin-kafka convention where secondary.* keys fall back
     * to the primary cluster's globals.
     *
     * @param key          the configuration key (e.g. "secondary.kafka.health.timeout")
     * @param fallbackKey  optional fallback key (e.g. "kafka.health.timeout"); null for none
     * @param defaultValue the built-in default duration (e.g. "5s")
     * @return the resolved duration in milliseconds
     */
    protected static long resolveDurationMs(String key, String fallbackKey, String defaultValue) {
        var config = AppConfigReader.getInstance();
        return resolveDurationMs(key, config.getProperty(fallbackKey, defaultValue));
    }

    /**
     * Resolve a duration configuration key to milliseconds with a built-in default.
     *
     * @param key          the configuration key (e.g. "kafka.health.timeout")
     * @param defaultValue the built-in default duration (e.g. "5s")
     * @return the resolved duration in milliseconds
     */
    protected static long resolveDurationMs(String key, String defaultValue) {
        var util = Utility.getInstance();
        var config = AppConfigReader.getInstance();
        return util.getDurationInSeconds(config.getProperty(key, defaultValue)) * 1000L;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        if (INFO.equals(headers.get(TYPE))) {
            Map<String, Object> result = new HashMap<>();
            result.put(SERVICE, serviceName);
            result.put(HREF, consumerProperties.getProperty(BOOTSTRAP_SERVERS, PRIMARY_SERVICE_NAME));
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
                    log.info("{} health check is ready", serviceName);
                } catch (Exception e) {
                    // stay in placeholder mode until the grace period ends
                    warmingUp.set(false);
                    log.warn("{} health check warm-up pending - {}", serviceName, e.getMessage());
                }
            });
        }
    }

    // S2093 (try-with-resources): the try/finally releases the ReentrantLock; the KafkaConsumer is
    // deliberately long-lived - cached across health checks and closed via closeQuietly on failure
    @SuppressWarnings("java:S2093")
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
            result.put(HREF, consumerProperties.getProperty(BOOTSTRAP_SERVERS, PRIMARY_SERVICE_NAME));
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
                consumer.close(CloseOptions.timeout(Duration.ofSeconds(2)));
            } catch (Exception e) {
                log.debug("Ignorable error while closing Kafka health-check consumer - {}", e.getMessage());
            }
            consumer = null;
        }
    }
}
