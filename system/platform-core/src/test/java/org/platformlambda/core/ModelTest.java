/*

    Copyright 2018-2024 Accenture Technology

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

package org.platformlambda.core;

import org.junit.Test;
import org.platformlambda.core.models.AsyncHttpRequest;

import static org.junit.Assert.*;

public class ModelTest {

    @Test
    public void asyncHttpRequestModel() {
        final String HELLO = "hello";
        final String WORLD = "world";
        final String PUT = "PUT";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setContentLength(HELLO.length());
        request.setBody(HELLO);
        request.setHeader(HELLO, WORLD);
        request.setCookie(HELLO, WORLD);
        request.setMethod(PUT);
        request.setFileName("none");
        request.setPathParameter(HELLO, WORLD);
        request.setQueryString(HELLO+"="+WORLD);
        request.setQueryParameter(HELLO, WORLD);
        request.setRemoteIp("127.0.0.1");
        request.setSecure(false);
        request.setStreamRoute("none");
        request.setSessionInfo(HELLO, WORLD);
        request.setTrustAllCert(false);
        request.setTimeoutSeconds(10);
        request.setTargetHost("http://localhost");
        request.setUploadTag("file");
        request.setUrl("/api/hello");

        AsyncHttpRequest restored = new AsyncHttpRequest(request.toMap());
        assertEquals(HELLO, restored.getBody());
        // header, cookie, path and query's get methods are case-insensitive
        assertEquals(WORLD, restored.getHeader("hElLo"));
        assertEquals(WORLD, restored.getCookie("heLLO"));
        assertEquals(WORLD, restored.getPathParameter("Hello"));
        assertEquals(WORLD, restored.getQueryParameter("HellO"));
        assertEquals(PUT, restored.getMethod());
        assertEquals("none", restored.getFileName());
        assertEquals("127.0.0.1", restored.getRemoteIp());
        assertFalse(restored.isSecure());
        assertEquals("none", restored.getStreamRoute());
        assertEquals(WORLD, restored.getSessionInfo(HELLO));
        assertFalse(restored.isTrustAllCert());
        assertEquals(10, restored.getTimeoutSeconds());
        assertEquals("http://localhost", restored.getTargetHost());
        assertEquals("file", restored.getUploadTag());
        assertEquals("/api/hello", restored.getUrl());
    }
}
