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
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.platformlambda.quartz.common.TestBase;
import org.platformlambda.quartz.db.SchedulerDatabase;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the v1.schedule.admin function (the handler behind GET/POST /api/schedule endpoints)
 * as a service call, which is how REST automation invokes it.
 */
class ScheduleAdminTest extends TestBase {

    private static final String SCHEDULE_ADMIN = "v1.schedule.admin";

    private EventEnvelope adminRequest(String method, String jobName, Object body)
            throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", Utility.getInstance().getUuid(),
                method + " /api/schedule");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod(method);
        req.setUrl("/api/schedule" + (jobName == null ? "" : "/" + jobName));
        if (jobName != null) {
            req.setPathParameter("name", jobName);
        }
        if (body != null) {
            req.setBody(body);
        }
        EventEnvelope request = new EventEnvelope().setTo(SCHEDULE_ADMIN).setBody(req);
        return po.request(request, RPC_TIMEOUT).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    void jobListIsReturnedWithoutAJobName() throws InterruptedException, ExecutionException {
        EventEnvelope response = adminRequest("GET", null, null);
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertNotNull(map.getElement("jobs"));
        assertEquals("GET /api/schedule/{name}", map.getElement("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void manualJobRunCreatesTheStateRecord() throws InterruptedException, ExecutionException {
        EventEnvelope response = adminRequest("POST", "demo-task", Map.of("operator", "unit.test"));
        assertEquals(200, response.getStatus());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("Job started", map.getElement("message"));
        assertEquals("demo-task", map.getElement("job"));
        // The job executor runs asynchronously: poll the admin endpoint until the run has ended
        MultiLevelMap detail = null;
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            EventEnvelope job = adminRequest("GET", "demo-task", null);
            if (job.getStatus() == 200 && job.getBody() instanceof Map<?, ?> m && m.get("end") != null) {
                detail = new MultiLevelMap((Map<String, Object>) m);
                break;
            }
            Utility.getInstance().sleep(500);
        }
        assertNotNull(detail, "the job's record should show the end of the manual run");
        assertEquals("demo-task", detail.getElement("name"));
        assertEquals("hello.world", detail.getElement("service"));
        assertNotNull(detail.getElement("start"));
        assertNotNull(detail.getElement("elapsed"));
        // the resolver stores the origin id with the parameters; the dashboard filters the system's keys out
        assertEquals("world", detail.getElement("parameters.hello"));
        assertNull(detail.getElement("parameters._origin"));
        var stored = SchedulerDatabase.jobStates().find(Filters.eq("_id", "demo-task")).first();
        assertNotNull(stored);
        assertEquals(Platform.getInstance().getOrigin(),
                stored.get("parameters", Document.class).getString("_origin"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aFlowJobShowsItsNestedParameters() throws InterruptedException, ExecutionException {
        adminRequest("POST", "demo-flow", Map.of("operator", "unit.test"));
        MultiLevelMap detail = null;
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            EventEnvelope job = adminRequest("GET", "demo-flow", null);
            if (job.getStatus() == 200 && job.getBody() instanceof Map<?, ?> m && m.get("name") != null) {
                detail = new MultiLevelMap((Map<String, Object>) m);
                break;
            }
            Utility.getInstance().sleep(500);
        }
        assertNotNull(detail, "the flow job's record should become readable after the manual run");
        assertEquals("flow://hello-flow", detail.getElement("service"));
        assertEquals("db://demo", detail.getElement("parameters.db.host"));
        assertEquals("some value", detail.getElement("parameters.other.demo"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void jobListShowsTheTimesOfEachRecordedJob() throws InterruptedException, ExecutionException {
        var jobs = SchedulerDatabase.jobStates();
        var start = new Date(System.currentTimeMillis() - 2000);
        jobs.insertOne(new Document("_id", "listed-job").append("start", start)
                .append("end", new Date()).append("parameters", new Document("hello", "world")));
        try {
            EventEnvelope response = adminRequest("GET", null, null);
            assertEquals(200, response.getStatus());
            var body = (Map<String, Object>) response.getBody();
            var listed = ((List<Map<String, Object>>) body.get("jobs")).stream()
                    .filter(j -> "listed-job".equals(j.get("name")))
                    .findFirst().orElse(null);
            assertNotNull(listed, "every record in the job_state collection is listed");
            assertNotNull(listed.get("start"));
            assertNotNull(listed.get("end"));
            assertFalse(listed.containsKey("parameters"), "the list shows the times only");
        } finally {
            jobs.deleteOne(Filters.eq("_id", "listed-job"));
        }
    }

    @Test
    void unknownJobNameIsRejected() throws InterruptedException, ExecutionException {
        EventEnvelope response = adminRequest("GET", "no-such-job", null);
        assertEquals(400, response.getStatus());
        assertEquals("Job name is invalid", response.getError());
    }

    @Test
    void runWithoutOperatorIsRejected() throws InterruptedException, ExecutionException {
        EventEnvelope response = adminRequest("POST", "demo-task", Map.of("something", "else"));
        assertEquals(400, response.getStatus());
    }

    @Test
    void jobWithoutStateRecordIsNotFound() throws InterruptedException, ExecutionException {
        // a job that the cron file declares but that has not run yet has no record
        SchedulerDatabase.jobStates().deleteOne(Filters.eq("_id", "demo-flow"));
        EventEnvelope response = adminRequest("GET", "demo-flow", null);
        assertEquals(400, response.getStatus());
        assertEquals("Job not found", response.getError());
    }
}
