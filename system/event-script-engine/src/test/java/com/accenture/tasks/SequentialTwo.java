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

package com.accenture.tasks;

import com.accenture.models.PoJo;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

@PreLoad(route="sequential.two", instances=10)
public class SequentialTwo implements TypedLambdaFunction<PoJo, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, PoJo input, int instance) {
        return new EventEnvelope().setBody(input).setHeaders(headers);
    }
}
