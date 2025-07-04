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

package com.accenture.rest;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@MainApplication
public class MainApp implements EntryPoint {

    @Override
    public void start(String[] args) {
        LambdaFunction f = (headers, input, instance) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("time", new Date());
            result.put("greeting", input);
            return result;
        };
        Platform.getInstance().registerPrivate("hello.world", f, 10);
    }
}
