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

package org.platformlambda.automation.http;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.platformlambda.automation.config.RoutingEntry;
import org.platformlambda.automation.models.AssignedRoute;
import org.platformlambda.automation.models.AsyncContextHolder;
import org.platformlambda.automation.services.HttpRouter;
import org.platformlambda.core.util.Utility;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class HttpRequestHandler implements Handler<HttpServerRequest> {
    private static final String ACCEPT = "Accept";
    private static final String POST = "POST";
    private static final String DATE = "Date";
    private static final String WS_PREFIX = "/ws/";
    private static final String KEEP_ALIVE = "keep-alive";
    private static final String CONNECTION_HEADER = "Connection";
    private final HttpRouter gateway;
    private final ConcurrentMap<String, AsyncContextHolder> contexts;

    public HttpRequestHandler(HttpRouter gateway) {
        this.gateway = gateway;
        this.contexts = gateway.getContexts();
    }

    @Override
    public void handle(HttpServerRequest request) {
        Utility util = Utility.getInstance();
        HttpServerResponse response = request.response();
        response.putHeader(DATE, util.getHtmlDate(new Date()));
        String connectionType = request.getHeader(CONNECTION_HEADER);
        if (KEEP_ALIVE.equals(connectionType)) {
            response.putHeader(CONNECTION_HEADER, KEEP_ALIVE);
        }
        String uri = util.getDecodedUri(request.path());
        String method = request.method().name();
        String requestId = util.getUuid();
        AsyncContextHolder holder = new AsyncContextHolder(request);
        String acceptContent = request.getHeader(ACCEPT);
        if (acceptContent != null) {
            holder.setAccept(acceptContent);
        }
        contexts.put(requestId, holder);
        RoutingEntry re = RoutingEntry.getInstance();
        AssignedRoute route = uri.startsWith(WS_PREFIX)? null : re.getRouteInfo(method, uri);
        int status = 200;
        String error = null;
        if (route == null) {
            status = 404;
            error = "Resource not found";
        } else if (route.info == null) {
            status = 405;
            error = "Method not allowed";
        } else {
            holder.setTimeout(route.info.timeoutSeconds * 1000L);
            if (POST.equals(method) && route.info.upload) {
                try {
                    request.setExpectMultipart(true);
                    request.pause();
                } catch (Exception e) {
                    status = 400;
                    error = e.getMessage();
                }
            }
        }
        gateway.handleEvent(route, requestId, status, error);
    }
}
