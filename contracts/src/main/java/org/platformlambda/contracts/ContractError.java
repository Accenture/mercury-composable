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

/** Stable public failures. Messages never contain exception text or local paths. */
public enum ContractError {
    INVALID_COMMAND("Command is not supported"),
    UNKNOWN_CONTRACT("Contract is not installed"),
    CONTRACT_VERSION_MISMATCH("Installed contract providers are incompatible"),
    INVALID_EXPORT_ROOT("Export root is not a trusted directory"),
    EXPORT_EXISTS("Mercury platform skill already exists"),
    EXPORT_INTEGRITY_FAILED("Exported skill failed integrity verification"),
    EXPORT_IO_FAILED("Mercury platform skill could not be exported"),
    EXPORT_CLEANUP_FAILED("Incomplete Mercury platform skill could not be cleaned up");

    private final String message;

    ContractError(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
