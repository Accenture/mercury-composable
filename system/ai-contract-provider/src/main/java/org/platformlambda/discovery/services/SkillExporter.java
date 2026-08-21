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

package org.platformlambda.discovery.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.discovery.export.OfflineSkillWriter;

import java.util.Map;

/**
 * Write the offline mercury-platform Agent Skill. This function is deliberately NOT
 * mapped in rest.yaml - filesystem export is a local operator action, launched by the
 * application's --export mode through the export-skill flow.
 */
@PreLoad(route = "v1.skill.exporter", instances = 1, isPrivate = true)
public class SkillExporter implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var directory = input.get("directory") instanceof String value ? value : null;
        return new OfflineSkillWriter().export(directory);
    }
}
