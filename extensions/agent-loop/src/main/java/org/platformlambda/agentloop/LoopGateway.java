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

package org.platformlambda.agentloop;

import org.platformlambda.core.models.EventEnvelope;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * How the loop reaches the routes it calls: the model turn ({@code llm.chat}) and each tool's route. A request
 * that times out answers with status 408 rather than throwing, so a tool's timeout becomes its error result.
 */
public interface LoopGateway {

    /**
     * Whether a route is served: a local function, the mesh, or a declared Event-over-HTTP target.
     *
     * @param route the route
     * @return true when a request to it can be delivered
     */
    boolean reachable(String route);

    /**
     * Send one model turn to {@code llm.chat}.
     *
     * @param request the turn's request body
     * @param timeoutMs how long to wait
     * @return the response; a timeout is status 408
     */
    CompletableFuture<EventEnvelope> chat(Map<String, Object> request, long timeoutMs);

    /**
     * Call a tool's route with the model's input as the event body.
     *
     * @param route the route
     * @param body the input
     * @param timeoutMs how long to wait
     * @return the response; a timeout is status 408
     */
    CompletableFuture<EventEnvelope> call(String route, Object body, long timeoutMs);
}
