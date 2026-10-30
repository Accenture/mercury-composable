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
import org.platformlambda.scheduler.JobLoader;
import org.platformlambda.scheduler.services.JobExecutor;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A job's optional time zone: its cron statement is read in that zone, so the job keeps its wall-clock hour
 * when the region's daylight saving moves the UTC offset.
 */
class TimeZoneTest {
    private static final String EASTERN_JOB = "eastern-probe";
    private static final BlockingQueue<Map<String, String>> resolverEvents = new ArrayBlockingQueue<>(4);

    @BeforeAll
    static void setup() {
        AppStarter.main(new String[0]);
        // the service and the state resolver of the test cron's "eastern-probe" job, whose cron never matches
        TypedLambdaFunction<Map<String, Object>, Void> service = (headers, input, instance) -> null;
        TypedLambdaFunction<Map<String, Object>, Boolean> resolver = (headers, input, instance) -> {
            resolverEvents.offer(headers);
            return true;
        };
        var platform = Platform.getInstance();
        platform.registerPrivate("timezone.probe", service, 1);
        platform.registerPrivate("timezone.probe.resolver", resolver, 1);
    }

    @Test
    void aJobReadsItsCronInItsTimeZone() throws SchedulerException {
        var job = JobLoader.getJob(EASTERN_JOB);
        assertNotNull(job);
        assertEquals(ZoneId.of("America/New_York"), job.zone);
        var trigger = cronTrigger(EASTERN_JOB);
        assertEquals("America/New_York", trigger.getTimeZone().getID());
        // 09:00 in New York is 13:00 UTC under daylight saving time and 14:00 UTC under standard time
        assertEquals(Instant.parse("2099-06-01T13:00:00Z"), fireTimeAfter(trigger, "2099-06-01T00:00:00Z"));
        assertEquals(Instant.parse("2099-12-01T14:00:00Z"), fireTimeAfter(trigger, "2099-12-01T00:00:00Z"));
    }

    @Test
    void aJobWithoutATimeZoneUsesTheJvmDefault() throws SchedulerException {
        var job = JobLoader.getJob("demo-task");
        assertNotNull(job);
        assertEquals(ZoneId.systemDefault(), job.zone);
        assertEquals(job.cronSchedule, job.schedule);
        assertEquals(TimeZone.getDefault().getID(), cronTrigger("demo-task").getTimeZone().getID());
    }

    /** The resolver records the schedule for information, so a job's time zone rides after the cron statement. */
    @Test
    void theResolverRecordsTheScheduleWithItsTimeZone() throws InterruptedException {
        var po = new PostOffice("unit.test", Utility.getInstance().getUuid(), "JOB " + EASTERN_JOB);
        po.send(new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR)
                .setHeader("job", EASTERN_JOB).setHeader("operator", "unit.test"));
        Map<String, String> start = null;
        for (int i = 0; i < 2 && start == null; i++) {
            var event = resolverEvents.poll(10, TimeUnit.SECONDS);
            assertNotNull(event);
            if ("start".equals(event.get("type"))) {
                start = event;
            }
        }
        assertNotNull(start);
        assertEquals(EASTERN_JOB, start.get("name"));
        assertEquals("0 0 9 * * ? 2099 @America/New_York", start.get("schedule"));
    }

    @Test
    void anUnknownTimeZoneRejectsTheJob() throws SchedulerException {
        assertNull(JobLoader.getJob("misspelt-zone"));
        var scheduler = new StdSchedulerFactory().getScheduler();
        assertTrue(scheduler.getTriggersOfJob(JobKey.jobKey("misspelt-zone", "jobs")).isEmpty());
    }

    private static CronTrigger cronTrigger(String name) throws SchedulerException {
        // the same Quartz scheduler that JobLoader started, looked up by its default name
        var scheduler = new StdSchedulerFactory().getScheduler();
        var triggers = scheduler.getTriggersOfJob(JobKey.jobKey(name, "jobs"));
        assertEquals(1, triggers.size());
        return assertInstanceOf(CronTrigger.class, triggers.getFirst());
    }

    private static Instant fireTimeAfter(CronTrigger trigger, String after) {
        var next = trigger.getFireTimeAfter(Date.from(Instant.parse(after)));
        assertNotNull(next);
        return next.toInstant();
    }
}
