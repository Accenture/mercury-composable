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

package org.platformlambda.servlets;

import io.vertx.core.Future;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleXmlWriter;
import org.platformlambda.core.services.ActuatorServices;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

public abstract class ServletBase extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ServletBase.class);
    private static final SimpleXmlWriter xml = new SimpleXmlWriter();

    private static final String TYPE = "type";
    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_CONTENT = ACCEPT.toLowerCase();
    private static final String CONTENT_TYPE = "content-type";
    private static final String HOST = "host";
    private static final String APP_INSTANCE = "X-App-Instance";
    protected static final AppConfigReader config = AppConfigReader.getInstance();
    protected static final boolean PROTECT_ENDPOINT = "true".equals(
            config.getProperty("protect.info.endpoints", "false"));

    protected void submit(String type, HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String myOrigin = Platform.getInstance().getOrigin();
        final String appOrigin = request.getHeader(APP_INSTANCE);
        if (PROTECT_ENDPOINT && !isIntranetAddress(request) && !myOrigin.equals(appOrigin)) {
            response.sendError(404, "Resource not found");
            return;
        }
        final String origin = appOrigin == null? myOrigin : appOrigin;
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event = new EventEnvelope().setHeader(TYPE, type);
        String accept = request.getHeader(ACCEPT);
        if (accept == null) {
            accept = MediaType.APPLICATION_JSON_VALUE;
        } else {
            // avoid CR/LF attack
            accept = accept.replace("\r", "").replace("\n", "");
        }
        event.setHeader(ACCEPT_CONTENT, accept).setTo(ActuatorServices.ACTUATOR_SERVICES);
        AsyncContext context = request.startAsync();
        Future<EventEnvelope> result = po.asyncRequest(event, 10000);
        result.onSuccess(evt -> {
            Utility util = Utility.getInstance();
            HttpServletResponse res = (HttpServletResponse) context.getResponse();
            res.setStatus(evt.getStatus());
            final Object data = evt.getRawBody();
            final String contentType = evt.getHeaders().getOrDefault(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            final byte[] b;
            if (MediaType.TEXT_PLAIN_VALUE.equals(contentType) && data instanceof String text) {
                res.setContentType(MediaType.TEXT_PLAIN_VALUE);
                b = util.getUTF(text);
            } else {
                if (MediaType.APPLICATION_XML_VALUE.equals(contentType)) {
                    res.setContentType(MediaType.APPLICATION_XML_VALUE);
                    if (data instanceof Map) {
                        b = util.getUTF(xml.write(data));
                    } else {
                        b = util.getUTF(data == null? "" : data.toString());
                    }
                } else {
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    if (data instanceof Map) {
                        b = SimpleMapper.getInstance().getMapper().writeValueAsBytes(data);
                    } else {
                        b = util.getUTF(data == null? "" : data.toString());
                    }
                }
            }
            res.setContentLength(b.length);
            try {
                res.getOutputStream().write(b);
            } catch (IOException e) {
                log.error("Unable to send HTTP response", e);
            }
            context.complete();
        });
        result.onFailure(ex -> {
            HttpServletResponse res = (HttpServletResponse) context.getResponse();
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try {
                res.sendError(408, "Target application timeout");
            } catch (IOException e) {
                log.error("Unable to send HTTP response", e);
            }
            context.complete();
        });
    }

    private boolean isIntranetAddress(HttpServletRequest request) {
        return Utility.getInstance().isIntranetAddress(request.getHeader(HOST));
    }
}
