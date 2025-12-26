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

package org.platformlambda.core.mock;

import org.platformlambda.common.JacksonSerializer;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.Map;

@PreLoad(route = "v1.reactive.flux.custom.serializer", customSerializer = JacksonSerializer.class)
public class FluxCustomSerializationFunction implements TypedLambdaFunction<PoJo, Flux<PoJo>> {
    private static final Logger log = LoggerFactory.getLogger(FluxCustomSerializationFunction.class);

    private static final String EXCEPTION = "exception";
    @Override
    public Flux<PoJo> handleEvent(Map<String, String> headers, PoJo input, int instance) {
        log.info("GOT {} {}", headers, input.getLongNumber());
        return Flux.create(emitter -> {
            if (headers.containsKey(EXCEPTION)) {
                emitter.error(new AppException(400, headers.get(EXCEPTION)));
            } else {
                // just generate two messages
                var pojo = new PoJo();
                pojo.setName("first_one");
                emitter.next(pojo);
                emitter.next(input);
                emitter.complete();
            }
        });
    }
}
