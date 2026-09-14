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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.redis.RedisConfig;
import org.platformlambda.redis.RedisHealthProbe;

/**
 * sync-over-async's Redis health check — the thin {@code @PreLoad} binding of the shared
 * {@link RedisHealthProbe} to this module's route ({@code soa.redis.health}) and config namespace
 * ({@code soa.redis.*}, with a plain {@code redis.*} fallback). It reports on the sync-over-async Redis
 * specifically; another Redis consumer in the same application (the distributed cache reading the plain
 * {@code redis.*} keys via {@code redis.health}, or the {@code minigraph-state-redis} extension) is a
 * separate server/config with its own health check. Both the route and the config keys carry the
 * {@code soa.} prefix deliberately so the two coexist without route or config collisions.
 *
 * <p>Add {@code soa.redis.health} to {@code mandatory.health.dependencies} (or
 * {@code optional.health.dependencies}) in {@code application.properties} and {@code /health} will include
 * the sync-over-async Redis status. The probe's config is resolved lazily (never in this constructor): the
 * function is constructed while the platform registers routes, before a credential-bootstrap
 * {@code @MainApplication} publishes the vault-fetched password — see {@link RedisHealthProbe}.
 * {@code soa.redis.health.timeout} (default {@code 5s}) bounds the probe; {@code soa.redis.health.startup.grace}
 * (default {@code 30s}) is the start-up placeholder window.
 */
// multiple workers because /health is polled concurrently (operations tooling plus the container
// platform's liveness/readiness probes): info and placeholder responses run in parallel, while the
// probe connection stays protected by the base's ReentrantLock
@PreLoad(route = "soa.redis.health", instances = 5)
public class SoaRedisHealthCheck extends RedisHealthProbe {

    private static final String TIMEOUT_KEY = "soa.redis.health.timeout";
    private static final String GRACE_KEY = "soa.redis.health.startup.grace";
    // legacy un-prefixed fallbacks (read only when the soa. key is absent) - same policy as RedisConfig
    private static final String LEGACY_TIMEOUT_KEY = "redis.health.timeout";
    private static final String LEGACY_GRACE_KEY = "redis.health.startup.grace";
    private static final String DEFAULT_TIMEOUT = "5s";
    private static final String DEFAULT_GRACE = "30s";

    /** Instantiated reflectively when the platform's {@code @PreLoad} scanner registers the route. */
    public SoaRedisHealthCheck() {
        super(() -> RedisConfig.from(AppConfigReader.getInstance(), RedisConfig.SOA_PREFIX),
                resolveDurationMs(TIMEOUT_KEY, LEGACY_TIMEOUT_KEY, DEFAULT_TIMEOUT),
                resolveDurationMs(GRACE_KEY, LEGACY_GRACE_KEY, DEFAULT_GRACE));
    }
}
