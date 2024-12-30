/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core.services

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.models.EventEnvelope
import org.platformlambda.core.models.KotlinLambdaFunction
import org.platformlambda.core.system.FastRPC
import org.platformlambda.core.util.Utility

@PreLoad(route="rpc.forwarder", instances=200)
class RpcForwarder: KotlinLambdaFunction<EventEnvelope, EventEnvelope> {
    override suspend fun handleEvent(headers: Map<String, String>, input: EventEnvelope, instance: Int): EventEnvelope {
        val fastRPC = FastRPC(headers)
        val target = headers["target"] ?: throw IllegalArgumentException("Missing target service")
        val timeout = headers["timeout"] ?: throw IllegalArgumentException("Missing timeout value")
        val request = EventEnvelope().setTo(target).setBody(input.body)
        return fastRPC.awaitRequest(request, 500L.coerceAtLeast(Utility.getInstance().str2long(timeout)))
    }
}