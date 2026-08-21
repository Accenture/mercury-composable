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
import org.platformlambda.contracts.ContractBuild;
import org.platformlambda.contracts.MercuryContract;
import org.platformlambda.contracts.MercuryContractProvider;

import java.util.Collection;
import java.util.List;

/** Operational contract implemented by MiniGraph. */
public class MiniGraphContractProvider implements MercuryContractProvider {
    @Override
    public String providerId() {
        return "minigraph-provider";
    }

    @Override
    public String contractBuildId() {
        return ContractBuild.ID;
    }

    @Override
    public Collection<MercuryContract> contracts() {
        return List.of(new MercuryContract(
                "minigraph",
                "minigraph-playground-engine",
                "MiniGraph models, skills, commands, validation, and compilation rules",
                List.of(GraphCommandService.class, CompileGraph.class, GraphModelValidator.class),
                List.of(
                        "references/guides/knowledge-graph/ai-agent-guide.md",
                        "references/guides/knowledge-graph/command-reference.md",
                        "references/guides/knowledge-graph/minigraph-commands.json",
                        "references/guides/knowledge-graph/skills-reference.md")));
    }
}
