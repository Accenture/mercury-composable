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

import com.accenture.demo.models.ProfileConfirmation;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;

@PreLoad(route="v1.create.profile", instances=10)
public class CreateProfile implements TypedLambdaFunction<Map<String, Object>, ProfileConfirmation> {

    private static final Utility util = Utility.getInstance();
    private static final String REQUIRED_FIELDS = "required_fields";
    private static final String PROTECTED_FIELDS = "protected_fields";

    /**
     * To make this function generic, we use Map as input instead of the Profile class.
     *
     * @param headers containing required_fields and protected_fields parameters
     * @param input dataset
     * @param instance of this function
     * @return profile confirmation object
     */
    @Override
    public ProfileConfirmation handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (!input.containsKey("id")) {
            throw new IllegalArgumentException("Missing id");
        }
        String requiredFields = headers.get(REQUIRED_FIELDS);
        if (requiredFields == null) {
            throw new IllegalArgumentException("Missing required_fields");
        }
        String protectedFields = headers.get(PROTECTED_FIELDS);
        if (protectedFields == null) {
            throw new IllegalArgumentException("Missing protected_fields");
        }
        MultiLevelMap data = new MultiLevelMap(input);
        List<String> fields = util.split(requiredFields, ", ");
        for (String f: fields) {
            if (!data.exists(f)) {
                throw new IllegalArgumentException("Missing " + f);
            }
        }
        List<String> pFields = util.split(protectedFields, ", ");
        for (String f: pFields) {
            if (data.exists(f)) {
                data.setElement(f, "***");
            }
        }
        ProfileConfirmation result = new ProfileConfirmation();
        result.profile = data.getMap();
        result.type = "CREATE";
        result.secure = pFields;
        return result;
    }
}
