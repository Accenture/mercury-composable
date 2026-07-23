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
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Demo authentication service for the Event-over-HTTP endpoint.<p>
 *
 * The rest.yaml entry for "POST /api/event" declares
 * {@code authentication: 'event.api.auth'}, so every incoming Event API request
 * is delivered here first. This demo compares the caller's "authorization"
 * header against a shared token that both peers resolve from the environment
 * ({@code demo.peer.token=${DEMO_PEER_TOKEN:demo}} in application.properties) -
 * never hard-code a real credential in source or config files.<p>
 *
 * Returning an EventEnvelope with a boolean body tells the REST automation
 * engine to continue (true) or reject with HTTP-401 (false). Additional
 * headers on the envelope become session info that rides to the target
 * function as read-only headers - the "user" header in this demo.
 * Replace this class with your own OAuth 2.0 bearer-token validation for
 * production use.
 */
@PreLoad(route="event.api.auth", instances=10)
public class EventApiAuth implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final Logger log = LoggerFactory.getLogger(EventApiAuth.class);

    private static final String AUTHORIZATION = "authorization";
    private static final String DEMO_TOKEN_KEY = "demo.peer.token";

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        AppConfigReader config = AppConfigReader.getInstance();
        String expected = config.getProperty(DEMO_TOKEN_KEY, "demo");
        boolean authorized = expected.equals(input.getHeader(AUTHORIZATION));
        log.info("Event API authorization {} {} = {}", input.getMethod(), input.getUrl(),
                authorized? "PASS" : "FAIL");
        return new EventEnvelope().setBody(authorized).setHeader("user", "demo");
    }
}
