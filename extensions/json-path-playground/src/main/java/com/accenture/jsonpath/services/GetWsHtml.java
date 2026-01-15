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

package com.accenture.jsonpath.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.util.Map;

@PreLoad(route = "v1.get.ws.html", instances = 10)
public class GetWsHtml implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var in = this.getClass().getResourceAsStream("/template/ws.html");
        if (in == null) {
            throw new IllegalArgumentException("Template not found");
        }
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port");
        if (port == null) {
            throw new IllegalArgumentException("Missing rest.server.port in application.properties");
        }
        var url = "http://127.0.0.1:" + port + "/ws/json/path";
        var html = Utility.getInstance().stream2str(in).replace("$WS_URL", url);
        return new EventEnvelope().setHeader("Content-Type", "text/html; charset=utf-8").setBody(html);
    }
}
