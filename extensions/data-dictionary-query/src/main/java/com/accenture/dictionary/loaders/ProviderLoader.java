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

package com.accenture.dictionary.loaders;

import com.accenture.dictionary.models.DataProvider;
import org.platformlambda.core.util.ConfigReader;

import java.util.ArrayList;
import java.util.List;

public class ProviderLoader {
    private static final ProviderLoader INSTANCE = new ProviderLoader();

    private ProviderLoader() {
        // singleton
    }

    public static ProviderLoader getInstance() {
        return INSTANCE;
    }

    public DataProvider loadProvider(String providerId, ConfigReader config) {
        var protocol = config.getProperty("provider.protocol", "");
        var service = config.getProperty("provider.service", "");
        var url = config.getProperty("provider.url", "");
        var method = config.getProperty("provider.method", "");
        var skills = config.get("provider.skills", new ArrayList<>());
        var headers = config.get("provider.headers", new ArrayList<String>());
        var input = config.get("provider.input", new ArrayList<String>());
        if (!protocol.isEmpty() && !service.isEmpty() && !url.isEmpty() && !method.isEmpty() &&
                input instanceof List<?> inputList && !inputList.isEmpty()) {
            // String id, String protocol, String service, String url, String method
            var provider = new DataProvider(providerId, protocol, service, url, method);
            inputList.forEach(d -> provider.addInput(String.valueOf(d)));
            if (headers instanceof List<?> headersList) {
                for (Object h: headersList) {
                    var header = String.valueOf(h);
                    if (validHeader(header)) {
                        provider.addHeader(header);
                    } else {
                        throw new IllegalArgumentException("Invalid header '"+h+"' in provider "+providerId);
                    }
                }
            }
            if (skills instanceof List<?> skillsList) {
                skillsList.forEach(d -> provider.addSkill(String.valueOf(d)));
            }
            return provider;
        } else {
            throw new IllegalArgumentException("Invalid syntax in "+providerId+
                    " - check provider.id, protocol, service, url, method, skills, headers and input");
        }
    }

    private boolean validHeader(String header) {
        if (header.contains(":")) {
            var key = header.substring(0, header.indexOf(":")).trim();
            var value = header.substring(header.indexOf(":") + 1).trim();
            return !key.isEmpty() && !value.isEmpty();
        } else {
            return false;
        }
    }
}
