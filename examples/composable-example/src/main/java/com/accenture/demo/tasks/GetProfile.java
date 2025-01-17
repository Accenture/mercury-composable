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

import com.accenture.demo.models.Profile;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.util.Map;

@PreLoad(route="v1.get.profile", instances=10)
public class GetProfile implements TypedLambdaFunction<Map<String, Object>, Profile> {

    private static final  Utility util = Utility.getInstance();

    private static final String PROFILE_ID = "profile_id";
    private static final String TEMP_DATA_STORE = "/tmp/store";
    private static final String JSON_EXT = ".json";

    @Override
    public Profile handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws Exception {
        if (!headers.containsKey(PROFILE_ID)) {
            throw new AppException(400, "Missing profile_id");
        }
        String profileId = headers.get(PROFILE_ID);
        File profileFile = new File(TEMP_DATA_STORE, profileId+JSON_EXT);
        if (!profileFile.exists()) {
            throw new AppException(404, "Profile "+profileId+" not found");
        }
        String json = util.file2str(profileFile);
        return SimpleMapper.getInstance().getMapper().readValue(json, Profile.class);
    }

}
