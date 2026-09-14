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

import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.redis.RedisConfig;
import org.platformlambda.redis.RedisHealthProbe;

/**
 * The distributed cache's Redis health check — the thin {@code @PreLoad} binding of the shared
 * {@link RedisHealthProbe} to the reserved route {@code redis.health} and the plain {@code redis.*} config
 * namespace ({@link RedisConfig#BASE_PREFIX}). The route name was reserved for exactly this module when
 * sync-over-async took {@code soa.redis.health}, so the two coexist without collision.
 *
 * <p>Gated by the same {@code redis.cache.enabled} switch as the cache function, so it registers only when
 * the cache is enabled. Add {@code redis.health} to {@code mandatory.health.dependencies} (or
 * {@code optional.health.dependencies}) and {@code /health} will include the cache Redis status. The probe
 * config is resolved lazily (never in this constructor): a credential-bootstrap {@code @MainApplication}
 * publishes the vault password after this {@code @PreLoad} function is constructed — see {@link RedisHealthProbe}.
 * {@code redis.health.timeout} (default {@code 5s}) bounds the probe; {@code redis.health.startup.grace}
 * (default {@code 30s}) is the start-up placeholder window.
 */
@PreLoad(route = "redis.health", instances = 5)
@OptionalService("redis.cache.enabled")
public class CacheRedisHealthCheck extends RedisHealthProbe {

    private static final String TIMEOUT_KEY = "redis.health.timeout";
    private static final String GRACE_KEY = "redis.health.startup.grace";
    private static final String DEFAULT_TIMEOUT = "5s";
    private static final String DEFAULT_GRACE = "30s";

    /** Instantiated reflectively when the platform's {@code @PreLoad} scanner registers the route. */
    public CacheRedisHealthCheck() {
        super(() -> RedisConfig.from(AppConfigReader.getInstance(), RedisConfig.BASE_PREFIX),
                resolveDurationMs(TIMEOUT_KEY, TIMEOUT_KEY, DEFAULT_TIMEOUT),
                resolveDurationMs(GRACE_KEY, GRACE_KEY, DEFAULT_GRACE));
    }
}
