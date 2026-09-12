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
import org.apache.kafka.common.KafkaException;
import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Kafka health check function for the platform's health endpoint.
 *
 * <p>Add {@code kafka.health} to {@code mandatory.health.dependencies} (or
 * {@code optional.health.dependencies}) in application.properties and the {@code /health}
 * endpoint will include the Kafka cluster status. The function follows the standard health
 * contract: {@code type=info} describes the dependency, {@code type=health} returns a status
 * map when the cluster is reachable and a 503 response carrying a key-value map
 * ({@code text} + {@code status}) when it is not - the status code is what the health
 * aggregation (and Kubernetes) detects; the map is for the DevOps reader. While the
 * client configuration is still incomplete - a late credential not yet published - it returns a
 * passing {@code Waiting for Kafka connection} status instead of failing (see below).
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
 * <p>The probe follows whichever client the deployment configured: the consumer template normally, or
 * the PRODUCER template on a produce-only leg ({@code kafka.consumer.enabled=false}), where the cluster
 * grants no consumer credentials to build a probe from - see
 * {@code KafkaClientConfig.healthProbeProperties}. A bridge is healthy only when both clusters are
 * reachable, so each leg stays probeable whichever direction it runs in.
 *
 * <p>The probe's client configuration is resolved <b>lazily - when the probe client is built, and
 * again whenever a failed probe forces a rebuild</b> - never in the constructor. This function is
 * {@code @PreLoad}, so it is constructed while the platform registers routes, before any
 * {@code @MainApplication} start-up logic runs. A credential bootstrap that fetches secrets from a
 * vault and publishes them as system properties has therefore not executed yet, and a template whose
 * {@code sasl.jaas.config} interpolates such a credential would be frozen with the credential
 * missing. Every probe then fails forever with Kafka's {@code ConfigException: The OAuth
 * configuration option clientId value is required}, no matter what the environment does. Because a
 * failed probe closes the client, the next probe re-resolves the template and the check heals
 * itself as soon as the credential lands.
 *
 * <p>Until it lands, a template the client cannot even be <b>built</b> from is reported as a
 * passing {@code Waiting for Kafka connection} status rather than a failure: failing {@code /health}
 * would invite the container orchestrator to restart the pod, and a restart cannot produce the
 * missing credential. Only a real network round trip that fails - the client built, the cluster
 * unreachable - fails {@code /health}.
 *
 * <p>During application start-up the function returns a <b>placeholder healthy</b> status and
 * warms up the client in the background, so {@code /health} does not fail (or block) while the
 * Kafka client and the rest of the start-up sequence are still coming up. After the first
 * successful probe - or once the grace period ({@code kafka.health.startup.grace}, default
 * {@code 30s}) has elapsed - every check is a live probe and an unreachable cluster fails
 * the check with status 503.
 *
 * <p><b>Multiple workers.</b> {@code /health} is polled concurrently - operations tooling plus the
 * container platform's liveness/readiness probes - so the function runs several worker instances:
 * info and placeholder responses serve in parallel, while the non-thread-safe {@code KafkaConsumer}
 * stays protected because every probe serializes on the internal lock.
 *
 * <p><b>{@code @KernelThreadRunner}.</b> The Kafka consumer performs its network I/O on the CALLING
 * thread inside {@code synchronized} sections, and on Java 21 a virtual thread that blocks inside
 * {@code synchronized} pins its carrier - a probe waiting out {@code kafka.health.timeout} against an
 * unreachable cluster would pin a carrier for the full wait. Kernel threads avoid that, matching the
 * module convention for functions that drive Kafka clients ({@code SimpleKafkaNotification},
 * {@code SchemaCodec}); the warm-up probe uses the kernel-thread executor for the same reason.
 * (JDK 24's JEP 491 removes synchronized pinning, but the build targets Java 21 and field
 * installations run it.) Contrast {@code redis.health}, which deliberately stays on virtual threads:
 * Lettuce does its I/O on its own event-loop threads and the caller merely awaits a future, which
 * unmounts a virtual thread cleanly.
 */
