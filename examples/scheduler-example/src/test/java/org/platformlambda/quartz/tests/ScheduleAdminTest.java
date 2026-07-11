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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.platformlambda.quartz.common.TestBase;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the v1.schedule.admin function (the handler behind GET/POST /api/schedule endpoints)
 * as a service call, which is how REST automation invokes it.
 */
class ScheduleAdminTest extends TestBase {

    private static final String SCHEDULE_ADMIN = "v1.schedule.admin";

    @AfterAll
    static void cleanup() {
        Utility.getInstance().cleanupDir(new File("/tmp/scheduler-states"));
    }

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
        // the job executor runs asynchronously; the resolver's start record appears shortly after
        File state = new File("/tmp/scheduler-states", "demo-task");
        for (int i = 0; i < 30 && !state.exists(); i++) {
            Utility.getInstance().sleep(500);
        }
        assertTrue(state.exists(), "job executor should have recorded the start state");
        // once the state record exists, the job can be read back by name
        EventEnvelope job = adminRequest("GET", "demo-task", null);
        assertEquals(200, job.getStatus());
        MultiLevelMap detail = new MultiLevelMap((Map<String, Object>) job.getBody());
        assertEquals("demo-task", detail.getElement("name"));
        assertEquals("hello.world", detail.getElement("service"));
    }

    @Test
    void unknownJobNameIsRejected() throws InterruptedException, ExecutionException {
        EventEnvelope response = adminRequest("GET", "no-such-job", null);
        assertEquals(400, response.getStatus());
    }

    @Test
    void runWithoutOperatorIsRejected() throws InterruptedException, ExecutionException {
        EventEnvelope response = adminRequest("POST", "demo-task", Map.of("something", "else"));
        assertEquals(400, response.getStatus());
    }

    @Test
    void jobWithoutStateRecordIsNotFound() throws InterruptedException, ExecutionException {
        File state = new File("/tmp/scheduler-states", "demo-flow");
        assertTrue(!state.exists() || state.delete());
        EventEnvelope response = adminRequest("GET", "demo-flow", null);
        assertEquals(400, response.getStatus());
    }
}
