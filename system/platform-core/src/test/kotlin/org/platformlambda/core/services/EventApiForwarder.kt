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

@PreLoad(route="event.api.forwarder", instances=10)
class EventApiForwarder: KotlinLambdaFunction<EventEnvelope, Any> {
    override suspend fun handleEvent(headers: Map<String, String>, input: EventEnvelope, instance: Int): Any {
        val po = PostOffice(headers, instance)
        val util = Utility.getInstance()
        val timeout = util.str2long(headers["timeout"])
        val endpoint = headers["endpoint"]
        val authorization = headers["authorization"]
        if (input.body is ByteArray) {
            return Mono.create({ callback: MonoSink<EventEnvelope> ->
                val event = EventEnvelope(input.body as ByteArray)
                if (endpoint != null && authorization != null) {
                    val rpc = "true" == headers["rpc"]
                    val securityHeaders: MutableMap<String, String> = HashMap()
                    securityHeaders["Authorization"] = authorization
                    po.asyncRequest(event, timeout, securityHeaders, endpoint, rpc)
                        .onSuccess({ res: EventEnvelope ->
                            callback.success(res)
                        })
                } else {
                    po.asyncRequest(event, timeout)
                        .onSuccess({ res: EventEnvelope ->
                            callback.success(res)
                        })
                }
            })
        }
        throw IllegalArgumentException("Invalid request")
    }
}