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

package org.platformlambda.helpers.registry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.helpers.registry.store.SchemaStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaRegistryTest {

    @BeforeAll
    public static void setup() {
        SchemaStore.getInstance().clear();
        AppStarter.main(new String[0]);
    }

    @Test
    public void testHealthCheck() throws Exception {
        PostOffice po = new PostOffice("unit.test", "123", "GET /");
        EventEnvelope req = new EventEnvelope()
                .setTo("schema.registry.health");
                
        EventEnvelope res = po.request(req, 5000).get();
        assertEquals(200, res.getStatus());
        assertTrue(res.getBody() instanceof Map);
        Map<?,?> body = (Map<?,?>) res.getBody();
        assertTrue(body.isEmpty());
    }

    @Test
    public void testRegisterAndGetAvroSchema() throws Exception {
        PostOffice po = new PostOffice("unit.test", "123", "POST /subjects/test-avro/versions");
        
        String avroSchema = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}";
        Map<String, Object> payload = new HashMap<>();
        payload.put("schema", avroSchema);
        // schemaType is omitted, should default to AVRO
        
        EventEnvelope req = new EventEnvelope()
                .setTo("schema.registry.register")
                .setHeader("subject", "test-avro")
                .setBody(payload);
                
        EventEnvelope res = po.request(req, 5000).get();
        assertEquals(200, res.getStatus());
        
        assertTrue(res.getBody() instanceof Map);
        Map<?,?> body = (Map<?,?>) res.getBody();
        assertTrue(body.containsKey("id"));
        int id = (Integer) body.get("id");
        assertTrue(id > 0);
        
        // Now get the schema by ID
        EventEnvelope getReq = new EventEnvelope()
                .setTo("schema.registry.get")
                .setHeader("id", String.valueOf(id));
                
        EventEnvelope getRes = po.request(getReq, 5000).get();
        assertEquals(200, getRes.getStatus());
        
        Map<?,?> getBody = (Map<?,?>) getRes.getBody();
        assertEquals(avroSchema, getBody.get("schema"));
        assertFalse(getBody.containsKey("schemaType"), "AVRO schemaType should be omitted in response");
    }
    
    @Test
    public void testRegisterAndGetJsonSchema() throws Exception {
        PostOffice po = new PostOffice("unit.test", "123", "POST /subjects/test-json/versions");
        
        String jsonSchema = "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}";
        Map<String, Object> payload = new HashMap<>();
        payload.put("schema", jsonSchema);
        payload.put("schemaType", "JSON");
        
        EventEnvelope req = new EventEnvelope()
                .setTo("schema.registry.register")
                .setHeader("subject", "test-json")
                .setBody(payload);
                
        EventEnvelope res = po.request(req, 5000).get();
        assertEquals(200, res.getStatus());
        
        Map<?,?> body = (Map<?,?>) res.getBody();
        int id = (Integer) body.get("id");
        
        // Now get the schema by ID
        EventEnvelope getReq = new EventEnvelope()
                .setTo("schema.registry.get")
                .setHeader("id", String.valueOf(id));
                
        EventEnvelope getRes = po.request(getReq, 5000).get();
        assertEquals(200, getRes.getStatus());
        
        Map<?,?> getBody = (Map<?,?>) getRes.getBody();
        assertEquals(jsonSchema, getBody.get("schema"));
        assertEquals("JSON", getBody.get("schemaType"));
    }
    
    @Test
    public void testIdempotentRegistration() throws Exception {
        PostOffice po = new PostOffice("unit.test", "123", "POST /subjects/test-idem/versions");
        
        String schema = "{\"type\":\"string\"}";
        Map<String, Object> payload = new HashMap<>();
        payload.put("schema", schema);
        
        EventEnvelope req1 = new EventEnvelope()
                .setTo("schema.registry.register")
                .setHeader("subject", "test-idem")
                .setBody(payload);
                
        EventEnvelope res1 = po.request(req1, 5000).get();
        int id1 = (Integer) ((Map<?,?>) res1.getBody()).get("id");
        
        EventEnvelope req2 = new EventEnvelope()
                .setTo("schema.registry.register")
                .setHeader("subject", "test-idem")
                .setBody(payload);
                
        EventEnvelope res2 = po.request(req2, 5000).get();
        int id2 = (Integer) ((Map<?,?>) res2.getBody()).get("id");
        
        assertEquals(id1, id2, "Registering the same schema should return the same ID");
    }
    
    @Test
    public void testMissingSchema() throws Exception {
        PostOffice po = new PostOffice("unit.test", "123", "GET /schemas/ids/999");
        
        EventEnvelope req = new EventEnvelope()
                .setTo("schema.registry.get")
                .setHeader("id", "999");
                
        EventEnvelope res = po.request(req, 5000).get();
        assertEquals(404, res.getStatus());
    }
}
