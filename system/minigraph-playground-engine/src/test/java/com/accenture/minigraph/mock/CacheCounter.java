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

import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Call-counting mock provider for the fetcher cache-key regression guard
 * (parity finding F6). Each call increments a counter and returns it, so a
 * test can assert exactly how many times a graph run reached the provider.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "mock.cache.counter", instances = 10)
public class CacheCounter implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    private static final AtomicLong COUNTER = new AtomicLong(0);

    public static long current() {
        return COUNTER.get();
    }

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        return Map.of("count", COUNTER.incrementAndGet());
    }
}
