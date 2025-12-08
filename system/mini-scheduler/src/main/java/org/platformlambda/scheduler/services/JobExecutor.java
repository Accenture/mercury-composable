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

import com.accenture.adapters.FlowExecutor;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.JobLoader;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.platformlambda.scheduler.services.JobExecutor.JOB_EXECUTOR;

@PreLoad(route= JOB_EXECUTOR)
public class JobExecutor implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);
    public static final String JOB_EXECUTOR = "v1.job.executor";
    private static final String JOB = "job";
    private static final String TYPE = "type";
    private static final String START = "start";
    private static final String END = "end";
    private static final String EXPIRES = "expires";
    private static final String NAME = "name";
    private static final String SERVICE = "service";
    private static final String SCHEDULE = "schedule";
    private static final String FLOW_PROTOCOL = "flow://";
    private static final String BODY = "body";
    private static final String HEADER = "header";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance)
            throws ExecutionException, InterruptedException {
        var po = new PostOffice(headers, instance);
        var callbackId = input.getCorrelationId();
        if (callbackId != null && callbackId.contains("@")) {
            var sep = callbackId.indexOf('@');
            var restored = callbackId.substring(sep+1);
            ScheduledJob job = JobLoader.getJob(restored);
            if (job != null) {
                po.send(new EventEnvelope().setTo(job.resolver).setHeader(TYPE, END).setHeader(NAME, job.name));
            }
        } else {
            var name = headers.get(JOB);
            if (name != null) {
                ScheduledJob job = JobLoader.getJob(name);
                if (job != null) {
                    executeJob(po, job);
                }
            }
        }
        return null;
    }

    private void executeJob(PostOffice po, ScheduledJob job) throws ExecutionException, InterruptedException {
        // check if a job has already started by a peer
        if (readyToExecute(po, job)) {
            // encode job name in correlationId
            var cid = Utility.getInstance().getUuid() + "@" + job.name;
            if (job.service.startsWith(FLOW_PROTOCOL)) {
                var flowId = job.service.substring(FLOW_PROTOCOL.length());
                var flow = Flows.getFlow(flowId);
                if (flow == null) {
                    log.error("Scheduled flow {} does not exist", job.service);
                } else {
                    // map the input.body and input.header to a flow
                    var dataset = new HashMap<String, Object>();
                    dataset.put(BODY, job.parameters);
                    dataset.put(HEADER, Map.of(JOB, job.name));
                    FlowExecutor.getInstance().launch(po, flowId, dataset, JOB_EXECUTOR, cid);
                }
            } else {
                if (po.exists(job.service)) {
                    po.send(new EventEnvelope().setTo(job.service).setHeader(JOB, job.name)
                            .setBody(job.parameters).setCorrelationId(cid).setReplyTo(JOB_EXECUTOR));
                } else {
                    log.error("Scheduled service {} does not exist", job.service);
                }
            }
        }
    }

    private boolean readyToExecute(PostOffice po, ScheduledJob job)
            throws ExecutionException, InterruptedException {
        if (po.exists(job.resolver)) {
            var event = new EventEnvelope().setTo(job.resolver).setHeader(TYPE, EXPIRES).setHeader(NAME, job.name);
            var result = po.request(event, 5000).get();
            boolean expired = Boolean.TRUE == result.getBody();
            if (expired) {
                po.send(new EventEnvelope().setTo(job.resolver).setBody(job.parameters)
                        .setHeader(SCHEDULE, job.cronSchedule)
                        .setHeader(TYPE, START).setHeader(NAME, job.name).setHeader(SERVICE, job.service));
                return true;
            }
        } else {
            log.error("Resolver service {} does not exist", job.resolver);
        }
        return false;
    }
}
