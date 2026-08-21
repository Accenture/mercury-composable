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

package org.platformlambda.contracts.providers;

import org.platformlambda.automation.config.RoutingEntry;
import org.platformlambda.contracts.ContractBuild;
import org.platformlambda.contracts.MercuryContract;
import org.platformlambda.contracts.MercuryContractProvider;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.system.PostOffice;

import java.util.Collection;
import java.util.List;

/** Operational contracts implemented by Platform Core and REST automation. */
public class PlatformCoreContractProvider implements MercuryContractProvider {
    @Override
    public String providerId() {
        return "platform-core-provider";
    }

    @Override
    public String contractBuildId() {
        return ContractBuild.ID;
    }

    @Override
    public Collection<MercuryContract> contracts() {
        return List.of(
                new MercuryContract(
                        "platform-core",
                        "platform-core",
                        "Composable functions, EventEnvelope messages, and PostOffice routing",
                        List.of(AppStarter.class, PostOffice.class, EventEnvelope.class),
                        List.of(
                                "references/guides/ai-developer-guide.md",
                                "references/guides/event-driven/ai-agent-guide.md")),
                new MercuryContract(
                        "rest-automation",
                        "platform-core",
                        "REST automation routes, flow bindings, relays, headers, and CORS",
                        List.of(RoutingEntry.class),
                        List.of(
                                "references/guides/rest-automation/ai-agent-guide.md",
                                "references/guides/rest-automation/rest-grammar.md",
                                "references/guides/rest-automation/rest-automation.json",
                                "references/fixtures/rest-bindings.yaml")));
    }
}
