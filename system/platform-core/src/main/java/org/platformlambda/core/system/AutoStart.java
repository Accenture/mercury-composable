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

package org.platformlambda.core.system;

import org.platformlambda.core.util.AppConfigReader;

import java.lang.reflect.Method;

public class AutoStart {

    /**
     * This entry point decides the optimal way to start.
     *
     * @param args from command line, if any
     */
    public static void main(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        var springBoot = config.getProperty("spring.boot.main", "org.platformlambda.rest.RestServer");
        try {
            Class<?> cls = Class.forName(springBoot);
            Method method = cls.getMethod("main", String[].class);
            // Declare as a Spring Boot application so that AppStarter will defer loading main applications
            AppStarter.runAsSpringBootApp();
            // Execute BeforeApplication(s)
            AppStarter.main(args);
            method.invoke(null, (Object) args);
        } catch (ReflectiveOperationException e) {
            AppStarter.main(args);
        }
    }
}
