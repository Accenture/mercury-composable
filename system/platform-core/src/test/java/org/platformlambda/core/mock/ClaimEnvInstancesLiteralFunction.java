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
import org.platformlambda.core.models.LambdaFunction;

import java.util.Map;

/**
 * Claims-fixture mock for claim id "envinstances-config-key"
 * (see ClaimEnvInstancesConfigKeyTest).
 * <p>
 * envInstances names a configuration KEY that exists nowhere in the merged base
 * configuration, so the engine must fall back to the annotation's instances value (7).
 * <p>
 * NOTE: a raw "${...}" literal in envInstances is deliberately NOT used here - it is
 * fatal at boot (AppConfigReader.getProperty feeds the literal into the composite-key
 * parser, which rejects '$'-rooted keys); ClaimEnvInstancesConfigKeyTest pins that
 * config-layer behavior directly instead.
 */
@PreLoad(route = ClaimEnvInstancesLiteralFunction.ROUTE,
         instances = ClaimEnvInstancesLiteralFunction.ANNOTATION_INSTANCES,
         envInstances = ClaimEnvInstancesLiteralFunction.NON_EXIST_KEY)
public class ClaimEnvInstancesLiteralFunction implements LambdaFunction {

    public static final String ROUTE = "claim.env.instances.fallback";
    public static final int ANNOTATION_INSTANCES = 7;
    public static final String NON_EXIST_KEY = "claim.env.instances.missing.key";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        return input;
    }
}
