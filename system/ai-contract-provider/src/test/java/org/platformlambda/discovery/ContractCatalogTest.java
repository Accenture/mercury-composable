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

package org.platformlambda.discovery;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.discovery.models.ContractEntry;
import org.platformlambda.discovery.services.ContractCatalog;
import org.platformlambda.discovery.services.SkillSnapshot;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binds the contract catalog to the code it describes: every behavior anchor must resolve
 * with Class.forName (MiniGraph classes come from a test-scope dependency), so a renamed or
 * removed behavior class fails this module's build even though the app has no runtime
 * dependency on the playground engine.
 */
class ContractCatalogTest {

    @Test
    void catalogListsTheFourMercuryContracts() {
        var contracts = ContractCatalog.getInstance().getContracts();
        assertEquals(List.of("event-script", "minigraph", "platform-core", "rest-automation"),
                contracts.stream().map(ContractEntry::id).toList());
    }

    @Test
    void everyBehaviorAnchorResolvesToARealClass() throws ClassNotFoundException {
        for (ContractEntry contract : ContractCatalog.getInstance().getContracts()) {
            for (String anchor : contract.anchors()) {
                assertNotNull(Class.forName(anchor), anchor);
            }
        }
    }

    @Test
    void everyReferenceIsAMemberOfThePackagedSnapshot() {
        var snapshot = SkillSnapshot.getInstance().getFiles();
        for (ContractEntry contract : ContractCatalog.getInstance().getContracts()) {
            for (String reference : contract.references()) {
                assertTrue(snapshot.containsKey(reference),
                        contract.id() + " references a missing file: " + reference);
            }
        }
    }

    @Test
    void invalidCatalogsFailClosed() {
        for (String fixture : List.of("bad-id", "duplicate-id", "bad-reference")) {
            var reader = new ConfigReader("classpath:/invalid-catalogs/" + fixture + ".yaml");
            assertThrows(IllegalArgumentException.class, () -> ContractCatalog.load(reader), fixture);
        }
    }
}
