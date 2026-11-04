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

package com.accenture.minigraph.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A function that reports progress the way {@code llm.run} does: when its caller passes {@code x-return-path}, it
 * sends two progress events there and names neither its graph nor its node. It records the return path it was given,
 * per business correlation id, so a test can see that a node without {@code progress} gets none.
 */
@PreLoad(route = "v1.progress.sender", instances = 10)
public class ProgressSender implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final ConcurrentMap<String, String> RETURN_PATHS = new ConcurrentHashMap<>();
    private static final String NONE = "none";

    public static String returnPathSeen(String cid) {
        return RETURN_PATHS.get(cid);
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        var cid = String.valueOf(headers.get("my_correlation_id"));
        var returnPath = headers.get("x-return-path");
        RETURN_PATHS.put(cid, returnPath == null? NONE : returnPath);
        var sent = 0;
        if (returnPath != null) {
            // 'burst' sends many events and returns at once, so the run ends while the relay still holds some
            var count = "burst".equals(input.get("mode"))? 5000 : 2;
            for (int i = 1; i <= count; i++) {
                var event = new LinkedHashMap<String, Object>();
                event.put("type", "progress");
                event.put("event", "step");
                event.put("seq", i);
                event.put("time", "2026-11-04T00:00:00.000Z");
                event.put("message", "step " + i);
                EventEmitter.getInstance().send(new EventEnvelope().setTo(returnPath).setBody(event));
                sent++;
            }
        }
        if ("fail".equals(input.get("mode"))) {
            throw new AppException(422, "refused by the test");
        }
        var result = Map.of("item", String.valueOf(input.get("item")), "sent", sent);
        // 'slow' outlives a short flow ttl, so the run ends on the flow's timeout after its progress was sent
        return "slow".equals(input.get("mode"))? Mono.just(result).delayElement(Duration.ofMillis(4000)) : result;
    }
}
