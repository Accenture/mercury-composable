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

package com.accenture.services.plugins.gate;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

/**
 * Positive fixture for the allowlist gate: a nested helper class and a lambda,
 * all within the allowlist. Nested classes are analyzed with the plugin (their
 * bodies validated) rather than flagged as foreign types - this must register.
 */
@SimplePlugin
public class GateInnerOkPlugin implements PluginFunction {

    @Override
    public String getName() {
        return "gateInnerOk";
    }

    @Override
    public Object calculate(Object... input) {
        java.util.function.Supplier<String> viaLambda = () -> Helper.tag(input.length);
        return viaLambda.get();
    }

    private static class Helper {
        private static String tag(int n) {
            return "inner-" + n;
        }
    }
}
