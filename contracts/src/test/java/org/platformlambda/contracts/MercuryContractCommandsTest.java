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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercuryContractCommandsTest {

    private final MercuryContractCommands commands = new MercuryContractCommands(
            ContractRegistry.of(List.of(
                    ContractRegistryTest.provider("event-script-provider",
                            ContractRegistryTest.contract("event-script")))));

    @Test
    void helpUsesThePackagedSkillEntrypoint() {
        var result = commands.execute("help mercury");
        assertTrue(result.ok());
        assertEquals(SkillResources.readText("SKILL.md"), result.output());
    }

    @Test
    void listAndDescribeUseMercuryContractVocabulary() {
        var listed = commands.execute("list contracts");
        assertTrue(listed.ok());
        assertTrue(listed.output().startsWith("event-script - "));

        var described = commands.execute("describe contract event-script");
        assertTrue(described.ok());
        assertTrue(described.output().contains("Behavior anchors: java.lang.String"));
        assertTrue(described.output().contains("references/contract-index.md"));
    }

    @Test
    void unsupportedAndUnknownCommandsHaveStableErrors() {
        var invalid = commands.execute("export skill as anything");
        assertFalse(invalid.ok());
        assertEquals("INVALID_COMMAND", invalid.code());
        assertEquals(ContractError.INVALID_COMMAND.message(), invalid.output());

        var unknown = commands.execute("describe contract missing");
        assertFalse(unknown.ok());
        assertEquals("UNKNOWN_CONTRACT", unknown.code());
    }
}
