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

package com.accenture.benchmark.reporter;

import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.LambdaFunction;

import java.util.Map;

/**
 * The service under test: a trivial echo. Returning the input body produces a reply, so both the RPC
 * ({@code request}) and callback ({@code asyncRequest}) workloads get a response to time end-to-end.
 *
 * <p>It does no work of its own, so the measured latency is the framework's — event routing, the
 * ServiceQueue mailbox, the ElasticQueue back-pressure buffer (on the callback path), and the reply
 * round-trip. {@code @ZeroTracing} keeps distributed-trace bookkeeping out of the hot path.</p>
 */
@ZeroTracing
public class Worker implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        return input;
    }
}
