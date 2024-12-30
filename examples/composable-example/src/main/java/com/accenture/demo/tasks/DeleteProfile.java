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

 package com.accenture.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@PreLoad(route="v1.delete.profile", instances=100)
public class DeleteProfile implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String PROFILE_ID = "profile_id";
    private static final String TEMP_DATA_STORE = "/tmp/store";
    private static final String JSON_EXT = ".json";
    private static final String DELETED = "deleted";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        if (!headers.containsKey(PROFILE_ID)) {
            throw new AppException(400, "Missing profile_id");
        }
        String profileId = headers.get(PROFILE_ID);
        File f = new File(TEMP_DATA_STORE, profileId+JSON_EXT);
        if (!f.exists()) {
            throw new AppException(404, "Profile "+profileId+" not found");
        }
        // save profile ID into the return result set so the next task can use it
        Map<String, Object> result = new HashMap<>();
        result.put(PROFILE_ID, profileId);
        result.put(DELETED, f.delete());
        return result;
    }

}
