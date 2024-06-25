/*

    Copyright 2018-2024 Accenture Technology

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

import com.accenture.demo.models.ProfileConfirmation;
import com.accenture.demo.models.Profile;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;

@PreLoad(route="v1.create.profile", instances=100)
public class CreateProfile implements TypedLambdaFunction<Profile, ProfileConfirmation> {

    private static final Utility util = Utility.getInstance();
    private static final String PROTECTED_FIELDS = "protected_fields";

    @SuppressWarnings("unchecked")
    @Override
    public ProfileConfirmation handleEvent(Map<String, String> headers, Profile profile, int instance) {
        if (profile.id == null) {
            throw new IllegalArgumentException("Missing id");
        }
        String protectedFields = headers.get(PROTECTED_FIELDS);
        if (protectedFields == null) {
            throw new IllegalArgumentException("Missing protected_fields");
        }
        MultiLevelMap masked = new MultiLevelMap(SimpleMapper.getInstance().getMapper().readValue(profile, Map.class));
        List<String> fields = util.split(protectedFields, ", ");
        for (String f: fields) {
            if (masked.exists(f)) {
                masked.setElement(f, "***");
            }
        }
        ProfileConfirmation result = new ProfileConfirmation();
        result.profile = masked.getMap();
        result.type = "CREATE";
        result.secure = fields;
        return result;
    }

}
