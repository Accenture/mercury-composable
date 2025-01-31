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

import org.platformlambda.core.models.EventEnvelope
import org.platformlambda.core.models.KotlinLambdaFunction
import org.platformlambda.core.system.PostOffice
import org.platformlambda.core.util.Utility
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.util.*


/**
 * Demonstrate non-blocking RPC for single request and fork-n-join
 */
class LongRunningRpcSimulator : KotlinLambdaFunction<EventEnvelope, Any> {

    override suspend fun handleEvent(headers: Map<String, String>, input: EventEnvelope, instance: Int): Any {
        val po = PostOffice(headers, instance)
        val util = Utility.getInstance()
        po.annotateTrace("demo", "long-running")
        po.annotateTrace("time", util.date2str(Date()))
        val timeout = Utility.getInstance().str2long(headers.getOrDefault(TIMEOUT, "5000"))
        return Mono.create({ callback: MonoSink<EventEnvelope> ->
            if (headers.containsKey(FORK_N_JOIN)) {
                val forward = EventEnvelope().setTo(HELLO_WORLD).setHeaders(headers).setHeader(BODY, input.body)
                val requests  = ArrayList<EventEnvelope>()
                // create a list of 4 request events
                for (i in 0..3) {
                    requests.add(EventEnvelope(forward.toBytes()).setBody(i).setCorrelationId("cid-$i"))
                }
                val consolidated = HashMap<String, Any>()
                po.asyncRequest(requests, timeout, false)
                    .onSuccess({ response: List<EventEnvelope> ->
                        for (res in response) {
                            if (res.status == 200) {
                                consolidated[res.correlationId] = res.body
                            }
                        }
                        callback.success(EventEnvelope().setBody(consolidated))
                    })
            }
            else {
                // simulate delay of one second by setting body to even number
                val forward = EventEnvelope().setTo(HELLO_WORLD).setBody(2)
                                .setHeaders(headers).setHeader(BODY, input.body)
                po.asyncRequest(forward, timeout, false)
                    .onSuccess({ res: EventEnvelope ->
                        callback.success(res)
                    })
            }
        })
    }

    companion object {
        private const val HELLO_WORLD = "hello.world"
        private const val FORK_N_JOIN = "fork-n-join"
        private const val TIMEOUT = "timeout"
        private const val BODY = "body"
    }

}