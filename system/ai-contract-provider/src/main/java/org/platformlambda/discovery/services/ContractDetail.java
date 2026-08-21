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
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.LinkedHashMap;
import java.util.Map;

/** Describe one installed contract: behavior anchors and packaged references. */
@PreLoad(route = "v1.contract.detail", instances = 10, isPrivate = true)
public class ContractDetail implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var id = input.get("id") instanceof String value ? value : "";
        var contract = ContractCatalog.getInstance().getContract(id);
        if (contract == null) {
            throw new AppException(404, "Contract " + id + " is not installed");
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("id", contract.id());
        result.put("module", contract.module());
        result.put("summary", contract.summary());
        result.put("mercury_version", SkillSnapshot.getInstance().getMercuryVersion());
        result.put("behavior_anchors", contract.anchors());
        result.put("references", contract.references());
        return result;
    }
}
