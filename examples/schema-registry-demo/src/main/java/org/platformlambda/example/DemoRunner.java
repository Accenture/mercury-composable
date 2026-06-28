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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * A simple function to trigger the schema registration and retrieval process.
 */
@PreLoad(route = "demo.runner")
public class DemoRunner implements TypedLambdaFunction<EventEnvelope, Void> {

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) throws Exception {
        SchemaRegistryClient client = new SchemaRegistryClient();
        
        // 1. Register an Avro Schema
        String avroSchema = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}";
        int avroId = client.registerSchema("user-value", avroSchema, null);
        
        // 2. Register a JSON Schema
        String jsonSchema = "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}";
        int jsonId = client.registerSchema("person-value", jsonSchema, "JSON");
        
        // 3. Retrieve them
        client.getSchema(avroId);
        client.getSchema(jsonId);
        
        return null;
    }
}
