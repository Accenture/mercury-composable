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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModelTest {

    @Test
    void asyncHttpRequestModel() {
        final String HELLO = "hello";
        final String WORLD = "world";
        final String PUT = "PUT";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setContentLength(HELLO.length());
        request.setBody(HELLO);
        request.setHeader(HELLO, WORLD);
        request.setCookie(HELLO, WORLD).setMethod(PUT);
        request.setCookie("hack", "invalid\ncookie");
        request.setFileName("none")
                .setPathParameter(HELLO, WORLD)
                .setQueryString(HELLO+"="+WORLD)
                .setQueryParameter(HELLO, WORLD);
        request.setRemoteIp("127.0.0.1");
        request.setSecure(false).setStreamRoute("none");
        request.setSessionInfo(HELLO, WORLD).setTrustAllCert(false).setTimeoutSeconds(10);
        request.setTargetHost("http://localhost");
        request.setUploadTag("file").setUrl("/api/hello");
        request.removePathParameter(HELLO).setPathParameter(HELLO, WORLD);
        request.removeHeader(HELLO).setHeader(HELLO, WORLD);
        request.removeSessionInfo(HELLO).setSessionInfo(HELLO, WORLD);
        request.removeCookie(HELLO).setCookie(HELLO, WORLD);
        request.removeQueryParameter(HELLO).setQueryParameter(HELLO, WORLD);
        AsyncHttpRequest restored = new AsyncHttpRequest(request.toMap());
        assertEquals(HELLO, restored.getBody());
        // header, cookie, path and query's get methods are case-insensitive
        assertEquals(WORLD, restored.getHeader("hElLo"));
        assertEquals(WORLD, restored.getCookie("heLLO"));
        // prove that the invalid cookie will be pass thru
        assertEquals(1, restored.getCookies().size());
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
        Map<String, String> parameters = restored.getPathParameters();
        assertEquals(Map.of(HELLO, WORLD), parameters);
        Map<String, Object> query = restored.getQueryParameters();
        assertEquals(Map.of(HELLO, WORLD), query);
        List<String> qp = restored.getQueryParameters(HELLO);
        assertEquals(1, qp.size());
        assertEquals(WORLD, qp.getFirst());
    }
}
