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

package org.platformlambda.automation;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.mock.MockCloud;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MinimalistHttpTest extends TestBase {

    private static final int HTTP_PORT = MINIMALIST_HTTP_PORT;
    private static final String[] INFO_SERVICE = {"/info", "info"};
    private static final String[] INFO_LIB = {"/info/lib", "lib"};
    private static final String[] INFO_ROUTES = {"/info/routes", "routes"};
    private static final String[] HEALTH_SERVICE = {"/health", "health"};
    private static final String[] ENV_SERVICE = {"/env", "env"};
    private static final String[] LIVENESSPROBE = {"/livenessprobe", "livenessprobe"};
    private static final String[][] ADMIN_ENDPOINTS = {INFO_SERVICE, INFO_LIB, INFO_ROUTES,
            HEALTH_SERVICE, ENV_SERVICE, LIVENESSPROBE};

    @SuppressWarnings("unchecked")
    @Test
    void homePageTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("Minimalist HTTP server supports these admin endpoints",
                            multi.getElement("message"));
        int n = 0;
        for (String[] service: ADMIN_ENDPOINTS) {
            assertEquals(service[0], multi.getElement("endpoints["+n+"]"));
            n++;
        }
        assertTrue(multi.exists("time"));
        assertEquals("platform-core", multi.getElement("name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void infoEndpointTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/info", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("platform-core", multi.getElement("app.name"));
        assertEquals("REST", multi.getElement("personality"));
        assertEquals(Platform.getInstance().getOrigin(), multi.getElement("origin"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void libEndpointTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/info/lib", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("platform-core", multi.getElement("app.name"));
        assertTrue(result.containsKey("library"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void healthEndpointTest() throws IOException, InterruptedException {
        MockCloud.setSimulateException(false);
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/health", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap map = new MultiLevelMap(result);
        assertEquals("UP", map.getElement("status"));
        assertEquals("fine", map.getElement("dependency[0].message"));
        assertEquals(200, map.getElement("dependency[0].status_code"));
        assertEquals("mock.connector", map.getElement("dependency[0].service"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void simulateHealthCheckFailureTest() throws IOException, InterruptedException {
        MockCloud.setSimulateException(true);
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/health", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        // failed health check is returned as HTTP-400
        assertEquals(400, response.getStatus());
        MultiLevelMap map = new MultiLevelMap(result);
        assertEquals("DOWN", map.getElement("status"));
        assertEquals("just a test", map.getElement("dependency[0].message"));
        // original status code from dependency service is preserved
        assertEquals(500, map.getElement("dependency[0].status_code"));
        assertEquals("mock.connector", map.getElement("dependency[0].service"));
        // livenessProbe is linked to health check
        response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/livenessprobe", null);
        assert response != null;
        assertInstanceOf(String.class, response.getBody());
        assertEquals(400, response.getStatus());
        assertEquals("Unhealthy. Please check '/health' endpoint.", response.getBody());
        MockCloud.setSimulateException(false);
        // try it again
        httpGet("http://127.0.0.1:"+ HTTP_PORT, "/health", null);
        response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/livenessprobe", null);
        assert response != null;
        assertInstanceOf(String.class, response.getBody());
        assertEquals("OK", response.getBody());
    }

    @Test
    void livenessEndpointTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/livenessprobe", null);
        assert response != null;
        assertEquals("OK", response.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void envEndpointTest() throws IOException, InterruptedException {
        Utility util = Utility.getInstance();
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/env", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        // normalize the map for easy retrieval using MultiLevelMap
        Map<String, Object> result = util.getFlatMap((Map<String, Object>) response.getBody());
        MultiLevelMap multi = new MultiLevelMap();
        result.forEach(multi::setElement);
        assertEquals("platform-core", multi.getElement("app.name"));
        assertInstanceOf(Map.class, multi.getElement("env"));
        assertEquals(System.getenv("PATH"), multi.getElement("env.environment.PATH"));
        // environment variables that are not found will be shown as empty string
        assertEquals("", multi.getElement("env.environment.NON_EXIST"));
        assertEquals("true", multi.getElement("env.properties.rest.automation"));
        assertEquals("true", multi.getElement("env.properties.snake.case.serialization"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pageNotExists() throws IOException, InterruptedException {
        EventEnvelope response = httpGet("http://127.0.0.1:"+ HTTP_PORT, "/no_such_page", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, result.get("status"));
        assertEquals("Resource not found", result.get("message"));
    }
}
