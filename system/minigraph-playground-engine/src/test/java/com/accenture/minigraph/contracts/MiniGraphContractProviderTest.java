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

package com.accenture.minigraph.contracts;

import com.accenture.minigraph.common.GraphModelValidator;
import com.accenture.minigraph.services.GraphCommandService;
import com.accenture.minigraph.start.CompileGraph;
import org.junit.jupiter.api.Test;
import org.platformlambda.contracts.ContractBuild;
import org.platformlambda.contracts.ContractRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiniGraphContractProviderTest {

    @Test
    void assembledRuntimeDiscoversTheExactProviderAndAnchorInventory() {
        var provider = new MiniGraphContractProvider();
        var contract = provider.contracts().iterator().next();
        assertEquals(ContractBuild.ID, provider.contractBuildId());
        assertEquals("minigraph", contract.id());
        assertEquals(List.of(GraphCommandService.class, CompileGraph.class, GraphModelValidator.class),
                contract.behaviorAnchors());
        assertEquals(List.of(
                        "references/guides/knowledge-graph/ai-agent-guide.md",
                        "references/guides/knowledge-graph/command-reference.md",
                        "references/guides/knowledge-graph/minigraph-commands.json",
                        "references/guides/knowledge-graph/skills-reference.md"),
                contract.references());

        var registry = ContractRegistry.load();
        assertEquals(List.of("event-script-provider", "minigraph-provider", "platform-core-provider"),
                registry.providers().stream().map(item -> item.providerId()).toList());
        assertEquals(List.of("event-script", "minigraph", "platform-core", "rest-automation"),
                registry.contracts().stream().map(item -> item.id()).toList());
    }
}
