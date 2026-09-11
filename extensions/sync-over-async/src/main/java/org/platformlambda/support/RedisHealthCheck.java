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

package org.platformlambda.support;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Redis health check function for the platform's health endpoint.
 *
 * <p>Add {@code redis.health} to {@code mandatory.health.dependencies} (or
 * {@code optional.health.dependencies}) in application.properties and the {@code /health}
 * endpoint will include the Redis server status. The function follows the standard health
 * contract: {@code type=info} describes the dependency, {@code type=health} returns a status
 * map when the server is reachable and throws {@code AppException} when it is not. While the
 * client configuration is still incomplete - a late credential not yet published, or not yet
 * accepted by the server - it returns a passing {@code Waiting for Redis connection} status
 * instead of failing (see below).
 *
 * <p>The probe is a single Redis <b>PING</b> on a dedicated connection built from the module's
 * discrete {@code redis.*} startup parameters ({@link RedisConfig}) - the lightest round trip
 * the protocol offers, and one successful call proves connectivity, TLS, and authentication in
 * a single request. The {@code minigraph-state-redis} extension reads the same {@code redis.*}
 * parameters, so one {@code redis.health} covers a deployment using either or both modules.
 *
 * <p>The probe's client configuration is resolved <b>lazily - when the probe client is built,
 * and again whenever a failed probe forces a rebuild</b> - never in the constructor. This
 * function is {@code @PreLoad}, so it is constructed while the platform registers routes,
 * before any {@code @MainApplication} start-up logic runs. A credential bootstrap that fetches
 * secrets from a vault and publishes them as system properties has therefore not executed yet -
 * a {@code redis.password} resolved at construction time would be frozen as missing for the
 * life of the instance. Because a failed probe closes the client, the next probe re-resolves
 * the configuration and the check heals itself as soon as the credential lands.
 *
 * <p>Until it lands, an unusable configuration is reported as a passing
 * {@code Waiting for Redis connection} status rather than a failure: failing {@code /health}
 * would invite the container orchestrator to restart the pod, and a restart cannot produce the
 * missing credential. "Unusable" means the client cannot even be built from the resolved values
 * (e.g. an unresolved placeholder), or the server rejects the credentials
 * ({@code NOAUTH} / {@code WRONGPASS} - the signature of a password that has not landed yet).
 * Only a real connectivity failure - connection refused or a timed-out round trip - fails
 * {@code /health} with HTTP 503.
 *
 * <p>During application start-up the function returns a <b>placeholder healthy</b> status and
 * warms up the client in the background, so {@code /health} does not fail (or block) while the
 * Redis client and the rest of the start-up sequence are still coming up. After the first
 * successful probe - or once the grace period ({@code redis.health.startup.grace}, default
 * {@code 30s}) has elapsed - every check is a live probe. {@code redis.health.timeout}
 * (default {@code 5s}) bounds the probe's connect and command round trips.
 */
