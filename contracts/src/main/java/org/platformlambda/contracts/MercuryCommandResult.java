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

import java.util.Objects;

/** Result returned by the read-only Mercury contract command surface. */
public record MercuryCommandResult(boolean ok, String code, String output) {
    public MercuryCommandResult {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(output, "output");
    }

    public static MercuryCommandResult success(String output) {
        return new MercuryCommandResult(true, "OK", output);
    }

    public static MercuryCommandResult failure(ContractError error) {
        return new MercuryCommandResult(false, error.name(), error.message());
    }
}
