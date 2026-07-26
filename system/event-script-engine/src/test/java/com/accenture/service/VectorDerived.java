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
 * the DERIVED plugin name - Java lowercases the first letter of this class's simple name and the
 * Rust carrier camelCases "fn vector_derived", so idiomatic declarations in both languages
 * register the same "vectorDerived".
 */
@SimplePlugin
public class VectorDerived implements PluginFunction {

    @Override
    public Object calculate(Object... input) {
        return input == null ? 0 : input.length;
    }
}
