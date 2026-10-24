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

package org.platformlambda.dynatrace.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The REST endpoint's function: one more hop, an RPC to demo.echo, before it answers.
 */
@PreLoad(route = "demo.hello", instances = 5)
public class DemoHello implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
        PostOffice po = new PostOffice(headers, instance);
        EventEnvelope echo = po.request(new EventEnvelope().setTo("demo.echo").setBody("hello"), 5000)
                .get(5000, TimeUnit.MILLISECONDS);
        return Map.of("echo", String.valueOf(echo.getBody()));
    }
}
