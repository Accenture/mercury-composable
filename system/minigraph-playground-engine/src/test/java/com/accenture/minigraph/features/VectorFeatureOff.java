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

package com.accenture.minigraph.features;

import com.accenture.minigraph.annotations.FetchFeature;
import com.accenture.minigraph.common.FeatureRunner;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.MultiLevelMap;

/**
 * Registration-metadata conformance fixture (see test resource registration-vectors/feature.json):
 * the negated @OptionalService condition evaluates FALSE under the assumed configuration, so this
 * feature must never register - the golden vectors list it under "gatedOut".
 */
@FetchFeature("vector-feature-off")
@OptionalService("!vector.feature.on")
public class VectorFeatureOff implements FeatureRunner {

    @Override
    public boolean runBefore() {
        return false;
    }

    @Override
    public void execute(AsyncHttpRequest request, EventEnvelope response, MultiLevelMap stateMachine,
                        String nodeName) {
        // conformance fixture - no behavior required
    }
}
