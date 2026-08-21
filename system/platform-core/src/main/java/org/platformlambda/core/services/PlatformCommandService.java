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

import org.platformlambda.contracts.ContractException;
import org.platformlambda.contracts.ContractRegistry;
import org.platformlambda.contracts.MercuryCommandResult;
import org.platformlambda.contracts.MercuryContractCommands;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.LambdaFunction;

import java.util.LinkedHashMap;
import java.util.Map;

/** Private, development-only adapter for Mercury operational-contract discovery. */
@OptionalService("app.env=dev")
@PreLoad(route = PlatformCommandService.ROUTE, instances = 10, isPrivate = true)
public class PlatformCommandService implements LambdaFunction {
    public static final String ROUTE = MercuryContractCommands.ROUTE;

    private final MercuryContractCommands commands;

    public PlatformCommandService() {
        this(new MercuryContractCommands(ContractRegistry.load()));
    }

    PlatformCommandService(MercuryContractCommands commands) {
        this.commands = commands;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        try {
            return toMap(commands.execute(input instanceof String text ? text : ""));
        } catch (ContractException e) {
            return Map.of(
                    "ok", false,
                    "code", e.getError().name(),
                    "output", e.getError().message());
        } catch (RuntimeException e) {
            return Map.of(
                    "ok", false,
                    "code", "CONTRACT_VERSION_MISMATCH",
                    "output", "Installed contract providers are incompatible");
        }
    }

    private static Map<String, Object> toMap(MercuryCommandResult result) {
        var response = new LinkedHashMap<String, Object>();
        response.put("ok", result.ok());
        response.put("code", result.code());
        response.put("output", result.output());
        return response;
    }
}
