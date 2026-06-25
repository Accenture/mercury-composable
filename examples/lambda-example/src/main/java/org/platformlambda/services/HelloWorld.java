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

package org.platformlambda.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@PreLoad(route="hello.world", instances=10)
public class HelloWorld implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(HelloWorld.class);

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws AppException {
        var po = new PostOffice(headers, instance);
        log.info("echo #{} got a request", instance);
        Map<String, Object> result = new HashMap<>();
        result.put("body", input);
        result.put("instance", instance);
        result.put("origin", Platform.getInstance().getOrigin());
        // forward event to hello.pojo so we can see the span-id of "hello.world" propagated to "hello.pojo"
        po.send(new EventEnvelope().setTo("hello.pojo").setHeader("id", 1));
        return result;
    }
}
