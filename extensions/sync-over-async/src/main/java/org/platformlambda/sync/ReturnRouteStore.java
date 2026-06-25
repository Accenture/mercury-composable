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

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis-backed cross-pod state, keyed by correlation-id:
 * <ul>
 *   <li><b>route</b> {@code request:{cid}} - the originating pod's return channel (TTL = route lifetime).</li>
 *   <li><b>response</b> {@code response:{cid}} - the response payload, the <em>source of truth</em>
 *       written via {@code SETEX} before any Pub/Sub notification (TTL = short rendezvous window).</li>
 * </ul>
 * Pub/Sub is only a wake-up signal (see {@code ReturnChannelSubscriber}); correctness rests on these
 * keys, so a missed notification is recovered by a final read before timeout.
 */
public class ReturnRouteStore {

    private static final String ROUTE_PREFIX = "request:";
    private static final String RESPONSE_PREFIX = "response:";

    private final StatefulRedisConnection<String, String> connection;

    public ReturnRouteStore(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    public void saveRoute(String correlationId, String returnChannel, long ttlSeconds) {
        commands().setex(ROUTE_PREFIX + correlationId, ttlSeconds, returnChannel);
    }

    /** @return the return channel for this correlation-id, or {@code null} if absent/expired (orphan). */
    public String getRoute(String correlationId) {
        return commands().get(ROUTE_PREFIX + correlationId);
    }

    public void saveResponse(String correlationId, String payload, long ttlSeconds) {
        commands().setex(RESPONSE_PREFIX + correlationId, ttlSeconds, payload);
    }

    /** @return the response payload for this correlation-id, or {@code null} if not yet written/expired. */
    public String getResponse(String correlationId) {
        return commands().get(RESPONSE_PREFIX + correlationId);
    }

    private RedisCommands<String, String> commands() {
        return connection.sync();
    }
}
