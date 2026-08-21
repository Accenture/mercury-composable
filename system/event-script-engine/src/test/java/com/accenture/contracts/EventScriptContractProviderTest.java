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
import org.junit.jupiter.api.Test;
import org.platformlambda.contracts.ContractBuild;
import org.platformlambda.contracts.ContractRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventScriptContractProviderTest {

    @Test
    void providerAnchorsTheEventScriptCompiler() {
        var provider = new EventScriptContractProvider();
        var contract = provider.contracts().iterator().next();
        assertEquals(ContractBuild.ID, provider.contractBuildId());
        assertEquals("event-script", contract.id());
        assertEquals(CompileFlows.class, contract.behaviorAnchors().getFirst());
        assertEquals(List.of(
                        "references/guides/event-script/ai-agent-guide.md",
                        "references/guides/event-script/flow-grammar.md",
                        "references/guides/event-script/event-script-flow.json"),
                contract.references());
        assertEquals(List.of("event-script-provider", "platform-core-provider"),
                ContractRegistry.load().providers().stream().map(providerItem ->
                        providerItem.providerId()).toList());
    }
}
