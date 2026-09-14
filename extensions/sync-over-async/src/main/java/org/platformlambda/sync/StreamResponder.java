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

package org.platformlambda.sync;

import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.platformlambda.redis.RedisBackend;
import org.platformlambda.redis.RedisBackendFactory;
import org.platformlambda.redis.RedisConfig;

/**
 * The segment-producer side of the streaming return route: a backend service posts progressive events
 * (and the terminal {@code eof}/{@code exception}) straight to Redis, and whichever pod holds the user's
 * HTTP connection drains and renders them. Deliberately lightweight - it is constructed from the same
 * discrete {@code redis.*} parameters ({@link RedisConfig}) but needs <b>no coordinator</b>: no
 * subscriber, no return channel of its own, no {@code sync.over.async.enabled} switch — only the key
 * contract ({@code queue:{cid}} + {@code request:{cid}}).
 *
 * <pre>
 * try (var responder = new StreamResponder(RedisConfig.from(config))) {
 *     responder.post(cid, StreamSegment.DATA, null, "Hello");
 *     responder.post(cid, StreamSegment.DATA, "tokens", "{...}");
 *     boolean live = responder.post(cid, StreamSegment.EOF, null, metadata);
 *     // false = the rendezvous is over (orphan) - stop producing for that cid
 * }
 * </pre>
 *
 * <p><b>Ordering is the posting discipline (D7).</b> There is no sequence number: Redis executes each
 * connection's commands in arrival order, so a producer that requires strict ordering (the AI-chat case)
 * posts sequentially - one thread, one responder - and list order equals generation order for free.
 * Several producers on one cid (the event-notification case) interleave at segment granularity in
 * arbitrary order, by design. Concurrent threads sharing one responder are safe (Lettuce multiplexes),
 * but their relative order is then scheduling-dependent - use it only where ordering does not matter.</p>
 *
 * <p><b>Any producer may close the channel</b> by posting a terminal segment; the consumer then deletes
 * the route, and every other producer's next {@code post} returns {@code false}. A {@code false} return
 * always means stop: the consumer disconnected, timed out, crashed, or another producer already closed
 * the channel. (The just-appended segment stays queued under its TTL and simply ages out - the
 * store-first order is deliberate, so a wake-up can never precede its data.)</p>
 *
 * <p>Create one responder per application (or per ordered producer) and reuse it: each instance owns a
 * Redis client and connection.</p>
 */
public class StreamResponder implements AutoCloseable {

    /** Default queue TTL - matches the {@code sync.stream.ttl.seconds} default on the consumer side. */
    public static final long DEFAULT_TTL_SECONDS = 1800;

    private final RedisBackend<String> backend;
    private final RedisClusterCommands<String, String> commands;
    private final ReturnRouteStore store;
    private final long ttlSeconds;

    public StreamResponder(RedisConfig config) {
        this(config, DEFAULT_TTL_SECONDS);
    }

    /**
     * @param config     the discrete {@code redis.*} connection parameters (standalone or cluster, per
     *                   {@code redis.cluster.mode})
     * @param ttlSeconds queue TTL refreshed on every post (the crash safety net; align it with the
     *                   consumer side's {@code sync.stream.ttl.seconds})
     */
    public StreamResponder(RedisConfig config, long ttlSeconds) {
        this.backend = RedisBackendFactory.create(config);
        this.commands = backend.commands();
        this.store = new ReturnRouteStore(commands);
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Post one segment: append it to {@code queue:{cid}} with a TTL refresh (store-first), then wake the
     * consuming pod via its return channel. The whole producer contract is <em>post in the order you
     * mean</em> - no sequence header to stamp, no per-cid state to hold.
     *
     * @param businessCorrelationId the rendezvous cid (handed out by the consuming side)
     * @param type {@link StreamSegment#DATA}, {@link StreamSegment#EOF} or {@link StreamSegment#EXCEPTION}
     * @param name optional SSE event name ({@code null} = unnamed)
     * @param body the segment payload as text ({@code null} = empty, e.g. a bare {@code eof})
     * @return {@code true} if the rendezvous is live; {@code false} for an orphan - stop producing
     * @throws IllegalArgumentException if {@code type} is not a valid segment type
     */
    public boolean post(String businessCorrelationId, String type, String name, String body) {
        StreamSegment segment = StreamSegment.of(type, name, body);
        store.appendSegment(businessCorrelationId, segment.toJson(), ttlSeconds);
        String channel = store.getRoute(businessCorrelationId);
        if (channel == null) {
            return false;
        }
        commands.publish(channel, businessCorrelationId);
        return true;
    }

    @Override
    public void close() {
        backend.close();
    }
}
