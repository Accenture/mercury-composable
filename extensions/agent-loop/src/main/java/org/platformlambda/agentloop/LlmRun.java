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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * {@code llm.run}, the agent loop (RFC-0011): a graph node calls it with {@code graph.task}, giving it a tool
 * catalog, a prompt, a mode and limits, and routes on the outcome envelope it returns. See {@link AgentLoop}.
 * <p>
 * Telemetry: the function's span parents every {@code llm.chat} turn and every tool call, and its annotations
 * carry the status, the turn counts, the model and the usage. They never carry a prompt, a completion, a tool's
 * input or a tool's result, and neither does the log line.
 */
@PreLoad(route = "llm.run", instances = 50, envInstances = "worker.instances.llm.run")
public class LlmRun implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(LlmRun.class);

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws AppException {
        var po = new PostOffice(headers, instance);
        var loop = new AgentLoop(new PlatformGateway(po), LoopSettings.Defaults.fromConfig(),
                System::currentTimeMillis);
        var envelope = loop.run(input);
        var status = String.valueOf(envelope.get(AgentLoop.STATUS));
        var reason = envelope.get(AgentLoop.CAP);
        var outcome = reason == null ? status : status + "/" + reason;
        po.annotateTrace(AgentLoop.STATUS, outcome);
        po.annotateTrace(AgentLoop.TURNS, String.valueOf(envelope.get(AgentLoop.TURNS)));
        if (envelope.get(AgentLoop.TURN) != null) {
            po.annotateTrace(AgentLoop.TURN, String.valueOf(envelope.get(AgentLoop.TURN)));
        }
        if (envelope.get(AgentLoop.MODEL) instanceof String model) {
            po.annotateTrace(AgentLoop.MODEL, model);
        }
        po.annotateTrace(AgentLoop.USAGE, (Map<String, Object>) envelope.get(AgentLoop.USAGE));
        log.info("llm.run {} after {} turn(s), conversation {}, model {}", outcome,
                envelope.get(AgentLoop.TURNS), envelope.get(AgentLoop.CONVERSATION), envelope.get(AgentLoop.MODEL));
        return envelope;
    }
}
