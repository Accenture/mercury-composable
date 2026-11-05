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

package com.accenture.minigraph.websocket.server;

import com.accenture.minigraph.progress.ReturnChannel;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * Where the flow of a run started from a WebSocket session replies. The run's channel has already sent the result to
 * the session and closed it. Therefore, the reply matters only for a run whose graph never started - a flow that
 * failed before the graph executor ran - which still ends its session with the error.
 */
@ZeroTracing
@OptionalService(GraphRunSocket.ENABLED)
@PreLoad(route = GraphRunReply.ROUTE, instances = 20)
public class GraphRunReply implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String ROUTE = "graph.run.socket.reply";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        var txPath = ReturnChannel.takeSocket(input.getCorrelationId());
        if (txPath != null) {
            GraphRunSocket.refuse(txPath, input.getStatus() >= 400? input.getStatus() : 500,
                    "The run ended before the graph started - " + input.getError());
        }
        return null;
    }
}
