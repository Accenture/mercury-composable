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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdminEndpointTest extends TestBase {

    @SuppressWarnings("unchecked")
    @Test
    void infoEndpointTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/info", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("platform-core", multi.getElement("app.name"));
        assertEquals("REST", multi.getElement("personality"));
        String origin = Platform.getInstance().getOrigin();
        assertEquals(origin, multi.getElement("origin"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void infoEndpointXmlTest() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/xml");
        EventEnvelope response = httpGet(localHost, "/info", headers);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("platform-core", multi.getElement("app.name"));
        assertEquals("REST", multi.getElement("personality"));
        String origin = Platform.getInstance().getOrigin();
        assertEquals(origin, multi.getElement("origin"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void libEndpointTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/info/lib", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals("platform-core", multi.getElement("app.name"));
        assertTrue(result.containsKey("library"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void routeEndpointTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/info/routes", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertInstanceOf(Map.class, result.get("routing"));
        Map<String, Object> routing = (Map<String, Object>) result.get("routing");
        assertTrue(routing.containsKey("public"));
        assertTrue(routing.containsKey("private"));
        Map<String, Object> publicRoutes = (Map<String, Object>) routing.get("public");
        Map<String, Object> privateRoutes = (Map<String, Object>) routing.get("private");
        assertTrue(privateRoutes.containsKey("routes.actuator.service"));
        assertTrue(publicRoutes.containsKey("hello.mock"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void healthEndpointTest() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/health", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        MultiLevelMap map = new MultiLevelMap(result);
        assertEquals("UP", map.getElement("status"));
        assertEquals("fine", map.getElement("dependency[0].message"));
        assertEquals(200, map.getElement("dependency[0].status_code"));
        assertEquals("mock.connector", map.getElement("dependency[0].service"));
        // livenessProbe is linked to health check
        response = httpGet(localHost, "/livenessprobe", null);
        assert response != null;
        assertEquals("OK", response.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void simulateHealthCheckFailureTest() throws IOException, InterruptedException {
        MockCloud.setSimulateException(true);
        EventEnvelope response = httpGet(localHost, "/health", null);
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
        response = httpGet(localHost, "/livenessprobe", null);
        assert response != null;
        assertEquals(400, response.getStatus());
        assertEquals("Unhealthy. Please check '/health' endpoint.", response.getBody());
        MockCloud.setSimulateException(false);
        // try it again
        httpGet(localHost, "/health", null);
        response = httpGet(localHost, "/livenessprobe", null);
        assert response != null;
        assertEquals("OK", response.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void envEndpointTest() throws IOException, InterruptedException {
        Utility util = Utility.getInstance();
        EventEnvelope response = httpGet(localHost, "/env", null);
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

    @Test
    void getIndexPage() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/index.html", null);
        assert response != null;
        assertEquals("text/html", response.getHeader("content-type"));
        assertNull(response.getHeader("ETag"));
        assertInstanceOf(String.class, response.getBody());
        String text = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/index.html");
        String index = Utility.getInstance().stream2str(in);
        assertEquals(index, text);
    }

    @Test
    void getCssPage() throws IOException, InterruptedException {
        // test path traversal protection that "../" and "..\" will be removed
        EventEnvelope response = httpGet(localHost, "/../..\\sample.css", null);
        assert response != null;
        assertEquals("text/css", response.getHeader("content-type"));
        assertNotNull(response.getHeader("ETag"));
        assertInstanceOf(String.class, response.getBody());
        String text = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/sample.css");
        String css = Utility.getInstance().stream2str(in);
        assertEquals(css, text);
    }

    @Test
    void getTextPage() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/sample.txt", null);
        assert response != null;
        assertEquals("text/plain", response.getHeader("content-type"));
        assertInstanceOf(String.class, response.getBody());
        String text = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/sample.txt");
        String plain = Utility.getInstance().stream2str(in);
        assertEquals(plain, text);
    }

    @Test
    void getJsPage() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/sample.js", null);
        assert response != null;
        assertEquals("text/javascript", response.getHeader("content-type"));
        assertInstanceOf(String.class, response.getBody());
        String text = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/sample.js");
        String js = Utility.getInstance().stream2str(in);
        assertEquals(js, text);
    }

    @SuppressWarnings("unchecked")
    @Test
    void pageNotExists() throws IOException, InterruptedException {
        EventEnvelope response = httpGet(localHost, "/no_such_page", null);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(404, response.getStatus());
        assertEquals(404, result.get("status"));
        assertEquals("Resource not found", result.get("message"));
    }
}
