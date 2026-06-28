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

package org.platformlambda.example;

import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class SchemaRegistryClient {
    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryClient.class);
    private static final String HTTP_REQUEST = "async.http.request";
    
    private final String registryUrl;
    
    public SchemaRegistryClient() {
        this.registryUrl = AppConfigReader.getInstance().getProperty("schema.registry.url", "http://127.0.0.1:8081");
    }

    public int registerSchema(String subject, String schema, String schemaType) throws ExecutionException, InterruptedException, TimeoutException {
        PostOffice po = new PostOffice("demo.client", "1", "POST " + registryUrl + "/subjects/" + subject + "/versions");
        
        Map<String, Object> body = new HashMap<>();
        body.put("schema", schema);
        if (schemaType != null) {
            body.put("schemaType", schemaType);
        }
        
        EventEnvelope req = new EventEnvelope()
                .setTo(HTTP_REQUEST)
                .setHeader("url", registryUrl + "/subjects/" + subject + "/versions")
                .setHeader("method", "POST")
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json")
                .setBody(body);
                
        EventEnvelope res = po.request(req, 5000).get();
        if (res.getStatus() == 200 && res.getBody() instanceof Map) {
            Map<?,?> resBody = (Map<?,?>) res.getBody();
            if (resBody.containsKey("id")) {
                int id = (Integer) resBody.get("id");
                log.info("Registered {} schema for subject '{}' with ID {}", schemaType != null ? schemaType : "AVRO", subject, id);
                return id;
            }
        }
        log.error("Failed to register schema. Status: {}, Body: {}", res.getStatus(), res.getBody());
        return -1;
    }
    
    public String getSchema(int id) throws ExecutionException, InterruptedException, TimeoutException {
        PostOffice po = new PostOffice("demo.client", "1", "GET " + registryUrl + "/schemas/ids/" + id);
        
        EventEnvelope req = new EventEnvelope()
                .setTo(HTTP_REQUEST)
                .setHeader("url", registryUrl + "/schemas/ids/" + id)
                .setHeader("method", "GET")
                .setHeader("accept", "application/json");
                
        EventEnvelope res = po.request(req, 5000).get();
        if (res.getStatus() == 200 && res.getBody() instanceof Map) {
            Map<?,?> resBody = (Map<?,?>) res.getBody();
            if (resBody.containsKey("schema")) {
                String schema = (String) resBody.get("schema");
                String type = (String) resBody.getOrDefault("schemaType", "AVRO");
                log.info("Retrieved {} schema for ID {}: {}", type, id, schema);
                return schema;
            }
        }
        log.error("Failed to get schema for ID {}. Status: {}, Body: {}", id, res.getStatus(), res.getBody());
        return null;
    }
}
