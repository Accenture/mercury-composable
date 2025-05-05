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

package com.accenture.tests;

import com.accenture.common.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class GreetingTest extends TestBase {

    @SuppressWarnings("unchecked")
    @Test
    void helloWorld() throws IOException, InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", "12345", "TEST /helloworld");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", "application/json");
        req.setUrl("/greeting");
        req.setTargetHost("http://127.0.0.1:"+springPort);
        EventEnvelope request = new EventEnvelope().setTo(HTTP_REQUEST).setBody(req);
        EventEnvelope response = po.request(request, RPC_TIMEOUT).get();
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("Hello, World", map.getElement("content.greeting"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void helloUser() throws IOException, InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", "24680", "TEST /helloworld");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", "application/json");
        req.setUrl("/greeting");
        req.setQueryParameter("name", "user");
        req.setTargetHost("http://127.0.0.1:"+springPort);
        EventEnvelope request = new EventEnvelope().setTo(HTTP_REQUEST).setBody(req);
        EventEnvelope response = po.request(request, RPC_TIMEOUT).get();
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("Hello, user", map.getElement("content.greeting"));
    }
}
