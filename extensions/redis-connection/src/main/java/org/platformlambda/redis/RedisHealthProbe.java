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

package org.platformlambda.redis;

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Reusable Redis health-check logic for the platform's {@code /health} endpoint — the shared PING probe
 * behind each module's own health route. It is <b>not</b> a {@code @PreLoad} function itself (a foundation
 * function would auto-register in every consumer under {@code web.component.scan=org.platformlambda}); each
 * module supplies a thin {@code @PreLoad} subclass fixing its route and config prefix — {@code soa.redis.health}
 * for sync-over-async, {@code redis.health} for the distributed cache — over this one probe.
 *
 * <p>The function follows the standard health contract: {@code type=info} describes the dependency,
 * {@code type=health} returns a status map when the server is reachable and a 503 response carrying a
 * key-value map ({@code text} + {@code code}) when it is not — the status code is what the health
 * aggregation (and Kubernetes) detects; the map is for the DevOps reader. While the client configuration is
 * still incomplete — a late credential not yet published, or not yet accepted by the server — it returns a
 * passing {@code Waiting for Redis connection} status instead of failing (see below).
 *
 * <p>The probe is a single Redis <b>PING</b> on a dedicated connection built from the module's discrete
 * connection parameters ({@link RedisConfig}) — the lightest round trip the protocol offers, and one
 * successful call proves connectivity, TLS, and authentication in a single request.
 *
 * <p>The probe's client configuration is resolved <b>lazily — when the probe client is built, and again
 * whenever a failed probe forces a rebuild</b> — never in the constructor. The subclass is {@code @PreLoad},
 * so it is constructed while the platform registers routes, before any {@code @MainApplication} start-up
 * logic runs. A credential bootstrap that fetches secrets from a vault and publishes them as system
 * properties has therefore not executed yet — a password resolved at construction time would be frozen as
 * missing for the life of the instance. Because a failed probe closes the client, the next probe re-resolves
 * the configuration and the check heals itself as soon as the credential lands.
 *
 * <p>Until it lands, an unusable configuration is reported as a passing {@code Waiting for Redis connection}
 * status rather than a failure: failing {@code /health} would invite the container orchestrator to restart
 * the pod, and a restart cannot produce the missing credential. "Unusable" means the client cannot even be
 * built from the resolved values (e.g. an unresolved placeholder), or the server rejects the credentials
 * ({@code NOAUTH} / {@code WRONGPASS} — the signature of a password that has not landed yet). Only a real
 * connectivity failure — connection refused or a timed-out round trip — fails {@code /health} with HTTP 503.
 *
 * <p>During application start-up the function returns a <b>placeholder healthy</b> status and warms up the
 * client in the background, so {@code /health} does not fail (or block) while the Redis client and the rest
 * of the start-up sequence are still coming up. After the first successful probe — or once the grace period
 * has elapsed — every check is a live probe.
 *
 * <p>This probe deliberately stays on virtual threads (no {@code @KernelThreadRunner}, unlike
 * {@code kafka.health}): Lettuce performs network I/O on its own event-loop threads, and the calling thread
 * merely awaits a future — which unmounts a virtual thread cleanly instead of pinning its carrier.
 */
