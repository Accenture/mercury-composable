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
    private static final String PLAYGROUND_PAGE = "/template/playground.html";
    private static final String PLAIN_PAGE = "/template/index.html";
    private final String content;

    public GetIndexHtml() {
        var config = AppConfigReader.getInstance();
        // The Playground web app is served ONLY in dev mode - app.env=dev, the same gate as every
        // Playground service. Any other value, or no app.env at all, serves the plain service page,
        // so a production deployment never shows the Playground UI. The static public/index.html is
        // that same plain page, so an application that does not route this function never serves
        // the Playground by accident either.
        var resPath = "dev".equals(config.getProperty("app.env"))? PLAYGROUND_PAGE : PLAIN_PAGE;
        log.info("Home page - {}", resPath);
        var in = this.getClass().getResourceAsStream(resPath);
        content = Utility.getInstance().stream2str(in);
    }

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        return new EventEnvelope().setHeader("Content-Type", "text/html; charset=utf-8").setBody(content);
    }
}
