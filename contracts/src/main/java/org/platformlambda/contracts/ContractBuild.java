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

/**
 * Build identity shared by the Mercury contract API and providers compiled against it.
 *
 * <p>The constants are deliberately compile-time values. Java inlines them into provider
 * implementations so a provider built for another Mercury release is rejected when loaded
 * with this contract registry.</p>
 */
public final class ContractBuild {
    public static final String MERCURY_VERSION = "4.11.9";
    public static final String ID = "platform-contracts/" + MERCURY_VERSION;
    public static final int SCHEMA_VERSION = 1;

    private ContractBuild() {
        // utility class
    }
}