// multiple workers because /health is polled concurrently (operations tooling plus the container
// platform's liveness/readiness probes): info and placeholder responses run in parallel, while the
// probe connection stays protected - every probe serializes on the ReentrantLock below
@PreLoad(route = "redis.health", instances = 5)
public class RedisHealthCheck implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(RedisHealthCheck.class);

    private static final String SERVICE_NAME = "redis";
    private static final String TYPE = "type";
    private static final String INFO = "info";
    private static final String HEALTH = "health";
    private static final String SERVICE = "service";
    private static final String HREF = "href";
    private static final String STATUS = "status";
    private static final String TIMEOUT_KEY = "redis.health.timeout";
    private static final String GRACE_KEY = "redis.health.startup.grace";
    private static final String DEFAULT_TIMEOUT = "5s";
    private static final String DEFAULT_GRACE = "30s";
    private static final String PLACEHOLDER = "Redis client is starting up";
    private static final String WAITING = "Waiting for Redis connection";
    private static final String REACHABLE = "Redis is reachable";

    // virtual-thread friendly: a ReentrantLock does not pin the carrier thread like 'synchronized'.
    // The lock also serializes access to the probe connection (the health worker and the background
    // warm-up thread would otherwise race).
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean warmingUp = new AtomicBoolean(false);
    private final Supplier<RedisConfig> probeConfig;
    // what the current probe client was built from; also the href source - replaced on every rebuild
    private final AtomicReference<RedisConfig> currentConfig = new AtomicReference<>();
    private final long timeoutMs;
    private final long graceDeadline;
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private volatile boolean ready = false;

    /** Instantiated reflectively when the platform's {@code @PreLoad} scanner registers the route. */
    public RedisHealthCheck() {
        this(() -> RedisConfig.from(AppConfigReader.getInstance()),
             resolveDurationMs(TIMEOUT_KEY, DEFAULT_TIMEOUT),
             resolveDurationMs(GRACE_KEY, DEFAULT_GRACE));
    }

    /**
     * Reuse/test seam. The supplier is invoked when the probe client is built - and again on every
     * rebuild after a failure - so a configuration whose values are published later in the start-up
     * sequence (e.g. a vault-fetched {@code redis.password}) is resolved correctly on the next probe
     * instead of being frozen at construction time.
     *
     * @param probeConfig supplies the Redis connection parameters, re-invoked on every rebuild
     * @param timeoutMs   probe timeout in milliseconds (bounds connect and command round trips)
     * @param graceMs     start-up grace period in milliseconds (0 = probe immediately)
     */
    public RedisHealthCheck(Supplier<RedisConfig> probeConfig, long timeoutMs, long graceMs) {
        this.probeConfig = probeConfig;
        this.timeoutMs = timeoutMs;
        this.graceDeadline = System.currentTimeMillis() + graceMs;
    }

    /**
     * Resolve a duration configuration key to milliseconds with a built-in default.
     *
     * @param key          the configuration key (e.g. "redis.health.timeout")
     * @param defaultValue the built-in default duration (e.g. "5s")
     * @return the resolved duration in milliseconds
     */
    private static long resolveDurationMs(String key, String defaultValue) {
        var util = Utility.getInstance();
        var config = AppConfigReader.getInstance();
        return util.getDurationInSeconds(config.getProperty(key, defaultValue)) * 1000L;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        if (INFO.equals(headers.get(TYPE))) {
            Map<String, Object> result = new HashMap<>();
            result.put(SERVICE, SERVICE_NAME);
            result.put(HREF, href());
            return result;
        }
        if (HEALTH.equals(headers.get(TYPE))) {
            if (!ready && System.currentTimeMillis() < graceDeadline) {
                // let the Redis client and the application start-up sequence complete first:
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
            Platform.getInstance().getVirtualThreadExecutor().execute(() -> {
                try {
                    probe();
                    if (ready) {
                        log.info("{} health check is ready", SERVICE_NAME);
                    } else {
                        // the client configuration is still incomplete - allow another warm-up attempt
                        warmingUp.set(false);
                    }
                } catch (Exception e) {
                    // stay in placeholder mode until the grace period ends
                    warmingUp.set(false);
                    log.warn("{} health check warm-up pending - {}", SERVICE_NAME, e.getMessage());
                }
            });
        }
    }

    // S2093 (try-with-resources): the try/finally releases the ReentrantLock; the Redis connection is
    // deliberately long-lived - cached across health checks and closed via closeQuietly on failure
    @SuppressWarnings("java:S2093")
    private Map<String, Object> probe() throws AppException {
        lock.lock();
        try {
            if (connection == null) {
                // re-resolved, not cached from the constructor: a credential published by a later
                // @MainApplication bootstrap is not visible while this @PreLoad function is constructed
                RedisConfig config = probeConfig.get();
                currentConfig.set(config);
                RedisURI uri = config.toUri();
                uri.setTimeout(Duration.ofMillis(timeoutMs));
                client = RedisClient.create(uri);
                connection = client.connect();
            }
            connection.sync().ping();
            ready = true;
            Map<String, Object> result = new HashMap<>();
            result.put(STATUS, REACHABLE);
            result.put(HREF, href());
            return result;
        } catch (Exception e) {
            closeQuietly();
            if (waitingOnConfig(e)) {
                // the configuration is not yet usable (unbuildable values, or credentials the server
                // rejects): report a PASSING waiting status - failing /health would invite the container
                // orchestrator to restart the pod, and a restart cannot produce the credential.
                // The next probe re-resolves, so the check goes live once the real values land.
                log.warn("{} health check waiting for a usable client configuration - {}",
                        SERVICE_NAME, rootCause(e));
                Map<String, Object> result = new HashMap<>();
                result.put(STATUS, WAITING);
                return result;
            }
            throw new AppException(503, "Redis is not reachable - " + rootCause(e));
        } finally {
            lock.unlock();
        }
    }

    /**
     * The waiting-vs-outage boundary. True when the failure is a configuration that is not yet usable:
     * the client cannot be built from the resolved values (IllegalArgument/IllegalState while mapping
     * them onto a RedisURI - e.g. an unresolved placeholder port), or the server rejected the
     * credentials ({@code NOAUTH} / {@code WRONGPASS} / {@code ERR Client sent AUTH} anywhere in the
     * cause chain) - the signature of a password that has not landed yet, which a pod restart cannot
     * fix. Everything else (connection refused, timeout) is a genuine outage and fails /health.
     */
    static boolean waitingOnConfig(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof IllegalArgumentException || t instanceof IllegalStateException) {
                return true;
            }
            String message = t.getMessage();
            if (message != null && (message.startsWith("NOAUTH") || message.startsWith("WRONGPASS")
                    || message.startsWith("ERR Client sent AUTH"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The dependency's href - the configured host:port. Reported before the first probe too, so it
     * resolves the configuration on demand when no client has been built yet; host and port do not
     * depend on a late credential, so the first resolve's answer stays valid.
     */
    private String href() {
        RedisConfig config = currentConfig.get();
        if (config == null) {
            config = probeConfig.get();
            currentConfig.set(config);
        }
        return config.host() + ":" + config.port();
    }

    /** The most specific reason - Lettuce wraps connect failures in generic outer exceptions. */
    private static String rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }

    private void closeQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.debug("Ignorable error while closing Redis health-check connection - {}", e.getMessage());
            }
            connection = null;
        }
        if (client != null) {
            try {
                client.shutdown(Duration.ZERO, Duration.ofSeconds(2));
            } catch (Exception e) {
                log.debug("Ignorable error while shutting down Redis health-check client - {}", e.getMessage());
            }
            client = null;
        }
    }
}
