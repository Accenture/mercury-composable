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

package com.accenture.minigraph.tests;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * Token-free stand-in for the AI node (agent-orchestration experiment E0).
 * <p>
 * In production the support-triage graph reaches 'llm.chat' on a polyglot wrapper
 * through declarative Event-over-HTTP; in this test scope the same route name
 * resolves to this mock - the route-decoupling contract is what makes the LLM
 * node mockable by configuration, so the graph is testable without tokens or
 * credentials. The reply mirrors the 'llm.chat' contract (data / usage / model /
 * stop_reason) with a deterministic verdict derived from the prompt.
 */
@PreLoad(route = "llm.chat", instances = 10)
public class MockLlmChat implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String prompt = String.valueOf(input.get("prompt"));
        String label = prompt.contains("crash")? "bug" :
                        prompt.contains("wish") || prompt.contains("feature")? "feature" : "question";
        return Map.of(
                "data", Map.of("label", label, "reason", "mock verdict for the token-free test"),
                "usage", Map.of("input_tokens", 42, "output_tokens", 7),
                "model", "mock-llm",
                "stop_reason", "end_turn");
    }
}
