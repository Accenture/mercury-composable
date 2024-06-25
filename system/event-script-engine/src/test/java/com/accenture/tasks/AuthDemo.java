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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@PreLoad(route="v1.api.auth", instances=10)
public class AuthDemo implements TypedLambdaFunction<AsyncHttpRequest, Boolean> {
    private static final Logger log = LoggerFactory.getLogger(AuthDemo.class);
    @Override
    public Boolean handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws AppException {
        String auth = input.getHeader("Authorization");
        log.info("Authorization = {}", auth);
        if ("demo".equals(auth)) {
            return true;
        } else {
            throw new AppException(401, "Unauthorized");
        }
    }
}
