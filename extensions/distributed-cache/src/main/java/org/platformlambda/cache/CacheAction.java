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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The bounded, cache-shaped operation set of {@code v1.cache.redis} (design spec §2, Q5). Each maps to a
 * cluster-correct Redis call through the {@code RedisBackend} seam:
 *
 * <ul>
 *   <li>{@link #PUT} — {@code SETEX} (value + TTL);</li>
 *   <li>{@link #GET} — {@code GET} (value, or null miss);</li>
 *   <li>{@link #MGET} — {@code MGET} (a cluster-safe cross-slot scatter-gather);</li>
 *   <li>{@link #MPUT} — a pipelined per-entry {@code SETEX} (TTL-preserving, non-atomic across the map —
 *       not raw {@code MSET}, which sets no TTL);</li>
 *   <li>{@link #DELETE} — {@code DEL} (count removed);</li>
 *   <li>{@link #PUT_IF_NOT_PRESENT} — atomic {@code SET key value NX EX ttl} (true if stored);</li>
 *   <li>{@link #LIST_PUSH} — atomic {@code RPUSH}+{@code EXPIRE} (new length);</li>
 *   <li>{@link #LIST_POP} — destructive {@code LPOP} (oldest value, or null);</li>
 *   <li>{@link #LIST_LEN} — {@code LLEN} (list length).</li>
 * </ul>
 *
 * {@code PING} is intentionally not here — it backs the {@code redis.health} check, not a general action.
 */
public enum CacheAction {
    PUT, GET, MGET, MPUT, DELETE, PUT_IF_NOT_PRESENT, LIST_PUSH, LIST_POP, LIST_LEN;

    /**
     * Resolve an {@code action} header (case-insensitive) to an action, with a clear error naming the
     * supported set when it does not match.
     */
    public static CacheAction from(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Missing 'action' - one of " + supported());
        }
        try {
            return CacheAction.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported action '" + action + "' - one of " + supported());
        }
    }

    private static String supported() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
