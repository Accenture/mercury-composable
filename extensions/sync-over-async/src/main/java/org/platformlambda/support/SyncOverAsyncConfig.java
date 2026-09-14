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
 * @param routeTtlSeconds     TTL for a one-shot {@code request:{cid}} route (should cover REST timeout + buffer)
 * @param responseTtlSeconds  TTL for a one-shot rendezvous queue (short rendezvous window)
 * @param maxPendingRequests  per-pod ceiling on in-flight synchronous requests
 * @param streamTtlSeconds    TTL for a streaming rendezvous route and queue, refreshed on every post -
 *                            session-scale (an SSE notification channel legitimately idles), the crash
 *                            safety net behind the eager deletes
 * @param maxPendingStreams   per-pod ceiling on concurrently open streams
 */
public record SyncOverAsyncConfig(
        String returnChannelPrefix,
        long routeTtlSeconds,
        long responseTtlSeconds,
        int maxPendingRequests,
        long streamTtlSeconds,
        int maxPendingStreams) {

    private static final String RETURN_CHANNEL_PREFIX = "sync.return.channel.prefix";
    private static final String ROUTE_TTL_SECONDS = "sync.route.ttl.seconds";
    private static final String RESPONSE_TTL_SECONDS = "sync.response.ttl.seconds";
    private static final String MAX_PENDING_REQUESTS = "sync.max.pending.requests";
    private static final String STREAM_TTL_SECONDS = "sync.stream.ttl.seconds";
    private static final String MAX_PENDING_STREAMS = "sync.max.pending.streams";

    private static final long DEFAULT_STREAM_TTL_SECONDS = 1800;
    private static final int DEFAULT_MAX_PENDING_STREAMS = 1000;

    /** Convenience form for the one-shot tunables, with the streaming tunables at their defaults. */
    public SyncOverAsyncConfig(String returnChannelPrefix, long routeTtlSeconds, long responseTtlSeconds,
                               int maxPendingRequests) {
        this(returnChannelPrefix, routeTtlSeconds, responseTtlSeconds, maxPendingRequests,
                DEFAULT_STREAM_TTL_SECONDS, DEFAULT_MAX_PENDING_STREAMS);
    }

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
                util.str2int(config.getProperty(MAX_PENDING_REQUESTS, "10000")),
                util.str2long(config.getProperty(STREAM_TTL_SECONDS, String.valueOf(DEFAULT_STREAM_TTL_SECONDS))),
                util.str2int(config.getProperty(MAX_PENDING_STREAMS, String.valueOf(DEFAULT_MAX_PENDING_STREAMS))));
    }
}
