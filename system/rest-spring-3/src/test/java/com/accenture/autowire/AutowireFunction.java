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

package com.accenture.autowire;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.LambdaFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@PreLoad(route = "v1.autowire.test")
public class AutowireFunction implements LambdaFunction {
    @Autowired
    TestBean testBean;

    @Value("${value.injection.test}")
    String injectedValue;

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
        return Map.of("success", testBean != null && "someValue".equals(injectedValue));
    }
}
