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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.services.JobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {
    private static final Logger log = LoggerFactory.getLogger(SchedulerTest.class);
    private static final BlockingQueue<Map<String, Object>> bench1 = new ArrayBlockingQueue<>(2);
    private static final BlockingQueue<Map<String, Object>> bench2 = new ArrayBlockingQueue<>(2);

    @BeforeAll
    static void setup() {
        AppStarter.main(new String[0]);
        TypedLambdaFunction<Map<String, Object>, Void> f1 = (headers, input, instance) -> {
            log.info("hello.world got {} {}", headers, input);
            bench1.offer(Map.of("body", input, "headers", headers));
            return null;
        };
        TypedLambdaFunction<Map<String, Object>, Void> f2 = (headers, input, instance) -> {
            log.info("hello.flow got {} {}", headers, input);
            bench2.offer(Map.of("body", input, "headers", headers));
            return null;
        };
        Platform platform = Platform.getInstance();
        platform.registerPrivate("hello.world", f1, 1);
        platform.registerPrivate("hello.flow", f2, 1);
    }

    @AfterAll
    static void cleanup() {
        File tempFolder = new File("/tmp/scheduler-states");
        Utility.getInstance().cleanupDir(tempFolder);
    }

    @SuppressWarnings("unchecked")
    @Test
    void runJobManually() throws InterruptedException {
        var name = "demo-task";
        var uuid = Utility.getInstance().getUuid();
        var po = new PostOffice("unit.test", uuid, "JOB "+name);
        var event = new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR)
                            .setHeader("job", name).setHeader("operator", "unit.test");
        po.send(event);
        var result1 = bench1.poll(20, TimeUnit.SECONDS);
        assertNotNull(result1);
        assertTrue(result1.containsKey("body"));
        assertTrue(result1.containsKey("headers"));
        var body1 = result1.get("body");
        var headers1 = result1.get("headers");
        assertInstanceOf(Map.class, headers1);
        assertEquals(Map.of("hello", "world"), body1);
        assertEquals("demo-task", ((Map<String, String>) headers1).get("job"));
        log.info("Job executed successfully");
    }

    @SuppressWarnings("unchecked")
    @Test
    void checkScheduledJobs() throws InterruptedException {
        // check scheduled task result
        var result1 = bench1.poll(20, TimeUnit.SECONDS);
        assertNotNull(result1);
        assertTrue(result1.containsKey("body"));
        assertTrue(result1.containsKey("headers"));
        var body1 = result1.get("body");
        var headers1 = result1.get("headers");
        assertInstanceOf(Map.class, headers1);
        assertEquals(Map.of("hello", "world"), body1);
        assertEquals("demo-task", ((Map<String, String>) headers1).get("job"));
        log.info("Scheduled task executed successfully");
        // check scheduled flow result
        var result2 = bench2.poll(20, TimeUnit.SECONDS);
        assertNotNull(result2);
        assertTrue(result2.containsKey("body"));
        assertTrue(result2.containsKey("headers"));
        var body2 = result2.get("body");
        var headers2 = result2.get("headers");
        assertInstanceOf(Map.class, headers2);
        assertEquals(Map.of("hello", "flow"), body2);
        assertEquals("demo-flow", ((Map<String, String>) headers2).get("job"));
        log.info("Scheduled flow executed successfully");
    }
}