@KernelThreadRunner
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
    private static final String TEXT = "text";
    private static final String TOPICS = "topics";
    private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String TIMEOUT_KEY = "kafka.health.timeout";
    private static final String GRACE_KEY = "kafka.health.startup.grace";
    protected static final String DEFAULT_TIMEOUT = "5s";
    protected static final String DEFAULT_GRACE = "30s";
    private static final String PLACEHOLDER = "Kafka client is starting up";
    private static final String WAITING = "Waiting for Kafka connection";
    private static final String REACHABLE = "Kafka cluster is reachable";

    // the ReentrantLock serializes access to the KafkaConsumer, which is NOT thread-safe: the health
    // workers and the background warm-up thread would otherwise race. Sequential multi-thread access
    // under external synchronization is what the consumer's contract allows.
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean warmingUp = new AtomicBoolean(false);
    private final String serviceName;
    private final Supplier<Properties> probeConfig;
    // what the current probe client was built from; also the href source - replaced on every rebuild
    private final AtomicReference<Properties> consumerProperties = new AtomicReference<>();
    private final long timeoutMs;
    private final long graceDeadline;
    private KafkaConsumer<String, byte[]> consumer;
    private volatile boolean ready = false;

    /** Instantiated reflectively when the platform's {@code @PreLoad} scanner registers the route. */
    public KafkaHealthCheck() {
        this(PRIMARY_SERVICE_NAME,
             () -> KafkaClientConfig.healthProbeProperties(AppConfigReader.getInstance()),
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
     * Reuse seam with a FIXED, pre-resolved client configuration - the config is frozen for the life
     * of the instance (the test seams build on it). Prefer the {@link Supplier} form for anything
     * resolved from application config, so a credential published late in start-up is still picked up.
     *
     * @param serviceName        the dependency name reported by type=info (e.g. "secondary.kafka")
     * @param consumerProperties the Kafka consumer client configuration to probe with
     * @param timeoutMs          probe timeout in milliseconds
     * @param graceMs            start-up grace period in milliseconds (0 = probe immediately)
     */
    protected KafkaHealthCheck(String serviceName, Properties consumerProperties, long timeoutMs, long graceMs) {
        this(serviceName, () -> consumerProperties, timeoutMs, graceMs);
    }

    /**
     * Reuse seam for a library probing ANOTHER Kafka cluster (e.g. twin-kafka's
     * {@code secondary.kafka.health}): subclass with the other cluster's probe template, a distinct
     * service name for the /health dependency list, and its own tuning keys. The supplier is invoked
     * when the probe client is built - and again on every rebuild after a failure. Therefore, a template
     * that interpolates a credential published later in the start-up sequence is resolved correctly
     * on the next probe instead of being frozen at construction time.
     *
     * <p>The supplier itself must not be null (enforced here, at construction) and must return a
     * non-null template: when the real values have not been published yet, return your best-known
     * ones - an unusable result is handled by the waiting semantics, never by returning null.
     *
     * @param serviceName the dependency name reported by type=info (e.g. "secondary.kafka")
     * @param probeConfig supplies this cluster's probe client configuration, re-invoked on every rebuild
     * @param timeoutMs   probe timeout in milliseconds
     * @param graceMs     start-up grace period in milliseconds (0 = probe immediately)
     */
    protected KafkaHealthCheck(String serviceName, Supplier<Properties> probeConfig,
                               long timeoutMs, long graceMs) {
        this.serviceName = serviceName;
        this.probeConfig = Objects.requireNonNull(probeConfig, "probeConfig supplier is required");
        this.timeoutMs = timeoutMs;
        this.graceDeadline = System.currentTimeMillis() + graceMs;
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
            result.put(HREF, href());
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
            // kernel thread, not virtual: the consumer's calling-thread I/O would pin a carrier
            Platform.getInstance().getKernelThreadExecutor().execute(() -> {
                try {
                    probe();
                    if (ready) {
                        log.info("{} health check is ready", serviceName);
                    } else {
                        // the client configuration is still incomplete - allow another warm-up attempt
                        warmingUp.set(false);
                    }
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
    private Object probe() {
        lock.lock();
        try {
            if (consumer == null) {
                // re-resolved, not cached from the constructor: a credential published by a later
                // @MainApplication bootstrap is not visible while this @PreLoad function is constructed
                Properties config = probeConfig.get();
                consumerProperties.set(config);
                consumer = buildClient(config);
                if (consumer == null) {
                    // the template is still incomplete (e.g. that credential has not landed yet):
                    // report a PASSING waiting status - failing /health would invite the container
                    // orchestrator to restart the pod, and a restart cannot produce the credential.
                    // The next probe re-resolves, so the check goes live once construction succeeds.
                    Map<String, Object> result = new HashMap<>();
                    result.put(STATUS, WAITING);
                    return result;
                }
            }
            var topics = consumer.listTopics(Duration.ofMillis(timeoutMs));
            ready = true;
            Map<String, Object> result = new HashMap<>();
            result.put(STATUS, REACHABLE);
            result.put(TOPICS, topics.size());
            result.put(HREF, href());
            return result;
        } catch (Exception e) {
            closeQuietly();
            // a genuine outage: the 503 status is what the health aggregation (and Kubernetes)
            // detects; the key-value body keeps the code visible to the DevOps reader too
            Map<String, Object> down = new HashMap<>();
            down.put(TEXT, "Kafka cluster is not reachable - " + e.getMessage());
            down.put(STATUS, 503);
            return new EventEnvelope().setStatus(503).setBody(down);
        } finally {
            lock.unlock();
        }
    }

    /**
     * The dependency's href - this cluster's bootstrap.servers. Lifecycle: {@code probeConfig} is the
     * supplier itself, assigned final in the constructor, so it always exists by the time any event
     * arrives (the platform registers the route only after construction) - what starts out null is
     * the RESOLVED template in {@code consumerProperties}, because nothing is resolved at
     * {@code @PreLoad} time by design. A {@code type=info} call that arrives before the first probe
     * therefore resolves on demand (the supplier's first invocation) and caches the answer; every
     * probe that builds a client overwrites the cache with its own fresh resolve. bootstrap.servers
     * does not depend on a late credential, so one resolve serves every info call between rebuilds.
     */
    private String href() {
        Properties config = consumerProperties.get();
        if (config == null) {
            config = probeConfig.get();
            consumerProperties.set(config);
        }
        return config.getProperty(BOOTSTRAP_SERVERS, PRIMARY_SERVICE_NAME);
    }

    /**
     * Build the probe client from a freshly resolved template - or null when the client cannot even
     * be constructed from it, i.e. the template is still incomplete (a late credential not yet
     * published). The caller reports that as the passing waiting status.
     */
    private KafkaConsumer<String, byte[]> buildClient(Properties config) {
        try {
            return new KafkaConsumer<>(config);
        } catch (KafkaException e) {
            log.warn("{} health check waiting for a usable client configuration - {}",
                    serviceName, rootCause(e));
            return null;
        }
    }

    /** The most specific reason - client construction failures arrive wrapped in a generic KafkaException. */
    private static String rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
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
