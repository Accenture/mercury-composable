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

package com.accenture.tests;

import com.accenture.common.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Drives the actuator servlets (/info, /health, /env, /livenessprobe), the Spring error handler
 * (content-negotiated 404), and the XML/HTML/Text message converters via content negotiation on
 * {@code /greeting} - covering the servlets, spring.system and serializers packages end to end.
 */
class ActuatorEndpointTest extends TestBase {

    private EventEnvelope httpGet(String path, String accept) throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", "actuator-" + path.replace('/', '-'), "GET " + path);
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", accept);
        req.setUrl(path);
        req.setTargetHost("http://127.0.0.1:" + springPort);
        EventEnvelope request = new EventEnvelope().setTo(HTTP_REQUEST).setBody(req);
        EventEnvelope response = po.request(request, RPC_TIMEOUT).get();
        assertNotNull(response);
        return response;
    }

    @Test
    void infoEndpointVariants() throws Exception {
        // /info, /info/routes, /info/lib exercise the doGet path-selection branches
        assertEquals(200, httpGet("/info", "application/json").getStatus());
        assertEquals(200, httpGet("/info/routes", "application/json").getStatus());
        assertEquals(200, httpGet("/info/lib", "application/json").getStatus());
        // XML accept exercises the composeXmlOrJson XML branch in ServletBase
        assertEquals(200, httpGet("/info", "application/xml").getStatus());
    }

    @Test
    void healthLivenessEnvEndpoints() throws Exception {
        assertEquals(200, httpGet("/health", "application/json").getStatus());
        assertEquals(200, httpGet("/livenessprobe", "application/json").getStatus());
        assertEquals(200, httpGet("/env", "application/json").getStatus());
        assertEquals(200, httpGet("/env", "application/xml").getStatus());
    }

    @Test
    void errorHandlerNegotiatesContentType() throws Exception {
        // an unmapped path routes to the Spring ErrorController -> HttpErrorHandler.sendResponse,
        // which picks JSON / XML / HTML / text by the Accept header
        assertEquals(404, httpGet("/no-such-path", "application/json").getStatus());
        assertEquals(404, httpGet("/no-such-path", "application/xml").getStatus());
        assertEquals(404, httpGet("/no-such-path", "text/html").getStatus());
        assertEquals(404, httpGet("/no-such-path", "text/plain").getStatus());
    }

    @Test
    void greetingContentNegotiation() throws Exception {
        // exercise the XML / HTML / Text HttpMessageConverters through Spring content negotiation
        assertEquals(200, httpGet("/greeting", "application/xml").getStatus());
        assertEquals(200, httpGet("/greeting", "text/html").getStatus());
        assertEquals(200, httpGet("/greeting", "text/plain").getStatus());
        assertEquals(200, httpGet("/greeting", "application/json").getStatus());
    }
}
