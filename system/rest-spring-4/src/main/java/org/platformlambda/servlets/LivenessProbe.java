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

package org.platformlambda.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;

@WebServlet(urlPatterns={"/livenessprobe"}, asyncSupported=true)
public class LivenessProbe extends ServletBase {
    private static final Logger log = LoggerFactory.getLogger(LivenessProbe.class);

    @Serial
    private static final long serialVersionUID = 3607030982796747671L;
    private static final String LIVENESS_PROBE = "livenessprobe";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            submit(LIVENESS_PROBE, request, response);
        } catch (Exception e) {
            log.error("Unable to submit env request", e);
        }
    }
}
