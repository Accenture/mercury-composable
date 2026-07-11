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

package org.platformlambda.quartz.tests;

import org.junit.jupiter.api.Test;
import org.platformlambda.quartz.services.AnotherTask;
import org.platformlambda.quartz.services.HelloWorld;
import org.platformlambda.quartz.services.MyDbTask;
import org.platformlambda.quartz.services.StateResolver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The state resolver keeps one file per job under /tmp/scheduler-states - directly testable
 * without booting the platform. The placeholder tasks are exercised alongside.
 */
class StateResolverTest {

    private static final String TEMP_FOLDER = "/tmp/scheduler-states";
    private static final String JOB = "unit-test-job";

    private final StateResolver resolver = new StateResolver();

    private Map<String, String> headers(String type, String name) {
        Map<String, String> headers = new HashMap<>();
        headers.put("type", type);
        if (name != null) {
            headers.put("name", name);
            headers.put("service", "hello.world");
            headers.put("schedule", "0/15 0/1 * 1/1 * ? *");
        }
        return headers;
    }

    @Test
    void startEndAndExpiryLifecycle() {
        File state = new File(TEMP_FOLDER, JOB);
        assertTrue(!state.exists() || state.delete());
        // a job with no state file is good to go
        assertTrue(resolver.handleEvent(headers("expires", JOB), null, 1));
        // start creates the state file
        assertTrue(resolver.handleEvent(headers("start", JOB), Map.of("hello", "world"), 1));
        assertTrue(state.exists());
        // a fresh start is not expired (10s window)
        assertFalse(resolver.handleEvent(headers("expires", JOB), null, 1));
        // end stamps the end time and elapsed duration
        assertTrue(resolver.handleEvent(headers("end", JOB), null, 1));
        assertTrue(state.delete());
    }

    @Test
    void invalidTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                resolver.handleEvent(headers("nonsense", JOB), null, 1));
        // start/end without a job name falls through to the same rejection
        assertThrows(IllegalArgumentException.class, () ->
                resolver.handleEvent(headers("start", null), null, 1));
    }

    @Test
    void placeholderTasks() {
        Map<String, Object> input = Map.of("hello", "world");
        Map<String, String> headers = Map.of("job", JOB);
        assertNull(new HelloWorld().handleEvent(headers, input, 1));
        assertNull(new AnotherTask().handleEvent(headers, input, 1));
        // my.db.task echoes its input
        assertEquals(input, new MyDbTask().handleEvent(headers, input, 1));
    }
}
