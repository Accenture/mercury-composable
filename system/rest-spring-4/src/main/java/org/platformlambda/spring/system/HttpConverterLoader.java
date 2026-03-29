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

package org.platformlambda.spring.system;

import org.platformlambda.spring.serializers.HttpConverterHtml;
import org.platformlambda.spring.serializers.HttpConverterJson;
import org.platformlambda.spring.serializers.HttpConverterText;
import org.platformlambda.spring.serializers.HttpConverterXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class HttpConverterLoader implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(HttpConverterLoader.class);

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        log.info("Loading HTTP serializers");
        HttpConverterJson json = new HttpConverterJson();
        HttpConverterXml xml = new HttpConverterXml();
        HttpConverterHtml html = new HttpConverterHtml();
        HttpConverterText text = new HttpConverterText();
        builder.withJsonConverter(json).withXmlConverter(xml);
        builder.addCustomConverter(html).addCustomConverter(text);
    }
}
