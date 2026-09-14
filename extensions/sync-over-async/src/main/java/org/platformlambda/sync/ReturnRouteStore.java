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

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;

/**
 * Redis-backed cross-pod state, keyed by correlation-id:
 * <ul>
 *   <li><b>route</b> {@code request:{cid}} - the originating pod's return channel (TTL = rendezvous lifetime:
 *       short for a one-shot request, session-scale for a stream).</li>
 *   <li><b>queue</b> {@code queue:{cid}} - a Redis List holding the segments, the <em>source of truth</em>
 *       appended ({@code RPUSH}) before any Pub/Sub notification and drained destructively ({@code LPOP}).
 *       One store serves both patterns: a one-shot response is the degenerate stream - a queue whose first
 *       entry is terminal.</li>
 * </ul>
 * Pub/Sub is only a wake-up signal (see {@code ReturnRouteCoordinator}); correctness rests on these keys,
 * so a missed notification is recovered by a final drain - on timeout for the one-shot path, at edge idle
 * expiry for a stream. A fully drained list ceases to exist on its own; the TTLs are the crash safety net.
 */
public class ReturnRouteStore {

    private static final String ROUTE_PREFIX = "request:";
    private static final String QUEUE_PREFIX = "queue:";
    // Append + TTL refresh as ONE server-side atomic step (also a single round-trip on the hot path).
    // Two discrete commands would leave a TTL-less queue key if the client died between them - every key
    // this module creates must carry a TTL from birth so abandoned rendezvous state always ages out.
    private static final String APPEND_WITH_TTL =
            "redis.call('RPUSH', KEYS[1], ARGV[1]) return redis.call('EXPIRE', KEYS[1], ARGV[2])";

    private final RedisClusterCommands<String, String> commands;

    /**
     * @param commands the synchronous command API - the common super-type of standalone {@code RedisCommands}
     *                 and cluster {@code RedisAdvancedClusterCommands}, so one store serves both topologies.
     */
    public ReturnRouteStore(RedisClusterCommands<String, String> commands) {
        this.commands = commands;
    }

    /** Convenience for a standalone connection (single-node callers and tests). */
    public ReturnRouteStore(StatefulRedisConnection<String, String> connection) {
        this(connection.sync());
    }

    public void saveRoute(String businessCorrelationId, String returnChannel, long ttlSeconds) {
        commands.setex(ROUTE_PREFIX + businessCorrelationId, ttlSeconds, returnChannel);
    }

    /** @return the return channel for this correlation-id, or {@code null} if absent/expired (orphan). */
    public String getRoute(String businessCorrelationId) {
        return commands.get(ROUTE_PREFIX + businessCorrelationId);
    }

    /**
     * Append one serialized segment to the rendezvous queue and refresh the queue TTL, atomically
     * (store-first: call this <em>before</em> publishing the wake-up).
     */
    public void appendSegment(String businessCorrelationId, String segmentJson, long ttlSeconds) {
        commands.eval(APPEND_WITH_TTL, ScriptOutputType.INTEGER,
                new String[]{QUEUE_PREFIX + businessCorrelationId}, segmentJson, String.valueOf(ttlSeconds));
    }

    /**
     * Destructively pop the oldest queued segment. The pop is atomic, so concurrent drains cannot deliver
     * one segment twice; a duplicate wake-up simply pops nothing.
     *
     * @return the serialized segment, or {@code null} when the queue is empty (a drained list auto-deletes)
     */
    public String popSegment(String businessCorrelationId) {
        return commands.lpop(QUEUE_PREFIX + businessCorrelationId);
    }

    /** @return the number of queued segments (0 for an absent queue) - used by the drain's lost-wakeup re-check. */
    public long queueLength(String businessCorrelationId) {
        Long length = commands.llen(QUEUE_PREFIX + businessCorrelationId);
        return length == null ? 0 : length;
    }

    /**
     * Delete both keys for a completed rendezvous. The TTLs are the safety net for crashes/timeouts;
     * deleting on success frees the keys immediately instead of waiting out the TTL (less key churn).
     * The route's disappearance is also what tells every remaining producer to stop ({@code post} orphan).
     */
    public void cleanup(String businessCorrelationId) {
        // Two single-key deletes, not one two-key DEL: request:{cid} and queue:{cid} hash to different
        // slots on a Redis Cluster, so a combined DEL would fail with CROSSSLOT. On a standalone server the
        // two calls behave identically. A rendezvous whose cleanup is interrupted still ages out via each
        // key's TTL, so splitting the delete costs nothing but the extra round trip on the cold path.
        commands.del(ROUTE_PREFIX + businessCorrelationId);
        commands.del(QUEUE_PREFIX + businessCorrelationId);
    }
}
