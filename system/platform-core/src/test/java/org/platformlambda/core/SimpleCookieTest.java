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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleHttpCookie;

import org.platformlambda.core.util.Utility;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCookieTest {

    private static final String SET_COOKIE = "Set-Cookie";

    private SimpleHttpCookie createCookie(String key, String value) {
        SimpleHttpCookie cookie = new SimpleHttpCookie(key, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(60);
        return cookie;
    }

    @Test
    void validateCookie() {
        String cookie = createCookie("hello", "world").toString();
        assertTrue(cookie.contains("Path=/;"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure;"));
        assertTrue(cookie.contains("hello=world;"));
        assertTrue(cookie.contains("Max-Age=60;"));
        assertTrue(cookie.contains("GMT;"));
    }

    @Test
    void cookieInEnvelope() {
        EventEnvelope event = new EventEnvelope();
        // "Set-Cookie" is the only header that supports multiple values
        event.setHeader(SET_COOKIE, createCookie("key1", "value1"));
        event.setHeader(SET_COOKIE, createCookie("key2", "value2"));
        String HELLO = "hello";
        String WORLD = "world";
        event.setHeader(HELLO, WORLD);
        String cookies = event.getHeader(SET_COOKIE);
        assertTrue(cookies.contains("|"));
        List<String> cookieList = Utility.getInstance().split(cookies, "|");
        assertEquals(2, cookieList.size());
        assertTrue(cookieList.get(0).startsWith("key1=value1;"));
        assertTrue(cookieList.get(1).startsWith("key2=value2;"));
        assertEquals(WORLD, event.getHeader(HELLO));
    }

}
