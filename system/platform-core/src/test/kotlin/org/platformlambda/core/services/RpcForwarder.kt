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
import org.platformlambda.core.system.PostOffice
import org.platformlambda.core.util.Utility
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink

@PreLoad(route="rpc.forwarder", instances=200)
class RpcForwarder: KotlinLambdaFunction<EventEnvelope, Any> {
    override suspend fun handleEvent(headers: Map<String, String>, input: EventEnvelope, instance: Int): Any {
        val po = PostOffice(headers, instance)
        val target = headers["target"] ?: throw IllegalArgumentException("Missing target service")
        val timeout = headers["timeout"] ?: throw IllegalArgumentException("Missing timeout value")
        val request = EventEnvelope().setTo(target).setBody(input.body)
        val timeoutMs = 500L.coerceAtLeast(Utility.getInstance().str2long(timeout))
        return Mono.create({ callback: MonoSink<EventEnvelope> ->
            po.asyncRequest(request, timeoutMs)
                .onSuccess({ res: EventEnvelope ->
                    callback.success(res)
                })
        })
    }
}