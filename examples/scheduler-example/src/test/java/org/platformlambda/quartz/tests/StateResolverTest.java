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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.quartz.common.TestDatabase;
import org.platformlambda.quartz.db.SchedulerDatabase;
import org.platformlambda.quartz.services.AnotherTask;
import org.platformlambda.quartz.services.HelloWorld;
import org.platformlambda.quartz.services.MyDbTask;
import org.platformlambda.quartz.services.StateResolver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The state resolver on the test database, called directly without booting the platform: the claim in "expires",
 * the records that "start" and "end" write, and the race the claim exists for - every instance asking at the same
 * instant, with exactly one winner. The placeholder tasks are exercised alongside.
 */
class StateResolverTest {

    private static final String JOB = "unit-test-job";
    private static final String ORIGIN = "_origin";
    private static final int INSTANCES = 8;
    private static final int ROUNDS = 50;

    private final StateResolver resolver = new StateResolver();

    @BeforeAll
    static void database() {
        TestDatabase.start();
    }

    private Map<String, String> headers(String type, String name) {
        Map<String, String> headers = new HashMap<>();
        headers.put("type", type);
        if (name != null) {
            headers.put("name", name);
            headers.put("service", "hello.world");
            headers.put("schedule", "0 0/1 * 1/1 * ? *");
        }
        return headers;
    }

    private boolean expires(String name, String origin) {
        return resolver.handleEvent(headers("expires", name), Map.of("hello", "world", ORIGIN, origin), 1);
    }

    private static Document record(String name) {
        return SchedulerDatabase.jobStates().find(Filters.eq("_id", name)).first();
    }

    /** The previous run started a minute ago, so its claim has passed the window. */
    private static void expire(String name) {
        SchedulerDatabase.jobStates().updateOne(Filters.eq("_id", name),
                Updates.set("start", new Date(System.currentTimeMillis() - 60_000)));
    }

    @Test
    void startEndAndClaimLifecycle() {
        SchedulerDatabase.jobStates().deleteOne(Filters.eq("_id", JOB));
        // a job with no record is claimed by the first instance that asks
        assertTrue(expires(JOB, "instance-a"));
        // the holder asks again (a retry after a timed-out attempt) and is answered true again
        assertTrue(expires(JOB, "instance-a"));
        // a peer within the window is refused
        assertFalse(expires(JOB, "instance-b"));
        // start records the run, with the origin among the parameters
        assertTrue(resolver.handleEvent(headers("start", JOB), Map.of("hello", "world", ORIGIN, "instance-a"), 1));
        var started = record(JOB);
        assertNotNull(started);
        assertEquals("hello.world", started.getString("service"));
        assertEquals("0 0/1 * 1/1 * ? *", started.getString("schedule"));
        assertEquals("instance-a", started.get("parameters", Document.class).getString(ORIGIN));
        assertNotNull(started.getDate("start"));
        assertNull(started.get("end"));
        // end stamps the end time
        assertTrue(resolver.handleEvent(headers("end", JOB), null, 1));
        var ended = record(JOB);
        assertNotNull(ended);
        assertNotNull(ended.getDate("end"));
    }

    @Test
    void anExpiredClaimGoesToTheNextInstanceThatAsks() {
        var job = "expired-job";
        SchedulerDatabase.jobStates().deleteOne(Filters.eq("_id", job));
        assertTrue(expires(job, "instance-a"));
        expire(job);
        assertTrue(expires(job, "instance-b"));
        // the claim is instance-b's now, so the previous holder is refused
        assertFalse(expires(job, "instance-a"));
        var claimed = record(job);
        assertNotNull(claimed);
        assertEquals("instance-b", claimed.get("parameters", Document.class).getString(ORIGIN));
    }

    @Test
    void instancesAskingAtTheSameInstantGetOneWinner() throws InterruptedException, ExecutionException {
        try (var pool = Executors.newFixedThreadPool(INSTANCES)) {
            for (int round = 0; round < ROUNDS; round++) {
                var job = "race-" + round;
                SchedulerDatabase.jobStates().deleteOne(Filters.eq("_id", job));
                assertEquals(1, race(pool, job), "first run of " + job);
                expire(job);
                assertEquals(1, race(pool, job), "run of " + job + " after the previous claim expired");
            }
        }
    }

    /** Every instance asks "expires" at the same instant; the answer is how many were told to run the job. */
    private int race(ExecutorService pool, String job)
            throws InterruptedException, ExecutionException {
        var gate = new CountDownLatch(1);
        List<Future<Boolean>> answers = new ArrayList<>();
        for (int i = 0; i < INSTANCES; i++) {
            var origin = "instance-" + i;
            answers.add(pool.submit(() -> {
                gate.await();
                return expires(job, origin);
            }));
        }
        gate.countDown();
        int winners = 0;
        for (var answer : answers) {
            if (Boolean.TRUE.equals(answer.get())) {
                winners++;
            }
        }
        return winners;
    }

    @Test
    void expiresNeedsTheOrigin() {
        Map<String, String> expiresHeaders = headers("expires", JOB);
        Map<String, Object> withoutOrigin = Map.of("hello", "world");
        var e = assertThrows(IllegalArgumentException.class,
                () -> resolver.handleEvent(expiresHeaders, withoutOrigin, 1));
        assertEquals("expires needs the _origin parameter", e.getMessage());
    }

    @Test
    void invalidTypeIsRejected() {
        Map<String, String> nonsense = headers("nonsense", JOB);
        assertThrows(IllegalArgumentException.class, () -> resolver.handleEvent(nonsense, null, 1));
        // start/end without a job name falls through to the same rejection
        Map<String, String> noJobName = headers("start", null);
        assertThrows(IllegalArgumentException.class, () -> resolver.handleEvent(noJobName, null, 1));
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
