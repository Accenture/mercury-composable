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
import org.platformlambda.core.util.Utility;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * The work an asynchronous {@code graph.extension} starts, recorded per business correlation id: what each run received,
 * when it ended, and how many runs were active at once. A run takes {@code delay} milliseconds, so a test can see
 * that the caller was answered before the work ended.
 */
@PreLoad(route = "v1.async.recorder", instances = 20)
public class AsyncRecorder implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final ConcurrentMap<String, Runs> BY_CID = new ConcurrentHashMap<>();

    /** The runs recorded for one business correlation id. */
    public static final class Runs {
        public final List<Map<String, Object>> started = new CopyOnWriteArrayList<>();
        public final AtomicInteger ended = new AtomicInteger();
        public final AtomicInteger active = new AtomicInteger();
        public final AtomicInteger maxActive = new AtomicInteger();

        private synchronized void changed() {
            notifyAll();
        }

        /**
         * Wait until this many runs have started.
         *
         * @return false when the wait expired first
         */
        public boolean awaitStarted(int count, long timeoutMs) throws InterruptedException {
            return await(() -> started.size() >= count, timeoutMs);
        }

        /**
         * Wait until this many runs have ended.
         *
         * @return false when the wait expired first
         */
        public boolean awaitEnded(int count, long timeoutMs) throws InterruptedException {
            return await(() -> ended.get() >= count, timeoutMs);
        }

        private synchronized boolean await(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
            var deadline = System.currentTimeMillis() + timeoutMs;
            while (!condition.getAsBoolean()) {
                var left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    return false;
                }
                wait(left);
            }
            return true;
        }
    }

    public static Runs runs(String cid) {
        return BY_CID.computeIfAbsent(cid, k -> new Runs());
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var runs = runs(String.valueOf(headers.get("my_correlation_id")));
        var record = new HashMap<String, Object>(input);
        runs.started.add(record);
        runs.changed();
        var now = runs.active.incrementAndGet();
        runs.maxActive.accumulateAndGet(now, Math::max);
        var delay = Math.max(0, Utility.getInstance().str2int(String.valueOf(input.get("delay"))));
        return Mono.just(Map.of("item", String.valueOf(input.get("item"))))
                .delayElement(Duration.ofMillis(delay))
                .doOnNext(done -> {
                    runs.active.decrementAndGet();
                    runs.ended.incrementAndGet();
                    runs.changed();
                });
    }
}
