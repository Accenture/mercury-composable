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
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * One element's work in a {@code for_each} fan-out, for the tests of a failing element: it answers
 * {@code {item}} for every element except the one named by {@code fail_on}, which fails with
 * {@code fail_status} (500 when absent). An element takes {@code delay} milliseconds and the failing one
 * {@code fail_delay}, so a test decides which of them ends first. The elements that started are kept per
 * business correlation id, so a test can tell that an element never ran.
 */
@PreLoad(route = "v1.iteration.outcome", instances = 20)
public class IterationOutcome implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Utility util = Utility.getInstance();
    private static final ConcurrentMap<String, Queue<String>> ELEMENTS = new ConcurrentHashMap<>();

    /**
     * The elements that started under a business correlation id.
     *
     * @param cid the business correlation id of the run
     * @return the items, in no particular order
     */
    public static Set<String> started(String cid) {
        var items = ELEMENTS.get(cid);
        return items == null? Set.of() : Set.copyOf(items);
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        var item = String.valueOf(input.get("item"));
        var cid = headers.get("my_correlation_id");
        if (cid != null) {
            ELEMENTS.computeIfAbsent(cid, k -> new ConcurrentLinkedQueue<>()).add(item);
        }
        var failing = item.equals(String.valueOf(input.get("fail_on")));
        var delay = util.str2int(String.valueOf(input.get(failing? "fail_delay" : "delay")));
        if (delay > 0) {
            util.sleep(delay);
        }
        if (failing) {
            var status = util.str2int(String.valueOf(input.get("fail_status")));
            throw new AppException(status < 400? 500 : status, "upstream failure for " + item);
        }
        return Map.of("item", item);
    }
}
