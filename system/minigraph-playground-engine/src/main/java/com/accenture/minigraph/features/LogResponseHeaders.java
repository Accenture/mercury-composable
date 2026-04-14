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

@FetchFeature("log-response-headers")
public class LogResponseHeaders implements FeatureRunner {

    @Override
    public boolean runBefore() {
        return false;
    }

    @Override
    public void execute(AsyncHttpRequest request, EventEnvelope response, MultiLevelMap stateMachine, String nodeName) {
        // this feature logs response headers to the state machine under the calling API fetcher's namespace
        if (response != null && stateMachine != null) {
            for (var kv : response.getHeaders().entrySet()) {
                stateMachine.setElement(nodeName + ".header.response." + kv.getKey(), kv.getValue());
            }
        }
    }
}
