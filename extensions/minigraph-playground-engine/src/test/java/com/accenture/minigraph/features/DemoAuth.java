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
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.MultiLevelMap;

@FetchFeature("demo-auth")
public class DemoAuth implements FeatureRunner {

    @Override
    public boolean runBefore() {
        return true;
    }

    @Override
    public void execute(AsyncHttpRequest request, EventEnvelope response, MultiLevelMap stateMachine, String nodeName) {
        /*
         * This is a demo feature to add a dummy Authorization header.
         * Your real implementation should acquire client-id and secret from a secret manager
         * and get access token from your company's federated authentication authority in the background.
         * Then apply it as a "bear token" in the header.
         *
         * OK. The demo bearer token is just the fetcher's node name so we can validate it in a unit test.
         */
        if (request != null) {
            request.setHeader("Authorization", "Bearer " + nodeName);
        }
    }
}
