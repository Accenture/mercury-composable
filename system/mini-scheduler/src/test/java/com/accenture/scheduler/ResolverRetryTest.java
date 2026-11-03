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
import org.platformlambda.scheduler.RetryRequest;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.platformlambda.scheduler.services.JobExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A state resolver that answers its first "expires" late, as at a cold start, is asked again with the same firing id,
 * and the job runs. The firing id rides the parameters the resolver receives as "_firing"; the job's own service
 * never sees it.
 */
class ResolverRetryTest {
    private static final String RETRY_JOB = "retry-probe";
    private static final BlockingQueue<Map<String, Object>> resolverEvents = new ArrayBlockingQueue<>(8);
    private static final BlockingQueue<Map<String, Object>> serviceBodies = new ArrayBlockingQueue<>(2);
    private static final AtomicInteger expiresCalls = new AtomicInteger();

    @BeforeAll
    static void setup() {
        AppStarter.main(new String[0]);
        TypedLambdaFunction<Map<String, Object>, Boolean> resolver = (headers, input, instance) -> {
            var event = new HashMap<>(input == null ? Map.of() : input);
            event.put("type", headers.get("type"));
            resolverEvents.add(event);
            if ("expires".equals(headers.get("type")) && expiresCalls.incrementAndGet() == 1) {
                Utility.getInstance().sleep(RetryRequest.TIMEOUT + 500);
            }
            return true;
        };
        TypedLambdaFunction<Map<String, Object>, Void> service = (headers, input, instance) -> {
            serviceBodies.add(input);
            return null;
        };
        var platform = Platform.getInstance();
        // two instances, so the retried question is answered while the first one is still being worked on
        platform.registerPrivate("retry.probe.resolver", resolver, 2);
        platform.registerPrivate("retry.probe", service, 1);
    }

    @Test
    void aLateResolverIsAskedAgainWithTheSameFiring() throws InterruptedException {
        var po = new PostOffice("unit.test", Utility.getInstance().getUuid(), "JOB " + RETRY_JOB);
        // the Quartz trigger's event: no operator, so the resolver is asked whether the job may run
        po.send(new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR).setHeader("job", RETRY_JOB));
        var events = new ArrayList<Map<String, Object>>();
        Map<String, Object> start = null;
        while (start == null) {
            var event = resolverEvents.poll(RetryRequest.TIMEOUT * 3, TimeUnit.MILLISECONDS);
            assertNotNull(event, "the resolver hears from the job: " + events);
            events.add(event);
            if ("start".equals(event.get("type"))) {
                start = event;
            }
        }
        var expires = events.stream().filter(e -> "expires".equals(e.get("type"))).toList();
        assertEquals(2, expires.size(), "the late first question is asked again");
        var firing = expires.getFirst().get(ScheduledJob.FIRING);
        assertNotNull(firing);
        assertEquals(firing, expires.get(1).get(ScheduledJob.FIRING), "a retry carries the same firing id");
        assertEquals(firing, start.get(ScheduledJob.FIRING), "the run's start carries it too");
        assertEquals("yes", start.get("probe"));
        // the job's own service receives its parameters without the system's keys
        var body = serviceBodies.poll(10, TimeUnit.SECONDS);
        assertEquals(Map.of("probe", "yes"), body);
    }

    @Test
    void aReservedParameterIsDroppedWhenTheJobIsLoaded() {
        var job = JobLoader.getJob(RETRY_JOB);
        assertNotNull(job);
        assertEquals(List.of("probe"), List.copyOf(job.parameters.keySet()));
    }
}
