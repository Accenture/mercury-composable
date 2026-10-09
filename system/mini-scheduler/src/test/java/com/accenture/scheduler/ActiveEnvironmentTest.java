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

package com.accenture.scheduler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.ActiveEnvironment;
import org.platformlambda.scheduler.services.JobExecutor;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The active environment of an active-active deployment (RFC-0008), against an in-memory store function
 * that stands in for the application's data store.
 */
class ActiveEnvironmentTest {
    private static final ConcurrentMap<String, String> store = new ConcurrentHashMap<>();
    private static final BlockingQueue<Map<String, String>> probe = new ArrayBlockingQueue<>(4);
    private static final String PROBE_JOB = "standby-probe";

    @BeforeAll
    static void setup() {
        AppStarter.main(new String[0]);
        TypedLambdaFunction<Map<String, Object>, Object> f = (headers, input, instance) -> {
            if (ActiveEnvironment.GET.equals(headers.get(ActiveEnvironment.TYPE))) {
                return store.get("active");
            }
            if (ActiveEnvironment.SET.equals(headers.get(ActiveEnvironment.TYPE))) {
                store.put("active", headers.get(ActiveEnvironment.ENVIRONMENT_HEADER));
                store.put("operator", headers.get(ActiveEnvironment.OPERATOR));
                return true;
            }
            throw new IllegalArgumentException("type must be get or set");
        };
        Platform.getInstance().registerPrivate(ActiveEnvironment.DEFAULT_STORE, f, 1);
        // the service of the test cron's "standby-probe" job, whose cron never matches: only the test fires it
        TypedLambdaFunction<Map<String, Object>, Void> service = (headers, input, instance) -> {
            probe.offer(headers);
            return null;
        };
        Platform.getInstance().registerPrivate("standby.probe", service, 1);
    }

    /**
     * The job executor's two paths: the Quartz trigger's event (no operator) is skipped on a standby
     * instance and run on an active one; an operator's run is honoured on a standby instance.
     */
    @Test
    void standbySkipsAScheduledJobAndHonoursAnOperatorRun() throws InterruptedException {
        var env = ActiveEnvironment.getInstance();
        var po = new PostOffice("unit.test", Utility.getInstance().getUuid(), "JOB " + PROBE_JOB);
        try {
            env.activate("DR", "unit.test");
            po.send(new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR).setHeader("job", PROBE_JOB));
            assertNull(probe.poll(3, TimeUnit.SECONDS), "a standby instance skips a scheduled job");
            env.activate("prod", "unit.test");
            po.send(new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR).setHeader("job", PROBE_JOB));
            var run = probe.poll(10, TimeUnit.SECONDS);
            assertNotNull(run, "an active instance runs the scheduled job");
            assertEquals(PROBE_JOB, run.get("job"));
            env.activate("DR", "unit.test");
            po.send(new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR).setHeader("job", PROBE_JOB)
                    .setHeader("operator", "unit.test"));
            var manual = probe.poll(10, TimeUnit.SECONDS);
            assertNotNull(manual, "an operator's run is honoured on a standby instance");
            assertEquals(PROBE_JOB, manual.get("job"));
        } finally {
            env.activate("prod", "unit.test");
        }
    }

    @Test
    void anUnconfiguredInstanceIsProductionAndActive() {
        var env = ActiveEnvironment.getInstance();
        assertEquals("prod", env.getEnvironment());
        assertEquals("prod", env.getActive());
        assertTrue(env.isActive());
        assertTrue(env.isPersistent());
        assertEquals(ActiveEnvironment.DEFAULT_STORE, env.getStore());
        var status = env.status();
        assertEquals("prod", status.get("environment"));
        assertEquals("active", status.get("mode"));
        assertEquals(true, status.get("persistent"));
    }

    @Test
    void switchingTheActiveEnvironmentPutsThisInstanceOnStandby() {
        var env = ActiveEnvironment.getInstance();
        try {
            var status = env.activate("DR", "unit.test");
            assertEquals("DR", status.get("active"));
            assertEquals("standby", status.get("mode"));
            assertFalse(env.isActive());
            assertFalse(env.isActiveNow());
            // persisted first, with the operator
            assertEquals("DR", store.get("active"));
            assertEquals("unit.test", store.get("operator"));
        } finally {
            env.activate("prod", "unit.test");
        }
        assertTrue(env.isActiveNow());
    }

    @Test
    void aRestartRestoresTheActiveEnvironmentFromTheStore() {
        var env = ActiveEnvironment.getInstance();
        try {
            env.activate("DR", "unit.test");
            // a pod restart drops the memory: the property default comes back...
            env.reset();
            assertEquals("prod", env.getActive());
            // ...until the next read of the store, which wins over the property
            assertEquals("DR", env.refresh());
            assertFalse(env.isActiveNow());
        } finally {
            env.activate("prod", "unit.test");
        }
    }

    @Test
    void anEnvironmentNameIsOneWord() {
        var env = ActiveEnvironment.getInstance();
        var ex = assertThrows(IllegalArgumentException.class, () -> env.activate(" ", "unit.test"));
        assertEquals("active must be one word of letters, digits, hyphens and underscores", ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> env.activate("east coast", "unit.test"));
        assertEquals("prod", env.getActive());
    }
}
