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
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

@PreLoad(route="v1.save.profile", instances=100)
public class SaveProfile implements TypedLambdaFunction<Profile, Void> {
    private static final Logger log = LoggerFactory.getLogger(SaveProfile.class);
    private static final  Utility util = Utility.getInstance();
    private static final String TEMP_DATA_STORE = "/tmp/store";
    private static final String JSON_EXT = ".json";

    @Override
    public Void handleEvent(Map<String, String> headers, Profile profile, int instance)
            throws Exception {
        if (profile.id == null) {
            throw new IllegalArgumentException("Missing id in profile");
        }
        String json = SimpleMapper.getInstance().getMapper().writeValueAsString(profile);
        File folder = new File(TEMP_DATA_STORE);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File f = new File(folder, profile.id+JSON_EXT);
        util.str2file(f, json);
        log.info("Profile {} saved", profile.id);
        // this end task does not have any output
        return null;
    }

}
