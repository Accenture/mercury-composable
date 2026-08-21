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

package com.accenture.contracts;

import com.accenture.automation.CompileFlows;
import org.platformlambda.contracts.ContractBuild;
import org.platformlambda.contracts.MercuryContract;
import org.platformlambda.contracts.MercuryContractProvider;

import java.util.Collection;
import java.util.List;

/** Operational contract implemented by Event Script. */
public class EventScriptContractProvider implements MercuryContractProvider {
    @Override
    public String providerId() {
        return "event-script-provider";
    }

    @Override
    public String contractBuildId() {
        return ContractBuild.ID;
    }

    @Override
    public Collection<MercuryContract> contracts() {
        return List.of(new MercuryContract(
                "event-script",
                "event-script-engine",
                "Event Script flow YAML, data mapping, execution, and compilation rules",
                List.of(CompileFlows.class),
                List.of(
                        "references/guides/event-script/ai-agent-guide.md",
                        "references/guides/event-script/flow-grammar.md",
                        "references/guides/event-script/event-script-flow.json")));
    }
}
