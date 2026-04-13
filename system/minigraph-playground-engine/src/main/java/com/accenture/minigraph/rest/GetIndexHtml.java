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

package com.accenture.minigraph.rest;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@PreLoad(route = "get.index.html", instances = 10)
public class GetIndexHtml implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final Logger log = LoggerFactory.getLogger(GetIndexHtml.class);
    private final String content;

    public GetIndexHtml() {
        var config = AppConfigReader.getInstance();
        var location = "dev".equals(config.getProperty("app.env", "dev"))? "/public" : "/template";
        var resPath = location + "/index.html";
        log.info("Home page - {}", resPath);
        var in = this.getClass().getResourceAsStream(resPath);
        content = Utility.getInstance().stream2str(in);
    }

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        return new EventEnvelope().setHeader("Content-Type", "text/html; charset=utf-8").setBody(content);
    }
}
