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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A caller's return route (declared by {@code return.path.routes=test.progress.*}): it records each event it
 * receives, in order, per business correlation id. One instance, so the record keeps the arrival order.
 */
@PreLoad(route = ProgressCollector.ROUTE, instances = 1)
public class ProgressCollector implements TypedLambdaFunction<Map<String, Object>, Void> {
    public static final String ROUTE = "test.progress.collector";
    private static final ConcurrentMap<String, List<Map<String, Object>>> EVENTS = new ConcurrentHashMap<>();
    private static final Object MONITOR = new Object();

    public static void record(Map<String, Object> event) {
        EVENTS.computeIfAbsent(String.valueOf(event.get("cid")), k -> new CopyOnWriteArrayList<>()).add(event);
        synchronized (MONITOR) {
            MONITOR.notifyAll();
        }
    }

    public static List<Map<String, Object>> events(String cid) {
        return new ArrayList<>(EVENTS.getOrDefault(cid, List.of()));
    }

    /**
     * Wait for a run's terminal event.
     *
     * @return the events received for the run, or what arrived before the wait expired
     */
    public static List<Map<String, Object>> awaitTerminal(String cid, long timeoutMs) throws InterruptedException {
        var deadline = System.currentTimeMillis() + timeoutMs;
        synchronized (MONITOR) {
            while (events(cid).stream().noneMatch(e -> !"progress".equals(e.get("type")))) {
                var left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    break;
                }
                MONITOR.wait(left);
            }
        }
        return events(cid);
    }

    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        record(input);
        return null;
    }
}
