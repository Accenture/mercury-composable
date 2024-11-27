/*

    Copyright 2018-2024 Accenture Technology

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

package org.platformlambda.core.mock

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.exception.AppException
import org.platformlambda.core.models.KotlinLambdaFunction
import org.platformlambda.core.system.PostOffice
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink

@PreLoad(route = "v1.reactive.flux.kotlin")
class FluxKotlinFunction : KotlinLambdaFunction<Map<String, Any>?, Flux<Map<String, Any>>> {
    private val log: Logger = LoggerFactory.getLogger(FluxKotlinFunction::class.java)
    private val exception: String = "exception"

    override suspend fun handleEvent(
        headers: Map<String, String>,
        input: Map<String, Any>?,
        instance: Int
    ): Flux<Map<String, Any>> {
        val po = PostOffice(headers, instance)
        po.annotateTrace("reactive", "test")
        log.info("GOT {} {}", headers, input)
        return Flux.create( { emitter: FluxSink<Map<String, Any>> ->
            if (headers.containsKey(exception)) {
                emitter.error(AppException(400, headers[exception]))
            } else {
                // just generate two messages
                emitter.next(mapOf("first" to "message"))
                if (input != null) {
                    emitter.next(input)
                }
                emitter.complete()
            }
        })
    }
}