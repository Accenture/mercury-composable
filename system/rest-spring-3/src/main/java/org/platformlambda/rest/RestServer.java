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

package org.platformlambda.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

/**
 * If you create your own spring boot start up application, please keep the ComponentScan configuration unchanged
 * because the AppLoader is packaged under the org.platformlambda package path.
 * <p>
 * The AppLoader will invoke user written MainApplication classes accordingly.
 */
@ServletComponentScan({"org.platformlambda"})
@ComponentScan({"org.platformlambda", "${web.component.scan}"})
@ImportAutoConfiguration
@SpringBootApplication
public class RestServer extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(RestServer.class, args);
    }
}
