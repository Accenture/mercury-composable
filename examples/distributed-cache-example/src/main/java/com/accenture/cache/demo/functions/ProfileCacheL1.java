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

package com.accenture.cache.demo.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Layer 1 (Platform Core / PostOffice).</b> The whole profile CRUD in one event-driven function: it is
 * bound directly to {@code /api/l1/profile/{profile_id}} in {@code rest.yaml} (no flow, no graph) and drives
 * the shared cache with the PostOffice RPC API in code. This is the event-driven surface - the orchestration
 * IS the function body.
 *
 * <ul>
 *   <li><b>GET</b> - read the value; a miss (null body from {@code v1.cache.redis}) is HTTP 404.</li>
 *   <li><b>POST</b> - hold the JSON body (a Map) in an {@link EventEnvelope}, store its bytes, return 201.</li>
 *   <li><b>DELETE</b> - evict the key and report whether anything was removed.</li>
 * </ul>
 *
 * The {@link PostOffice} is built from the request headers, so the cache RPC inherits this request's trace -
 * the span / parent-span chain stays continuous across the L1 -&gt; {@code v1.cache.redis} hop - and
 * {@code updateContext} adds a {@code layer} field to the app-context log. The key is the raw profile id
 * ({@code redis.cache.key.prefix=cache-demo:} namespaces it), and the value is the EventEnvelope wire format
 * shared with Layers 2 and 3, so a profile written here reads back through the other two layers unchanged.
 */
@PreLoad(route = "v1.profile.l1", instances = 20)
public class ProfileCacheL1 implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    private static final String CACHE = "v1.cache.redis";
    private static final String ACTION = "action";
    private static final String KEY = "key";
    private static final long TIMEOUT = 5000;

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws Exception {
        String method = input.getMethod() == null ? "" : input.getMethod().toUpperCase();
        String id = input.getPathParameter("profile_id");
        if (id == null || id.isBlank()) {
            throw new AppException(400, "Missing profile_id");
        }
        PostOffice po = new PostOffice(headers, instance);
        po.updateContext("layer", "1");   // app-context logging: tag this request's log with its layer
        return switch (method) {
            case "GET" -> get(po, id);
            case "POST" -> post(po, id, input.getBody());
            case "DELETE" -> delete(po, id);
            default -> throw new AppException(405, "Method not allowed: " + method);
        };
    }

    private Object get(PostOffice po, String id) throws Exception {
        EventEnvelope res = po.request(new EventEnvelope().setTo(CACHE)
                .setHeader(ACTION, "GET").setHeader(KEY, id), TIMEOUT).get();
        if (res.getBody() instanceof byte[] bytes && bytes.length > 0) {
            return EventEnvelope.of(bytes).getBody();   // the profile Map -> HTTP 200
        }
        throw new AppException(404, "Profile not found");
    }

    private Object post(PostOffice po, String id, Object profile) throws Exception {
        byte[] value = new EventEnvelope().setBody(profile).toBytes();
        po.request(new EventEnvelope().setTo(CACHE)
                .setHeader(ACTION, "PUT").setHeader(KEY, id).setBody(value), TIMEOUT).get();
        Map<String, Object> ack = new HashMap<>();
        ack.put("id", id);
        ack.put("layer", 1);
        ack.put("status", "stored");
        return new EventEnvelope().setStatus(201).setBody(ack);
    }

    private Object delete(PostOffice po, String id) throws Exception {
        EventEnvelope res = po.request(new EventEnvelope().setTo(CACHE)
                .setHeader(ACTION, "DELETE").setHeader(KEY, id), TIMEOUT).get();
        long removed = res.getBody() instanceof Number number ? number.longValue() : 0;
        Map<String, Object> ack = new HashMap<>();
        ack.put("id", id);
        ack.put("layer", 1);
        ack.put("deleted", removed > 0);
        return ack;
    }
}
