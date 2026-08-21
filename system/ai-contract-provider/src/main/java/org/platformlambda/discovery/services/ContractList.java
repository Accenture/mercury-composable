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

import java.util.LinkedHashMap;
import java.util.Map;

/** List the installed operational contracts (id, module, summary). */
@PreLoad(route = "v1.contract.list", instances = 10, isPrivate = true)
public class ContractList implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var contracts = ContractCatalog.getInstance().getContracts().stream().map(contract -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("id", contract.id());
            item.put("module", contract.module());
            item.put("summary", contract.summary());
            return item;
        }).toList();
        var result = new LinkedHashMap<String, Object>();
        result.put("mercury_version", SkillSnapshot.getInstance().getMercuryVersion());
        result.put("total", contracts.size());
        result.put("contracts", contracts);
        return result;
    }
}
