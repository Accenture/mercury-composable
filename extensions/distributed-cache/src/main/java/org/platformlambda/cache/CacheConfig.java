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

import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;
import org.platformlambda.redis.RedisConfig;

/**
 * Cache tunables read from {@code application.properties}, plus the shared {@link RedisConfig} connection
 * parameters resolved from the plain {@code redis.*} namespace ({@link RedisConfig#BASE_PREFIX}) — so the
 * cache and sync-over-async ({@code soa.redis.*}) never collide, while a deployment can still point both at
 * one Redis by setting only {@code redis.*}.
 *
 * <pre>
 * redis.cache.enabled=true            # opt-in master switch (@OptionalService on the function + health check)
 * redis.cache.instances=20            # virtual-thread worker instances (function concurrency), NOT connections
 *                                     #   (@PreLoad envInstances); all share the backend's one multiplexed connection
 * redis.cache.default.ttl=1h          # default TTL applied when a PUT/MPUT/LIST_PUSH omits one
 * redis.cache.key.prefix=             # optional namespace prepended to every key (isolate apps sharing one Redis)
 * </pre>
 *
 * @param redisConfig       the shared connection parameters (host/port/auth/ssl/cluster) from {@code redis.*}
 * @param keyPrefix         prepended to every cache key ({@code redis.cache.key.prefix}); blank = no prefix
 * @param defaultTtlSeconds default TTL in seconds for writes that omit one ({@code redis.cache.default.ttl})
 */
public record CacheConfig(RedisConfig redisConfig, String keyPrefix, long defaultTtlSeconds) {

    private static final String KEY_PREFIX = "redis.cache.key.prefix";
    private static final String DEFAULT_TTL = "redis.cache.default.ttl";
    private static final String DEFAULT_TTL_VALUE = "1h";

    public static CacheConfig from(ConfigBase config) {
        Utility util = Utility.getInstance();
        RedisConfig redis = RedisConfig.from(config, RedisConfig.BASE_PREFIX);
        String keyPrefix = config.getProperty(KEY_PREFIX, "");
        long ttlSeconds = util.getDurationInSeconds(config.getProperty(DEFAULT_TTL, DEFAULT_TTL_VALUE));
        return new CacheConfig(redis, keyPrefix, ttlSeconds);
    }
}
