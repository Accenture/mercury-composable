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
import org.platformlambda.scheduler.ActiveEnvironment;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the v1.environment.admin function (the handler behind GET/POST /api/scheduler/environment)
 * as a service call, which is how REST automation invokes it, against the sample file-backed store.
 */
class EnvironmentAdminTest extends TestBase {
    private static final String ENVIRONMENT_ADMIN = "v1.environment.admin";

    @AfterAll
    static void cleanup() {
        Utility.getInstance().cleanupDir(new File("/tmp/scheduler-environment"));
    }

    private EventEnvelope adminRequest(String method, Object body) throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", Utility.getInstance().getUuid(), method + " /api/scheduler/environment");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod(method);
        req.setUrl("/api/scheduler/environment");
        if (body != null) {
            req.setBody(body);
        }
        return po.request(new EventEnvelope().setTo(ENVIRONMENT_ADMIN).setBody(req), RPC_TIMEOUT).get();
    }

    @SuppressWarnings("unchecked")
    private MultiLevelMap body(EventEnvelope response) {
        assertInstanceOf(Map.class, response.getBody());
        return new MultiLevelMap((Map<String, Object>) response.getBody());
    }

    @Test
    void theSwitchMovesTheActiveEnvironmentAndTheStoreKeepsIt() throws InterruptedException, ExecutionException {
        var env = ActiveEnvironment.getInstance();
        try {
            var status = body(adminRequest("GET", null));
            assertEquals("prod", status.getElement("environment"));
            assertEquals("prod", status.getElement("active"));
            assertEquals("active", status.getElement("mode"));
            assertEquals(true, status.getElement("persistent"));
            assertEquals(ActiveEnvironment.DEFAULT_STORE, status.getElement("store"));
            // the operations team moves the active environment to DR for a production maintenance window
            var moved = body(adminRequest("POST", Map.of("active", "DR", "operator", "ops.team")));
            assertEquals("DR", moved.getElement("active"));
            assertEquals("standby", moved.getElement("mode"));
            assertEquals("Active environment set to DR", moved.getElement("message"));
            assertTrue(new File("/tmp/scheduler-environment/active").exists());
            // a restart drops the memory; the store wins over the property at the next read
            env.reset();
            assertEquals("prod", env.getActive());
            assertEquals("DR", env.refresh());
            assertEquals("standby", body(adminRequest("GET", null)).getElement("mode"));
        } finally {
            adminRequest("POST", Map.of("active", "prod", "operator", "ops.team"));
        }
        assertEquals("active", body(adminRequest("GET", null)).getElement("mode"));
    }

    @Test
    void aSwitchNeedsTheActiveNameAndTheOperator() throws InterruptedException, ExecutionException {
        var response = adminRequest("POST", Map.of("active", "DR"));
        assertEquals(400, response.getStatus());
        assertEquals("Missing 'active' or 'operator' parameter in request payload", response.getError());
        assertEquals("prod", ActiveEnvironment.getInstance().getActive());
    }
}
