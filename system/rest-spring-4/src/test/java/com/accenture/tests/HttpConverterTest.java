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

import org.junit.jupiter.api.Test;
import org.platformlambda.spring.serializers.HttpConverterHtml;
import org.platformlambda.spring.serializers.HttpConverterJson;
import org.platformlambda.spring.serializers.HttpConverterText;
import org.platformlambda.spring.serializers.HttpConverterXml;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Spring HttpMessageConverters (no running server) - exercises every write branch
 * (Map/PoJo, String, byte[], and the XML List branch) plus the read path for each media type.
 */
class HttpConverterTest {

    /** A minimal PoJo to drive the "object" (non-String/byte[]/Map/List) write branch. */
    public record Sample(String name, int value) { }

    private static final class Sink implements HttpOutputMessage {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final HttpHeaders headers = new HttpHeaders();
        @Override public OutputStream getBody() { return body; }
        @Override public HttpHeaders getHeaders() { return headers; }
        String text() { return body.toString(StandardCharsets.UTF_8); }
    }

    private static HttpInputMessage source(String content) {
        return new HttpInputMessage() {
            @Override public InputStream getBody() {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
            @Override public HttpHeaders getHeaders() { return new HttpHeaders(); }
        };
    }

    private static String write(HttpMessageConverter<Object> c, Object payload, MediaType type) throws Exception {
        Sink sink = new Sink();
        c.write(payload, type, sink);
        return sink.text();
    }

    @Test
    void jsonConverterCoversAllBranches() throws Exception {
        HttpConverterJson c = new HttpConverterJson();
        assertTrue(c.supports(Sample.class));
        assertTrue(write(c, Map.of("hello", "world"), MediaType.APPLICATION_JSON).contains("hello"));
        assertTrue(write(c, new Sample("a", 1), MediaType.APPLICATION_JSON).contains("\"name\""));
        assertEquals("plain", write(c, "plain", MediaType.APPLICATION_JSON));
        assertEquals("raw", write(c, "raw".getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON));
        Object parsed = c.read(Map.class, source("{\"a\":\"b\"}"));
        assertInstanceOf(Map.class, parsed);
    }

    @Test
    void textConverterCoversAllBranches() throws Exception {
        HttpConverterText c = new HttpConverterText();
        assertTrue(c.supports(Sample.class));
        assertEquals("hello", write(c, "hello", MediaType.TEXT_PLAIN));
        assertEquals("raw", write(c, "raw".getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN));
        assertTrue(write(c, Map.of("k", "v"), MediaType.TEXT_PLAIN).contains("k"));
        assertEquals("some text", c.read(Object.class, source("some text")));
    }

    @Test
    void htmlConverterCoversAllBranches() throws Exception {
        HttpConverterHtml c = new HttpConverterHtml();
        assertTrue(c.supports(Sample.class));
        assertEquals("<h1>hi</h1>", write(c, "<h1>hi</h1>", MediaType.TEXT_HTML));
        assertEquals("raw", write(c, "raw".getBytes(StandardCharsets.UTF_8), MediaType.TEXT_HTML));
        String html = write(c, Map.of("k", "v"), MediaType.TEXT_HTML);
        assertTrue(html.contains("<html>") && html.contains("</html>"));
        assertEquals("body text", c.read(Object.class, source("body text")));
    }

    @Test
    void xmlConverterCoversAllBranches() throws Exception {
        HttpConverterXml c = new HttpConverterXml();
        assertTrue(c.supports(Sample.class));
        assertTrue(write(c, Map.of("hello", "world"), MediaType.APPLICATION_XML).contains("<result>"));
        assertTrue(write(c, new Sample("a", 1), MediaType.APPLICATION_XML).contains("<sample>"));
        assertTrue(write(c, List.of("one", "two"), MediaType.APPLICATION_XML).contains("<result>"));
        assertEquals("plain", write(c, "plain", MediaType.APPLICATION_XML));
        assertEquals("raw", write(c, "raw".getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_XML));
        Object parsed = c.read(Map.class, source("<result><a>b</a></result>"));
        assertInstanceOf(Map.class, parsed);
    }
}
