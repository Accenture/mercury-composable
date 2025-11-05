/*

    Copyright 2018-2025 Accenture Technology

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

package com.accenture.scheduler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchedulerTest {
    private static final Logger log = LoggerFactory.getLogger(SchedulerTest.class);

    @BeforeAll
    static void setup() {
        AppStarter.main(new String[0]);
    }

    @Test
    void hello() throws InterruptedException {
        final BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        TypedLambdaFunction<Map<String, Object>, Void> f = (headers, input, instance) -> {
            log.info("{} {}", headers, input);
            bench.offer(input);
            return null;
        };
        Platform platform = Platform.getInstance();
        platform.registerPrivate("hello.world", f, 1);
        var result = bench.poll(20, TimeUnit.SECONDS);
        assertEquals(Map.of("hello", "world"), result);
        log.info("Scheduled task executed successfully");
    }
}
