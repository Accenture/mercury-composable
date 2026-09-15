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
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.util.Utility;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The distributed cache as one composable action function — route {@code v1.cache.redis} (design spec §4.4).
 * Opt in with {@code redis.cache.enabled=true} ({@link OptionalService}); the function then registers and is
 * reachable at Layer 1 (PostOffice {@code po.request}), Layer 2 (an Event Script task with output data
 * mapping) and Layer 3 (a {@code graph.task} node) — all three are just "call a route".
 *
 * <p><b>Contract.</b> The {@code action} header selects the operation ({@link CacheAction}); the payload rides
 * in headers and the body:
 * <table border="1">
 *   <caption>per-action input / output</caption>
 *   <tr><th>action</th><th>headers</th><th>body (input)</th><th>result</th></tr>
 *   <tr><td>{@code GET}</td><td>{@code key}</td><td>—</td><td>value {@code byte[]}, or null (miss)</td></tr>
 *   <tr><td>{@code PUT}</td><td>{@code key}, {@code ttl}?</td><td>value {@code byte[]}</td><td>{@code true}</td></tr>
 *   <tr><td>{@code DELETE}</td><td>{@code key}</td><td>—</td><td>count removed ({@code long})</td></tr>
 *   <tr><td>{@code PUT_IF_NOT_PRESENT}</td><td>{@code key}, {@code ttl}?</td><td>value {@code byte[]}</td><td>{@code boolean} (stored?)</td></tr>
 *   <tr><td>{@code MGET}</td><td>—</td><td>keys {@code List<String>}</td><td>{@code Map<String,byte[]>} (misses omitted)</td></tr>
 *   <tr><td>{@code MPUT}</td><td>{@code ttl}?</td><td>entries {@code Map<String,byte[]>}</td><td>{@code true}</td></tr>
 *   <tr><td>{@code LIST_PUSH}</td><td>{@code key}, {@code ttl}?</td><td>value {@code byte[]}</td><td>new length ({@code long})</td></tr>
 *   <tr><td>{@code LIST_POP}</td><td>{@code key}</td><td>—</td><td>value {@code byte[]}, or null (empty)</td></tr>
 *   <tr><td>{@code LIST_LEN}</td><td>{@code key}</td><td>—</td><td>length ({@code long})</td></tr>
 * </table>
 *
 * <p>Values are opaque {@code byte[]} — the caller owns serialisation (a {@code String} body is accepted as a
 * UTF-8 convenience). {@code ttl} is a duration string ({@code 30s}/{@code 5m}/{@code 1h}); when omitted a
 * write uses {@code redis.cache.default.ttl}. Every worker instance ({@code redis.cache.instances}) shares
 * the one connection held by {@link CacheRuntime}.
 */
@PreLoad(route = "v1.cache.redis", instances = 20, envInstances = "redis.cache.instances")
@OptionalService("redis.cache.enabled")
public class RedisCache implements LambdaFunction {

    private static final String ACTION = "action";
    private static final String KEY = "key";
    private static final String TTL = "ttl";

    private final Supplier<RedisCacheStore> store;

    /** Instantiated reflectively by the {@code @PreLoad} scanner; resolves the shared store lazily per call. */
    public RedisCache() {
        this(CacheRuntime::store);
    }

    /** Reuse/test seam: inject the store (e.g. one built against an embedded Redis). */
    RedisCache(Supplier<RedisCacheStore> store) {
        this.store = store;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        CacheAction action = CacheAction.from(headers.get(ACTION));
        RedisCacheStore cache = store.get();
        return switch (action) {
            case GET -> cache.get(headers.get(KEY));
            case PUT -> {
                cache.put(headers.get(KEY), asBytes(input), ttl(headers, cache));
                yield Boolean.TRUE;
            }
            case DELETE -> cache.delete(headers.get(KEY));
            case PUT_IF_NOT_PRESENT -> cache.putIfAbsent(headers.get(KEY), asBytes(input), ttl(headers, cache));
            case MGET -> cache.mget(asKeyList(input));
            case MPUT -> {
                cache.mput(asEntryMap(input), ttl(headers, cache));
                yield Boolean.TRUE;
            }
            case LIST_PUSH -> cache.listPush(headers.get(KEY), asBytes(input), ttl(headers, cache));
            case LIST_POP -> cache.listPop(headers.get(KEY));
            case LIST_LEN -> cache.listLen(headers.get(KEY));
        };
    }

    /** The write-TTL: the {@code ttl} header (a duration string) when present, else the configured default. */
    private static long ttl(Map<String, String> headers, RedisCacheStore cache) {
        String ttl = headers.get(TTL);
        return ttl == null || ttl.isBlank()
                ? cache.defaultTtlSeconds()
                : Utility.getInstance().getDurationInSeconds(ttl);
    }

    /** The value payload: {@code byte[]} as-is, or a {@code String} as UTF-8 (a convenience). */
    private static byte[] asBytes(Object input) {
        if (input instanceof byte[] bytes) {
            return bytes;
        }
        if (input instanceof String text) {
            return text.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("A value (byte[] or String) is required in the body");
    }

    /** The MGET key list: any {@code List} whose elements are read as their string form. */
    private static List<String> asKeyList(Object input) {
        if (input instanceof List<?> list) {
            List<String> keys = new ArrayList<>(list.size());
            for (Object element : list) {
                if (element != null) {
                    keys.add(element.toString());
                }
            }
            return keys;
        }
        throw new IllegalArgumentException("MGET requires a List of keys in the body");
    }

    /** The MPUT entries: any {@code Map} — string keys, byte[]/String values (String as UTF-8). */
    private static Map<String, byte[]> asEntryMap(Object input) {
        if (input instanceof Map<?, ?> map) {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.put(String.valueOf(entry.getKey()), asBytes(entry.getValue()));
            }
            return entries;
        }
        throw new IllegalArgumentException("MPUT requires a Map of key -> value in the body");
    }
}
