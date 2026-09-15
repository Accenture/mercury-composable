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

package org.platformlambda.cache;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.redis.RedisBackend;
import org.platformlambda.redis.RedisBackendFactory;
import org.platformlambda.redis.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Process-wide holder of the cache's <b>single shared, multiplexed</b> {@link RedisBackend} and the
 * {@link RedisCacheStore} over it. Every {@code v1.cache.redis} worker instance calls {@link #store()} and
 * shares this one connection — {@code redis.cache.instances} is worker concurrency, not a connection count
 * (design spec §4.4: no pool; Lettuce pipelines over one in-order connection).
 *
 * <p>The backend is built <b>lazily on first use</b>, re-resolving config each attempt until it connects.
 * This is deliberate (spec §6): the {@code @PreLoad} function is constructed before a credential-bootstrap
 * {@code @MainApplication} publishes the vault password, and — unlike sync-over-async, which must keep an
 * eager Pub/Sub subscriber live — a cache has nothing to maintain at start-up and <b>must not fail app
 * start-up when Redis is briefly unreachable</b>. So there is no eager autoloader: while the connection
 * cannot be built the store stays null and each call retries (a config change, e.g. a late credential, is
 * picked up); once built, Lettuce owns reconnection under it and the store is reused.
 *
 * <p>The one connection is released on shutdown through the platform's lifecycle
 * ({@link Platform#onShutdown}), registered from {@link #build()} so the cleanup is wired only when a
 * connection has actually been opened.
 */
public final class CacheRuntime {
    private static final Logger log = LoggerFactory.getLogger(CacheRuntime.class);

    /** String keys (UTF-8), opaque byte[] values — the cache's interop-friendly value type. */
    static final RedisCodec<String, byte[]> CODEC = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

    // a ReentrantLock, not 'synchronized': on Java 21 a virtual thread that blocks inside a synchronized
    // block PINS its carrier (JEP 491 lifts that only in JDK 24+; the build targets 21). Lettuce's connect
    // can block briefly, so the lazy build must not pin - matching RedisHealthProbe's lock.
    private static final ReentrantLock LOCK = new ReentrantLock();
    // AtomicReferences (not volatile object fields, Sonar S3077): the references are published safely for
    // the lock-free fast path in store() and for the shutdown hook.
    private static final AtomicReference<RedisCacheStore> STORE = new AtomicReference<>();
    private static final AtomicReference<RedisBackend<byte[]>> BACKEND = new AtomicReference<>();

    private CacheRuntime() {}

    /**
     * The shared store, built on first use and reused thereafter (double-checked locking). If the connection
     * cannot be built yet, the build exception propagates to the caller (fail-fast — a cache miss/failure is
     * the caller's concern via the flow's exception handler) and the next call retries with a fresh config.
     */
    static RedisCacheStore store() {
        RedisCacheStore current = STORE.get();
        if (current == null) {
            LOCK.lock();
            try {
                current = STORE.get();
                if (current == null) {
                    current = build();
                    STORE.set(current);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return current;
    }

    private static RedisCacheStore build() {
        CacheConfig config = CacheConfig.from(AppConfigReader.getInstance());
        RedisConfig redis = config.redisConfig();
        RedisBackend<byte[]> backend = RedisBackendFactory.create(redis, CODEC);
        BACKEND.set(backend);
        // register cleanup now that a connection exists, via the platform's shutdown lifecycle (one shared
        // JVM hook). build() runs once under the lock, so this registers exactly once.
        Platform.getInstance().onShutdown(CacheRuntime::shutdown);
        log.info("Redis cache connected (redis {}:{}, ssl={}, cluster={}, keyPrefix='{}', defaultTtl={}s)",
                redis.host(), redis.port(), redis.ssl(), backend.cluster(),
                config.keyPrefix(), config.defaultTtlSeconds());
        return new RedisCacheStore(backend, config.keyPrefix(), config.defaultTtlSeconds(), redis.timeoutMs());
    }

    /** Close the shared backend on shutdown — registered with {@link Platform#onShutdown} when it opens. */
    private static void shutdown() {
        LOCK.lock();
        try {
            RedisBackend<byte[]> backend = BACKEND.getAndSet(null);
            if (backend != null) {
                try {
                    backend.close();
                } catch (Exception e) {
                    log.debug("Ignorable error while closing the Redis cache backend - {}", e.getMessage());
                }
            }
            STORE.set(null);
        } finally {
            LOCK.unlock();
        }
    }
}
