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

import java.util.function.Supplier;

/**
 * Negative fixture for the allowlist gate: the disallowed reference (java.net)
 * hides inside a LAMBDA body that is never invoked. The compiled class carries
 * it in a synthetic lambda method - the body scan must still find it.
 */
@SimplePlugin
public class DisallowedLambdaPlugin implements PluginFunction {

    @Override
    public String getName() {
        return "disallowedLambda";
    }

    @Override
    public Object calculate(Object... input) {
        return (Supplier<Object>) () -> {
            try {
                return new java.net.Socket("127.0.0.1", 80);
            } catch (java.io.IOException e) {
                return null;
            }
        };
    }
}
