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

package org.platformlambda.demo;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventPublisher;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.demo.common.TestBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MultiPartFileUploadTest extends TestBase {

    @SuppressWarnings("unchecked")
    @Test
    void uploadTest() throws IOException, InterruptedException {
        String FILENAME = "unit-test-data.txt";
        BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("unit.test", traceId, "/stream/upload/test");
        int len = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventPublisher publisher = new EventPublisher(10000);
        for (int i=0; i < 10; i++) {
            String line = "hello world "+i+"\n";
            byte[] d = util.getUTF(line);
            publisher.publish(d);
            bytes.write(d);
            len += d.length;
        }
        publisher.publishCompletion();
        // emulate a multipart file upload
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/upload/demo");
        req.setTargetHost("http://127.0.0.1:8080");
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "multipart/form-data");
        req.setContentLength(len);
        req.setFileName(FILENAME);
        req.setStreamRoute(publisher.getStreamId());
        // send the HTTP request event to the "hello.upload" function
        EventEnvelope request = new EventEnvelope().setTo("hello.upload")
                .setBody(req).setTrace("12345", "/api/upload/demo").setFrom("unit.test");
        po.asyncRequest(request, 8000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        System.out.println(response.getBody());
        assertEquals(len, map.get("expected_size"));
        assertEquals(len, map.get("actual_size"));
        assertEquals(FILENAME, map.get("filename"));
        assertEquals("Upload completed", map.get("message"));
    }
}
