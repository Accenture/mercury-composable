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
import org.platformlambda.discovery.models.ContractEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/** The one URL an AI agent needs first: version, contract ids, and the endpoint map. */
@PreLoad(route = "v1.discovery.index", instances = 10)
public class DiscoveryIndex implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var endpoints = new LinkedHashMap<String, Object>();
        endpoints.put("contracts", "GET /api/contracts");
        endpoints.put("contract_detail", "GET /api/contracts/{id}");
        endpoints.put("skill", "GET /api/skill");
        endpoints.put("reference", "GET /api/references?path={reference-path}");
        endpoints.put("manifest", "GET /api/manifest");
        var result = new LinkedHashMap<String, Object>();
        result.put("name", "ai-contract-provider");
        result.put("description",
                "Version-matched Mercury operational contract for AI discovery (read-only)");
        result.put("mercury_version", SkillSnapshot.getInstance().getMercuryVersion());
        result.put("contracts",
                ContractCatalog.getInstance().getContracts().stream().map(ContractEntry::id).toList());
        result.put("endpoints", endpoints);
        return result;
    }
}
