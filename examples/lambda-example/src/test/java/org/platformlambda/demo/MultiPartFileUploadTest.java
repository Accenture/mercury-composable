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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventPublisher;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.platformlambda.demo.common.TestBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class MultiPartFileUploadTest extends TestBase {

    private static String port;

    @BeforeAll
    static void setup() {
        AppConfigReader config = AppConfigReader.getInstance();
        port = config.getProperty("rest.server.port");
    }

    @SuppressWarnings("unchecked")
    @Test
    void uploadSingleFileWithMultipart() throws IOException, InterruptedException, ExecutionException {
        String filename = "unit-test-data.txt";
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("unit.test", traceId, "/stream/upload/test");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String streamId = getStream(bytes, 15);
        // emulate a multipart file upload for a single file
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/upload");
        req.setTargetHost("http://127.0.0.1:" + port);
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "multipart/form-data");
        // for multipart upload, content-length must be set.
        req.setContentLength(bytes.size());
        // To upload a single file using multipart/form-data,
        // just set filename and stream-route.
        req.setFileName(filename);
        req.setStreamRoute(streamId);
        // send the HTTP request using AsyncHttpClient's route "async.http.request"
        EventEnvelope request = new EventEnvelope().setTo("async.http.request")
                .setBody(req).setTrace("101", "UPLOAD /multipart/1").setFrom("unit.test");
        EventEnvelope response = po.request(request, 8000).get();
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals(bytes.size(), map.getElement("total.length"));
        assertEquals(bytes.size(), map.getElement("file_size[0]"));
        assertEquals(filename, map.getElement("file_name[0]"));
        assertEquals("Upload completed", map.getElement("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void uploadTwoFilesWithMultipart() throws IOException, InterruptedException, ExecutionException {
        String filename1 = "unit-test-data1.txt";
        String filename2 = "unit-test-data2.txt";
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("unit.test", traceId, "/stream/upload/test");
        ByteArrayOutputStream bytes1 = new ByteArrayOutputStream();
        String streamId1 = getStream(bytes1, 10);
        ByteArrayOutputStream bytes2 = new ByteArrayOutputStream();
        String streamId2 = getStream(bytes2, 20);
        // emulate a multipart file upload for a single file
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/upload");
        req.setTargetHost("http://127.0.0.1:" + port);
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "multipart/form-data");
        // for multipart upload, content-length must be set.
        req.setContentLength(bytes1.size() + bytes2.size());
        // To upload multiple files using multipart/form-data,
        // file-names, file-content-types and stream-routes must be set.
        req.setFileNames(List.of(filename1, filename2));
        req.setFileContentTypes(List.of("text/plain", "text/plain"));
        req.setStreamRoutes(List.of(streamId1, streamId2));
        // send the HTTP request using AsyncHttpClient's route "async.http.request"
        EventEnvelope request = new EventEnvelope().setTo("async.http.request")
                .setBody(req).setTrace("102", "UPLOAD /multipart/2").setFrom("unit.test");
        EventEnvelope response = po.request(request, 8000).get();
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals(bytes1.size() + bytes2.size(), map.getElement("total.length"));
        assertEquals(bytes1.size(), map.getElement("file_size[0]"));
        assertEquals(filename1, map.getElement("file_name[0]"));
        assertEquals(bytes2.size(), map.getElement("file_size[1]"));
        assertEquals(filename2, map.getElement("file_name[1]"));
        assertEquals("Upload completed", map.getElement("message"));
    }

    private String getStream(ByteArrayOutputStream bytes, int lines) throws IOException {
        Utility util = Utility.getInstance();
        EventPublisher publisher = new EventPublisher(10000);
        for (int i=0; i < lines; i++) {
            String line = "hello world "+i+"\n";
            byte[] d = util.getUTF(line);
            publisher.publish(d);
            bytes.write(d);
        }
        publisher.publishCompletion();
        return publisher.getStreamId();
    }
}
