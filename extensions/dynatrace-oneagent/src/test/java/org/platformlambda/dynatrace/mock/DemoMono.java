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
import org.platformlambda.core.models.LambdaFunction;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * A deferred result: the hop returns at once, the result completes later on another thread.
 */
@PreLoad(route = "demo.mono", instances = 5)
public class DemoMono implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        return Mono.delay(Duration.ofMillis(200)).map(ignored -> "late " + input);
    }
}
