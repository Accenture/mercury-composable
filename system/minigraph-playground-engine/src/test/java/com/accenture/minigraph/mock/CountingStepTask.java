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
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A workflow step for suspend/resume unit tests. It counts invocations per
 * step-and-cid so a test can assert that a resumed run does NOT re-execute the
 * suspension point.
 */
@PreLoad(route = "v1.counting.step", instances = 10)
public class CountingStepTask implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final ConcurrentMap<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> BUSINESS_CIDS = new ConcurrentHashMap<>();

    public static int getCount(String step, String cid) {
        var counter = COUNTERS.get(step + ":" + cid);
        return counter == null? 0 : counter.get();
    }

    public static String getBusinessCid(String step, String cid) {
        return BUSINESS_CIDS.get(step + ":" + cid);
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var step = String.valueOf(input.get("step"));
        var cid = String.valueOf(input.get("cid"));
        // the business correlation ID injected by the platform at delivery -
        // the suspend/resume tests assert it matches the caller's X-Correlation-Id
        var myCid = headers.get("my_correlation_id");
        if (myCid != null) {
            BUSINESS_CIDS.put(step + ":" + cid, myCid);
        }
        var count = COUNTERS.computeIfAbsent(step + ":" + cid, k -> new AtomicInteger()).incrementAndGet();
        var result = new HashMap<String, Object>();
        result.put("step", step);
        result.put("count", count);
        if (input.containsKey("prior")) {
            result.put("prior", input.get("prior"));
        }
        return result;
    }
}
