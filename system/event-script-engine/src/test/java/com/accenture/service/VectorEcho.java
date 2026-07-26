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

package com.accenture.service;

import com.accenture.models.PluginFunction;
import com.accenture.models.SimplePlugin;

/**
 * Registration-metadata conformance fixture (see test resource registration-vectors/plugin.json):
 * the EXPLICIT plugin name wins over derivation - the Java carrier overrides getName(), the Rust
 * carrier writes the positional string, and both register the same "vectorEcho".
 */
@SimplePlugin
public class VectorEcho implements PluginFunction {

    @Override
    public String getName() {
        return "vectorEcho";
    }

    @Override
    public Object calculate(Object... input) {
        if (input == null || input.length != 1) {
            throw new IllegalArgumentException("One input is required to echo");
        }
        return input[0];
    }
}
