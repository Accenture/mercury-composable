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

package com.accenture.examples.services;

import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.Platform;

import java.util.HashMap;
import java.util.Map;

/**
 * This demonstrates preloading of user function
 * <p>
 * IMPORTANT:
 * You can use Kernel thread when dealing with computational intensive tasks.
 * <p>
 * This example function is not computational intensive. Therefore, it does not
 * represent the ideal use of kernel threads.
 * <p>
 * Using kernel thread for reactive code is safe because the function will
 * exit very quickly and thus the kernel thread is not held up for a long time.
 * <p>
 * For function that makes blocking RPC calls, you should use virtual thread
 * (the default setting without the KernelThreadRunner annotation). It would
 * provide higher throughput.
 */
@KernelThreadRunner
@PreLoad(route="hello.world", instances=20)
public class HelloWorld implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {            Map<String, Object> result = new HashMap<>();
        result.put("body", input);
        result.put("instance", instance);
        result.put("origin", Platform.getInstance().getOrigin());
        return result;
    }
}
