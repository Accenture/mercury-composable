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

package org.platformlambda.scheduler.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.scheduler.JobLoader;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.platformlambda.scheduler.services.JobExecutor.ROUTE;

@PreLoad(route=ROUTE)
public class JobExecutor implements TypedLambdaFunction<Map<String, Object>, Void> {
    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);
    public static final String ROUTE = "v1.job.executor";
    private static final String TYPE = "type";
    private static final String SAVE = "save";
    private static final String EXPIRES = "expires";
    private static final String NAME = "name";
    private static final String SERVICE = "service";

    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws ExecutionException, InterruptedException {
        var po = new PostOffice(headers, instance);
        var name = headers.get("job");
        if (name != null) {
            ScheduledJob job = JobLoader.getJob(name);
            if (job != null) {
                if (po.exists(job.service)) {
                    if (readyToExecute(po, job)) {
                        // check if a job has already started by a peer
                        var event = new EventEnvelope().setTo(job.service).setHeader("job", name)
                                .setBody(job.parameters);
                        po.send(event);
                    }
                } else {
                    log.error("Scheduled service {} does not exist", job.service);
                }
            }
        }
        return null;
    }

    private boolean readyToExecute(PostOffice po, ScheduledJob job)
            throws ExecutionException, InterruptedException {
        if (po.exists(job.resolver)) {
            var event = new EventEnvelope().setTo(job.resolver).setHeader(TYPE, EXPIRES).setHeader(NAME, job.name);
            var result = po.request(event, 5000).get();
            boolean expired = Boolean.TRUE == result.getBody();
            if (expired) {
                var persist = new EventEnvelope().setTo(job.resolver).setHeader(TYPE, SAVE)
                                                .setBody(Map.of(NAME, job.name, SERVICE, job.service));
                po.send(persist);
                return true;
            }
        } else {
            log.error("Resolver service {} does not exist", job.resolver);
        }
        return false;
    }
}
