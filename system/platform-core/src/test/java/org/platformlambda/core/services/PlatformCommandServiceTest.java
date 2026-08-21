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

package org.platformlambda.core.services;

import org.junit.jupiter.api.Test;
import org.platformlambda.contracts.ContractRegistry;
import org.platformlambda.contracts.MercuryContract;
import org.platformlambda.contracts.providers.PlatformCoreContractProvider;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Feature;
import org.platformlambda.automation.config.RoutingEntry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformCommandServiceTest {

    @Test
    void serviceIsPrivateAndDevelopmentOnly() {
        var optional = PlatformCommandService.class.getAnnotation(OptionalService.class);
        var preLoad = PlatformCommandService.class.getAnnotation(PreLoad.class);
        assertEquals("app.env=dev", optional.value());
        assertEquals(PlatformCommandService.ROUTE, preLoad.route());
        assertTrue(preLoad.isPrivate());

        var prior = System.getProperty("app.env");
        try {
            System.setProperty("app.env", "prod");
            assertFalse(Feature.isRequired(PlatformCommandService.class));
            System.setProperty("app.env", "dev");
            assertTrue(Feature.isRequired(PlatformCommandService.class));
        } finally {
            if (prior == null) {
                System.clearProperty("app.env");
            } else {
                System.setProperty("app.env", prior);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void serviceLoaderFindsThePlatformCoreContracts() {
        var registry = ContractRegistry.load();
        assertEquals(List.of("platform-core-provider"),
                registry.providers().stream().map(provider -> provider.providerId()).toList());
        assertEquals(List.of("platform-core", "rest-automation"),
                registry.contracts().stream().map(contract -> contract.id()).toList());

        var provider = new PlatformCoreContractProvider();
        var contracts = provider.contracts().stream().toList();
        assertEquals(List.of(
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
                                "references/fixtures/rest-bindings.yaml"))),
                contracts);

        var response = (Map<String, Object>) new PlatformCommandService()
                .handleEvent(Map.of(), "list contracts", 0);
        assertEquals(Boolean.TRUE, response.get("ok"));
        assertTrue(String.valueOf(response.get("output")).contains("platform-core - "));

        var export = (Map<String, Object>) new PlatformCommandService()
                .handleEvent(Map.of(), "export skill as anything", 0);
        assertEquals(Boolean.FALSE, export.get("ok"));
        assertEquals("INVALID_COMMAND", export.get("code"));
        assertFalse(String.valueOf(export.get("output")).contains("/"));
    }
}
