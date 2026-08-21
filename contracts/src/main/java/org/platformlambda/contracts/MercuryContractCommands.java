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

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/** Mercury vocabulary for read-only contract discovery. */
public final class MercuryContractCommands {
    public static final String ROUTE = "platform.command.service";

    private final ContractRegistry registry;

    public MercuryContractCommands(ContractRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public MercuryCommandResult execute(String input) {
        var command = input == null ? "" : input.trim().replaceAll("\\s+", " ");
        var normalized = command.toLowerCase(Locale.ROOT);
        if ("help mercury".equals(normalized)) {
            return MercuryCommandResult.success(SkillResources.readText("SKILL.md"));
        }
        if ("list contracts".equals(normalized)) {
            var output = registry.contracts().stream()
                    .map(contract -> contract.id() + " - " + contract.summary())
                    .collect(Collectors.joining("\n"));
            return MercuryCommandResult.success(output.isEmpty()
                    ? "No Mercury contracts installed" : output);
        }
        var words = normalized.split(" ");
        if (words.length == 3 && "describe".equals(words[0]) && "contract".equals(words[1])) {
            var contract = registry.get(words[2]);
            return contract == null ? MercuryCommandResult.failure(ContractError.UNKNOWN_CONTRACT)
                    : MercuryCommandResult.success(describe(contract));
        }
        return MercuryCommandResult.failure(ContractError.INVALID_COMMAND);
    }

    private static String describe(MercuryContract contract) {
        var anchors = contract.behaviorAnchors().stream().map(Class::getName)
                .collect(Collectors.joining(", "));
        return "Contract: " + contract.id() + '\n'
                + "Module: " + contract.module() + '\n'
                + "Purpose: " + contract.summary() + '\n'
                + "Behavior anchors: " + anchors + '\n'
                + "References:\n- " + String.join("\n- ", contract.references());
    }
}
