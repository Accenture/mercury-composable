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

package com.accenture.twin.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * The system-of-record task (sor role), consuming READ / UPSERT / DELETE requests off the cloud
 * {@code C_PROFILE_REQUEST} topic. The cloud cluster has no Schema Registry, so the payload is the plain
 * JSON string of the Request as byte[] - parsed here with the platform's JSON mapper.
 *
 * <p>The "database" is the temp store {@code /tmp/twin-kafka-demo}: one {@code <id>.json} file per
 * profile. Every command gets a Response (errors travel as data - a READ or DELETE of a missing id
 * replies with "not found" rather than failing), which the flow publishes to {@code C_PROFILE_RESPONSE}.</p>
 *
 * <p>Runs with multiple workers ({@code instances=5}) - processing is asynchronous end to end and no
 * message ordering is assumed.</p>
 */
@PreLoad(route = "v1.profile.store", instances = 5)
public class ProfileStore implements TypedLambdaFunction<byte[], Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(ProfileStore.class);

    private static final String STORE_DIR = "/tmp/twin-kafka-demo";
    private static final String COMMAND = "command";
    private static final String ID = "id";
    private static final String PROFILE = "profile";
    private static final String MESSAGE = "message";

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleEvent(Map<String, String> headers, byte[] input, int instance)
            throws IOException {
        String requestJson = new String(input, StandardCharsets.UTF_8);
        Map<String, Object> request = SimpleMapper.getInstance().getMapper().readValue(requestJson, Map.class);
        String command = String.valueOf(request.get(COMMAND));
        // the platform's JSON mapper parses whole numbers as Long
        int id = request.get(ID) instanceof Number n ? n.intValue() : -1;
        Map<String, Object> response = new HashMap<>();
        response.put(COMMAND, command);
        response.put(ID, id);
        response.put("originator", "SYSTEM_OF_RECORDS");
        File store = new File(STORE_DIR);
        if (!store.exists() && !store.mkdirs()) {
            throw new IOException("Unable to create temp store " + STORE_DIR);
        }
        File record = new File(store, id + ".json");
        switch (command) {
            case "READ" -> {
                if (record.exists()) {
                    response.put(PROFILE, SimpleMapper.getInstance().getMapper().readValue(
                            Files.readString(record.toPath()), Map.class));
                    response.put(MESSAGE, "Profile " + id + " found");
                } else {
                    response.put(MESSAGE, "Profile " + id + " not found");
                }
            }
            case "UPSERT" -> {
                Files.writeString(record.toPath(), SimpleMapper.getInstance().getMapper()
                        .writeValueAsString(request.get(PROFILE)));
                response.put(PROFILE, request.get(PROFILE));
                response.put(MESSAGE, "Profile " + id + " saved");
            }
            case "DELETE" -> {
                if (Files.deleteIfExists(record.toPath())) {
                    response.put(MESSAGE, "Profile " + id + " deleted");
                } else {
                    response.put(MESSAGE, "Profile " + id + " not found");
                }
            }
            default -> response.put(MESSAGE, "Unknown command " + command);
        }
        log.info("worker-{} {} id={} - {}", instance, command, id, response.get(MESSAGE));
        return response;
    }
}
