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

package org.platformlambda.graph.redis;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;

/**
 * Minimal entry point for the unit tests - this module is a library, so a test-scope
 * application shell is all the platform needs to boot and run the PreLoad scan.
 */
@MainApplication
public class TestApp implements EntryPoint {
    @Override
    public void start(String[] args) {
        // no-op: the state-store functions self-register through the classpath scan
    }
}
