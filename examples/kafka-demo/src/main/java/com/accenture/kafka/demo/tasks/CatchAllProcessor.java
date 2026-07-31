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

package com.accenture.kafka.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The task of {@code demo-catch-all-flow} - the DEFAULT FLOW of the second-level routing demo,
 * reached when NO routing rule matches a {@code demo.orders} record.
 *
 * <p>The body can be either shape here, so the input type is {@code Object}: a {@code Map} (a JSON
 * record that matched no rule) or the raw {@code byte[]} (a record {@code serializer: 'json'} could
 * not parse - best-effort by design, the raw bytes simply pass through). Handling both is the
 * pattern a production default handler should follow.</p>
 */
@PreLoad(route = "demo.catch.all", instances = 10)
public class CatchAllProcessor implements TypedLambdaFunction<Object, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(CatchAllProcessor.class);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Object input, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        Map<String, Object> response = new HashMap<>();
        // a raw byte[] means serializer: 'json' could not parse the record - show it as text;
        // a Map/List is a well-formed JSON record that simply matched no routing rule
        switch (input) {
            case byte[] bytes -> {
                response.put("received", new String(bytes, StandardCharsets.UTF_8));
                response.put("shape", "raw bytes (not a JSON object/array)");
            }
            case Map<?, ?> map -> {
                response.put("received", map);
                response.put("shape", "map (JSON object, no rule matched)");
            }
            case List<?> list -> {
                response.put("received", list);
                response.put("shape", "list (JSON array, no rule matched)");
            }
            case null, default -> {
                response.put("received", String.valueOf(input));
                response.put("shape", "other");
            }
        }
        log.info("Unmatched record caught by the default rule (cid={}, traceId={}): {}",
                po.getMyCorrelationId(), po.getTraceId(), response.get("shape"));
        response.put("processedBy", "demo-catch-all-flow");
        response.put("routedBy", "default");
        response.put("traceId", po.getTraceId());
        return response;
    }
}
