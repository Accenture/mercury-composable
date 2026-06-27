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

import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;

/**
 * Tunables for the return-route mechanism, read from {@code application.properties} via {@link #from}
 * (the per-request REST timeout is supplied separately, from the {@code rest.yaml} entry).
 *
 * @param returnChannelPrefix prefix for the per-pod Pub/Sub return channel, e.g. {@code svc-return}
 * @param routeTtlSeconds     TTL for {@code request:{cid}} (should cover REST timeout + buffer)
 * @param responseTtlSeconds  TTL for {@code response:{cid}} (short rendezvous window)
 * @param maxPendingRequests  per-pod ceiling on in-flight synchronous requests
 */
public record SyncOverAsyncConfig(
        String returnChannelPrefix,
        long routeTtlSeconds,
        long responseTtlSeconds,
        int maxPendingRequests) {

    private static final String RETURN_CHANNEL_PREFIX = "sync.return.channel.prefix";
    private static final String ROUTE_TTL_SECONDS = "sync.route.ttl.seconds";
    private static final String RESPONSE_TTL_SECONDS = "sync.response.ttl.seconds";
    private static final String MAX_PENDING_REQUESTS = "sync.max.pending.requests";

    public static SyncOverAsyncConfig defaults() {
        return new SyncOverAsyncConfig("svc-return", 90, 30, 10_000);
    }

    /** Build from configuration, falling back to {@link #defaults()} values for any unset key. */
    public static SyncOverAsyncConfig from(ConfigBase config) {
        Utility util = Utility.getInstance();
        return new SyncOverAsyncConfig(
                config.getProperty(RETURN_CHANNEL_PREFIX, "svc-return"),
                util.str2long(config.getProperty(ROUTE_TTL_SECONDS, "90")),
                util.str2long(config.getProperty(RESPONSE_TTL_SECONDS, "30")),
                util.str2int(config.getProperty(MAX_PENDING_REQUESTS, "10000")));
    }
}
