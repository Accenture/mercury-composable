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

package com.accenture.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;

import java.util.Collections;
import java.util.Map;

/**
 * Demonstrates the PROGRAMMATIC Event-over-HTTP pattern - the counterpart of the
 * declarative demo in flows/event-over-http-demo.yml.<p>
 *
 * This task calls the peer's "hello.world" function by passing the peer's Event API
 * endpoint URL directly to the PostOffice request API. Because the target address is
 * given programmatically, "hello.world" does NOT appear in event-over-http.yaml -
 * compare with "hello.declarative" (an alias of the same peer function), which is
 * resolved through that configuration file instead. The two REST endpoints
 * "/api/event/http/programmatic" and "/api/event/http/demo" therefore hit the same
 * peer function through the two different patterns.<p>
 *
 * Since this function runs in a virtual thread, future.get() suspends the virtual
 * thread without blocking a kernel thread.
 */
@PreLoad(route="v1.event.over.http.rpc", instances=10)
public class EventOverHttpRpc implements LambdaFunction {

    private static final String HELLO_WORLD = "hello.world";
    private static final String PEER_HOST = "peer.demo.host";
    private static final String PEER_PORT = "peer.demo.port";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
        var po = new PostOffice(headers, instance);
        AppConfigReader config = AppConfigReader.getInstance();
        String host = config.getProperty(PEER_HOST, "127.0.0.1");
        String port = config.getProperty(PEER_PORT, "8085");
        String eventEndpoint = "http://" + host + ":" + port + "/api/event";
        EventEnvelope req = new EventEnvelope().setTo(HELLO_WORLD).setBody(input);
        headers.forEach(req::setHeader);
        EventEnvelope response = po.request(req, 10000, Collections.emptyMap(), eventEndpoint, true).get();
        if (response.getStatus() != 200) {
            throw new AppException(response.getStatus(), String.valueOf(response.getError()));
        }
        return response.getBody();
    }
}
