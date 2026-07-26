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

package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * Registration-metadata conformance fixture (see test resource registration-vectors/core.json):
 * comma-separated route aliases plus an envInstances key resolved from configuration at boot.
 * The same fixture is declared in every engine's own carrier and must resolve to the identical
 * golden vector entry.
 */
@PreLoad(route = "vector.alias.one, vector.alias.two", instances = 5, envInstances = "vector.instances")
public class VectorAliasFunction implements TypedLambdaFunction<Map<String, Object>, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return input;
    }
}
