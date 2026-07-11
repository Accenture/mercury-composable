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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;

/**
 * First task of the three REST flows (rest role). Validates the incoming HTTP request, builds the
 * profile Request message, and returns both the message and the immediate acknowledgment:
 *
 * <ul>
 *   <li>{@code result.request} - the Request map {@code {command, id, profile?}} that the flow publishes
 *       to the on-prem {@code OP_PROFILE_REQUEST} topic (subject {@code op-profile-request})</li>
 *   <li>{@code result.ack} - the 202 body returned to the caller right away ({@code execution: response}),
 *       with {@code originator: HTTP_REQUEST}</li>
 * </ul>
 *
 * <p>The command arrives as the {@code command} task header (a constant per flow). READ and DELETE carry
 * the profile id as a path parameter; UPSERT carries the full profile in the request body and takes the
 * id from {@code profile.id}.</p>
 */
@PreLoad(route = "v1.api.request", instances = 10)
public class ApiRequest implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String COMMAND = "command";
    private static final String UPSERT = "UPSERT";
    private static final String ID = "id";
    private static final String PROFILE = "profile";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input,
                                           int instance) throws AppException {
        String command = headers.get(COMMAND);
        if (command == null) {
            throw new AppException(500, "Missing 'command' task header - check the flow definition");
        }
        Map<String, Object> request = new HashMap<>();
        request.put(COMMAND, command);
        if (UPSERT.equals(command)) {
            Object profile = input.get(PROFILE);
            if (!(profile instanceof Map) || ((Map<?, ?>) profile).isEmpty()) {
                throw new AppException(400, "Missing profile in request body");
            }
            int id = parseId(((Map<?, ?>) profile).get(ID));
            request.put(ID, id);
            request.put(PROFILE, profile);
        } else {
            request.put(ID, parseId(input.get(ID)));
        }
        Map<String, Object> ack = new HashMap<>();
        ack.put(COMMAND, command);
        ack.put(ID, request.get(ID));
        ack.put("originator", "HTTP_REQUEST");
        ack.put("message", "Request accepted for processing");
        Map<String, Object> result = new HashMap<>();
        result.put("request", request);
        result.put("ack", ack);
        return result;
    }

    private int parseId(Object id) throws AppException {
        int n = Utility.getInstance().str2int(String.valueOf(id));
        if (n <= 0) {
            throw new AppException(400, "Profile id must be a positive integer");
        }
        return n;
    }
}
