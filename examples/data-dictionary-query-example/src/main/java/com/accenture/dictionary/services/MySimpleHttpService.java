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

package com.accenture.dictionary.services;

import com.accenture.dictionary.base.DictionaryLambdaFunction;
import com.accenture.dictionary.models.DataProvider;
import com.accenture.dictionary.models.HostAndUri;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@PreLoad(route = "v1.simple.http.service", instances=200)
public class MySimpleHttpService extends DictionaryLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(MySimpleHttpService.class);
    private static final Utility util = Utility.getInstance();
    private static final AtomicLong skillWarning = new AtomicLong(0);
    private static final long WARNING_INTERVAL = 5000;

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws ExecutionException, InterruptedException, URISyntaxException {
        var timeout = Math.max(1, util.str2long(headers.getOrDefault(TIMEOUT, "30000")));
        if (!headers.containsKey(PROVIDER)) {
            throw new IllegalArgumentException("Missing provider header");
        }
        var provider = getDataProvider(headers.get(PROVIDER));
        if (provider == null) {
            throw new IllegalArgumentException("Provider '"+headers.get(PROVIDER)+"' not found");
        }
        var po = new PostOffice(headers, instance);
        var target = new HostAndUri(provider.url);
        var request = new AsyncHttpRequest();
        request.setMethod(provider.method).setTargetHost(target.host).setUrl(target.uri);
        mapHttpHeaders(request, provider.headers);
        mapHttpInput(request, input, provider);
        // As a simple HTTP client, this does not have implementation for any authentication skills.
        if (!provider.skills.isEmpty()) {
            lackOfSkillAdvice(po.getRoute(), provider);
        }
        log.info("{} {} with {}, ttl={}", request.getMethod(), request.getUrl(), input, timeout);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap());
        var result = po.request(event, timeout, false).get();
        return new EventEnvelope().setStatus(result.getStatus()).setBody(result.getBody());
    }

    private void lackOfSkillAdvice(String myRoute, DataProvider provider) {
        // just print out an advice once a while
        var now = System.currentTimeMillis();
        if (now - skillWarning.get() > WARNING_INTERVAL) {
            skillWarning.set(now);
            log.warn("{} does not implement {} - perhaps you need another one with the right skills",
                    myRoute, provider.skills);
        }
    }
}