public class RedisHealthProbe implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(RedisHealthProbe.class);

    private static final String SERVICE_NAME = "redis";
    private static final String TYPE = "type";
    private static final String INFO = "info";
    private static final String HEALTH = "health";
    private static final String SERVICE = "service";
    private static final String HREF = "href";
    private static final String STATUS = "status";
    private static final String TEXT = "text";
    private static final String CODE = "code";
    private static final String PLACEHOLDER = "Redis client is starting up";
    private static final String WAITING = "Waiting for Redis connection";
    private static final String REACHABLE = "Redis is reachable";

    // virtual-thread friendly: a ReentrantLock does not pin the carrier thread like 'synchronized'.
    // The lock also serializes access to the probe connection (the health worker and the background
    // warm-up thread would otherwise race).
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean warmingUp = new AtomicBoolean(false);
    private final Supplier<RedisConfig> probeConfig;
    // what the current probe client was built from; also the href source — replaced on every rebuild
    private final AtomicReference<RedisConfig> currentConfig = new AtomicReference<>();
    private final long timeoutMs;
    private final long graceDeadline;
    private RedisBackend<String> backend;
    private volatile boolean ready = false;

    /**
     * @param probeConfig supplies the Redis connection parameters, re-invoked on every rebuild — so a
     *                    configuration whose values are published later in the start-up sequence (e.g. a
     *                    vault-fetched password) is resolved correctly on the next probe instead of being
     *                    frozen at construction time. Must not be null, and must return a non-null config:
     *                    when the real values have not been published yet, return your best-known ones — an
     *                    unusable result is handled by the waiting semantics, never by returning null.
     * @param timeoutMs   probe timeout in milliseconds (bounds connect and command round trips)
     * @param graceMs     start-up grace period in milliseconds (0 = probe immediately)
     */
    public RedisHealthProbe(Supplier<RedisConfig> probeConfig, long timeoutMs, long graceMs) {
        this.probeConfig = Objects.requireNonNull(probeConfig, "probeConfig supplier is required");
        this.timeoutMs = timeoutMs;
        this.graceDeadline = System.currentTimeMillis() + graceMs;
    }

    /**
     * Resolve a duration configuration key to milliseconds with a built-in default, preferring the
     * module's own key and falling back to the un-prefixed one when it is absent (the same
     * backward-compatible policy {@link RedisConfig} applies to the connection keys). A subclass calls this
     * from its constructor to build the {@code timeoutMs} / {@code graceMs} arguments.
     *
     * @param key          the preferred configuration key (e.g. "soa.redis.health.timeout")
     * @param legacyKey    the legacy fallback key (e.g. "redis.health.timeout")
     * @param defaultValue the built-in default duration (e.g. "5s")
     * @return the resolved duration in milliseconds
     */
    protected static long resolveDurationMs(String key, String legacyKey, String defaultValue) {
        var util = Utility.getInstance();
        var config = AppConfigReader.getInstance();
        return util.getDurationInSeconds(config.getProperty(key, config.getProperty(legacyKey, defaultValue))) * 1000L;
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
                        // the client configuration is still incomplete — allow another warm-up attempt
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
    // deliberately long-lived — cached across health checks and closed via closeQuietly on failure
    @SuppressWarnings("java:S2093")
    private Object probe() {
        lock.lock();
        try {
            if (backend == null) {
                // re-resolved, not cached from the constructor: a credential published by a later
                // @MainApplication bootstrap is not visible while the @PreLoad subclass is constructed
                RedisConfig config = probeConfig.get();
                currentConfig.set(config);
                // the health check bounds its probe with its own timeout; the factory selects standalone or
                // cluster per cluster.mode (auto-detecting when so configured)
                backend = RedisBackendFactory.create(config.withTimeout(timeoutMs));
            }
            backend.commands().ping();
            ready = true;
            Map<String, Object> result = new HashMap<>();
            result.put(STATUS, REACHABLE);
            result.put(HREF, href());
            return result;
        } catch (Exception e) {
            closeQuietly();
            if (waitingOnConfig(e)) {
                // the configuration is not yet usable (unbuildable values, or credentials the server
                // rejects): report a PASSING waiting status — failing /health would invite the container
                // orchestrator to restart the pod, and a restart cannot produce the credential.
                // The next probe re-resolves, so the check goes live once the real values land.
                log.warn("{} health check waiting for a usable client configuration - {}",
                        SERVICE_NAME, rootCause(e));
                Map<String, Object> result = new HashMap<>();
                result.put(STATUS, WAITING);
                return result;
            }
            // a genuine outage: the 503 status is what the health aggregation (and Kubernetes)
            // detects; the key-value body keeps the code visible to the DevOps reader too
            Map<String, Object> down = new HashMap<>();
            down.put(TEXT, "Redis is not reachable - " + rootCause(e));
            down.put(CODE, 503);
            return new EventEnvelope().setStatus(503).setBody(down);
        } finally {
            lock.unlock();
        }
    }

    /**
     * The waiting-vs-outage boundary. True when the failure is a configuration that is not yet usable:
     * the client cannot be built from the resolved values (IllegalArgument/IllegalState while mapping
     * them onto a RedisURI — e.g. an unresolved placeholder port), or the server rejected the
     * credentials ({@code NOAUTH} / {@code WRONGPASS} / {@code ERR Client sent AUTH} anywhere in the
     * cause chain) — the signature of a password that has not landed yet, which a pod restart cannot
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
     * The dependency's href — the configured host:port. {@code probeConfig} is assigned final in the
     * constructor, so it always exists by the time any event arrives; what starts out null is the RESOLVED
     * config in {@code currentConfig}, because nothing is resolved at {@code @PreLoad} time by design. A
     * {@code type=info} call that arrives before the first probe therefore resolves on demand and caches the
     * answer; every probe that builds a client overwrites the cache. Host and port do not depend on a late
     * credential, so one resolve serves every info call between rebuilds.
     */
    private String href() {
        RedisConfig config = currentConfig.get();
        if (config == null) {
            config = probeConfig.get();
            currentConfig.set(config);
        }
        return config.host() + ":" + config.port();
    }

    /** The most specific reason — Lettuce wraps connect failures in generic outer exceptions. */
    private static String rootCause(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }

    private void closeQuietly() {
        if (backend != null) {
            try {
                backend.close();
            } catch (Exception e) {
                log.debug("Ignorable error while closing the Redis health-check backend - {}", e.getMessage());
            }
            backend = null;
        }
    }
}
