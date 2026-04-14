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

package com.accenture.minigraph.common;

import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.util.MultiLevelMap;

public interface FeatureRunner {

    /**
     * Run before or after an HTTP request
     *
     * @return true if this feature needs to run before the HTTP request
     */
    public boolean runBefore();

    /**
     * Execute this feature. If the feature runs before an HTTP request, the response object is not provided.
     * Pre-processing ("runBefore") is used to update the HTTP request object.
     * Post-processing ("runAfter") is used to read the HTTP response.
     * In both case, the feature function may read/write the state machine.
     * <p>
     * For example, "oauth-bearer" feature is designed to acquire an access token and set the
     * "Authorization" header before running an HTTP request.
     * <p>
     * The "log-request-headers" feature is designed to run before an HTTP request is made.
     * The "log-response-headers" feature logs response headers after an HTTP response is received.
     * The request/response headers are saved into the state machine of a graph instance model under
     * the keys "header.request" and "header.response" in the fetcher's node respectively.
     * <p>
     * The two header logging features are provided as a built-in feature to demonstrate how to write a FeatureRunner.
     * <p>
     * To implement a feature runner, your feature class must implement the FeatureRunner interface
     * with the "FetchFeature" annotation.
     *
     * @param request for the HTTP call
     * @param response event is provided for post-processing only
     * @param stateMachine for a graph instance
     * @param nodeName for updating the state machine if needed
     */
    public void execute(AsyncHttpRequest request, EventEnvelope response, MultiLevelMap stateMachine, String nodeName);
}
