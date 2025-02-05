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

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.services.ActuatorServices;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;

import java.io.IOException;
import java.io.Serial;
import java.util.Date;

@WebServlet("/shutdown")
public class ShutdownServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 37489647906664051L;

    private static final long GRACE_PERIOD = 5000;
    private static final String APP_INSTANCE = "X-App-Instance";
    private static final String TYPE = "type";
    private static final String SHUTDOWN = "shutdown";
    private static final String USER = "user";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(404, "Not Found");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String origin = request.getHeader(APP_INSTANCE);
        if (origin == null) {
            response.sendError(400, "Missing "+ APP_INSTANCE +" in request header");
            return;
        }
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event = new EventEnvelope().setHeader(TYPE, SHUTDOWN);
        if (origin.equals(Platform.getInstance().getOrigin())) {
            event.setTo(ActuatorServices.ACTUATOR_SERVICES);
        } else {
            if (!po.exists(origin)) {
                response.sendError(404, "Target not reachable");
                return;
            }
            event.setTo(ActuatorServices.ACTUATOR_SERVICES+"@"+origin);
        }
        event.setHeader(USER, System.getProperty("user.name"));
        po.sendLater(event, new Date(System.currentTimeMillis() + GRACE_PERIOD));
        response.sendError(200, "Target will be shutdown in "+GRACE_PERIOD+" ms");
    }

}
