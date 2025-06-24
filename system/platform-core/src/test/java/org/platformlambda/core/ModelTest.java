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
        final String hello = "hello";
        final String world = "world";
        final String putMethod = "PUT";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setContentLength(hello.length());
        request.setBody(hello);
        request.setHeader(hello, world);
        request.setCookie(hello, world).setMethod(putMethod);
        request.setCookie("hack", "invalid\ncookie");
        request.setFileName("none")
                .setPathParameter(hello, world)
                .setQueryString(hello+"="+world)
                .setQueryParameter(hello, world);
        request.setRemoteIp("127.0.0.1");
        request.setSecure(false).setStreamRoute("none");
        request.setSessionInfo(hello, world).setTrustAllCert(false).setTimeoutSeconds(10);
        request.setTargetHost("http://localhost");
        request.setUploadTag("file").setUrl("/api/hello");
        request.removePathParameter(hello).setPathParameter(hello, world);
        request.removeHeader(hello).setHeader(hello, world);
        request.removeSessionInfo(hello).setSessionInfo(hello, world);
        request.removeCookie(hello).setCookie(hello, world);
        request.removeQueryParameter(hello).setQueryParameter(hello, world);
        AsyncHttpRequest restored = new AsyncHttpRequest(request.toMap());
        assertEquals(hello, restored.getBody());
        // header, cookie, path and query's get methods are case-insensitive
        assertEquals(world, restored.getHeader("hElLo"));
        assertEquals(world, restored.getCookie("heLLO"));
        // prove that the invalid cookie will be pass thru
        assertEquals(1, restored.getCookies().size());
        assertEquals(world, restored.getPathParameter("Hello"));
        assertEquals(world, restored.getQueryParameter("HellO"));
        assertEquals(putMethod, restored.getMethod());
        assertEquals("none", restored.getFileName());
        assertEquals("127.0.0.1", restored.getRemoteIp());
        assertFalse(restored.isSecure());
        assertEquals("none", restored.getStreamRoute());
        assertEquals(world, restored.getSessionInfo(hello));
        assertFalse(restored.isTrustAllCert());
        assertEquals(10, restored.getTimeoutSeconds());
        assertEquals("http://localhost", restored.getTargetHost());
        assertEquals("file", restored.getUploadTag());
        assertEquals("/api/hello", restored.getUrl());
        Map<String, String> parameters = restored.getPathParameters();
        assertEquals(Map.of(hello, world), parameters);
        Map<String, Object> query = restored.getQueryParameters();
        assertEquals(Map.of(hello, world), query);
        List<String> qp = restored.getQueryParameters(hello);
        assertEquals(1, qp.size());
        assertEquals(world, qp.getFirst());
    }
}
