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

package org.platformlambda.contracts;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractRegistryTest {

    @Test
    void providersAndContractsAreDeterministicallyOrdered() {
        var registry = ContractRegistry.of(List.of(
                provider("z-provider", contract("rest-automation")),
                provider("a-provider", contract("event-script"))));

        assertEquals(List.of("a-provider", "z-provider"),
                registry.providers().stream().map(MercuryContractProvider::providerId).toList());
        assertEquals(List.of("event-script", "rest-automation"),
                registry.contracts().stream().map(MercuryContract::id).toList());
    }

    @Test
    void duplicateContractsFailClosed() {
        var error = assertThrows(ContractException.class, () -> ContractRegistry.of(List.of(
                provider("a-provider", contract("event-script")),
                provider("b-provider", contract("event-script")))));

        assertEquals(ContractError.CONTRACT_VERSION_MISMATCH, error.getError());
    }

    @Test
    void mixedContractBuildsFailClosed() {
        var oldProvider = new MercuryContractProvider() {
            @Override
            public String providerId() {
                return "old-provider";
            }

            @Override
            public String contractBuildId() {
                return ContractBuild.ID + "-old";
            }

            @Override
            public Collection<MercuryContract> contracts() {
                return List.of(contract("minigraph"));
            }
        };

        var error = assertThrows(ContractException.class,
                () -> ContractRegistry.of(List.of(oldProvider)));
        assertEquals(ContractError.CONTRACT_VERSION_MISMATCH, error.getError());
    }

    @Test
    void providerResultsAreValidatedAndSnapshotted() {
        var supplied = new ArrayList<>(List.of(contract("platform-core")));
        var mutableProvider = new MercuryContractProvider() {
            @Override
            public String providerId() {
                return "stable-provider";
            }

            @Override
            public String contractBuildId() {
                return ContractBuild.ID;
            }

            @Override
            public Collection<MercuryContract> contracts() {
                return supplied;
            }
        };
        var registry = ContractRegistry.of(List.of(mutableProvider));
        supplied.clear();

        assertEquals(List.of("platform-core"),
                registry.contracts().stream().map(MercuryContract::id).toList());

        var invalidProvider = provider("bad`provider", contract("event-script"));
        var error = assertThrows(ContractException.class,
                () -> ContractRegistry.of(List.of(invalidProvider)));
        assertEquals(ContractError.CONTRACT_VERSION_MISMATCH, error.getError());
    }

    @Test
    void contractTextCannotInjectGeneratedMarkdown() {
        assertThrows(IllegalArgumentException.class, () -> new MercuryContract(
                "platform-core", "platform-core", "Injected\n## heading",
                List.of(String.class), List.of("references/contract-index.md")));
    }

    @Test
    void malformedServiceLoaderInputHasAStableFailure() {
        var broken = new ClassLoader(null) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if (name.equals("META-INF/services/" + MercuryContractProvider.class.getName())) {
                    throw new IOException("untrusted classpath detail");
                }
                return super.getResources(name);
            }
        };

        var error = assertThrows(ContractException.class, () -> ContractRegistry.load(broken));
        assertEquals(ContractError.CONTRACT_VERSION_MISMATCH, error.getError());
        assertEquals(ContractError.CONTRACT_VERSION_MISMATCH.message(), error.getMessage());
    }

    static MercuryContractProvider provider(String id, MercuryContract contract) {
        return new MercuryContractProvider() {
            @Override
            public String providerId() {
                return id;
            }

            @Override
            public String contractBuildId() {
                return ContractBuild.ID;
            }

            @Override
            public Collection<MercuryContract> contracts() {
                return List.of(contract);
            }
        };
    }

    static MercuryContract contract(String id) {
        return new MercuryContract(id, "test-module", "Test contract", List.of(String.class),
                List.of("references/contract-index.md"));
    }
}
