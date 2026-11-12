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

package org.platformlambda.scheduler.services;

import com.accenture.adapters.FlowExecutor;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.ActiveEnvironment;
import org.platformlambda.scheduler.JobLoader;
import org.platformlambda.scheduler.RetryRequest;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.platformlambda.scheduler.services.JobExecutor.JOB_EXECUTOR;

@PreLoad(route= JOB_EXECUTOR)
public class JobExecutor implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);
    public static final String JOB_EXECUTOR = "run.scheduled.job";
    private static final String SCHEDULER_SERVICE = "cron.scheduler";
    private static final String JOB = "job";
    private static final String TYPE = "type";
    private static final String START = "start";
    private static final String END = "end";
    private static final String EXPIRES = "expires";
    private static final String OPERATOR = "operator";
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
            jobCompleted(po, callbackId);
        } else {
            var job = JobLoader.getJob(headers.get(JOB));
            if (job != null) {
                jobTriggered(po, job, headers.get(OPERATOR));
            }
        }
        return null;
    }

    /**
     * A job's service replied: the job name rides the correlation id after the run's id and an '@'. Tell the
     * resolver the job ended.
     */
    private void jobCompleted(PostOffice po, String callbackId) {
        var restored = callbackId.substring(callbackId.indexOf('@') + 1);
        ScheduledJob job = JobLoader.getJob(restored);
        if (job != null) {
            po.send(new EventEnvelope().setTo(job.resolver).setHeader(TYPE, END).setHeader(NAME, job.name));
        }
    }

    /**
     * The scheduled point (RFC-0008): an operator's run is explicit and honored on a standby instance too.
     * A Quartz trigger runs the job's normal path on an active instance and is skipped with one line otherwise.
     */
    private void jobTriggered(PostOffice po, ScheduledJob job, String operator)
            throws ExecutionException, InterruptedException {
        var env = ActiveEnvironment.getInstance();
        var active = env.isActiveNow();
        if (operator != null) {
            if (active) {
                log.info("Operator {} runs {} manually", operator, job.name);
            } else {
                log.info("Operator {} runs {} manually on a standby instance - the active environment is {}, " +
                        "this instance is {}", operator, job.name, env.getActive(), env.getEnvironment());
            }
            executeJob(po, job);
        } else if (active) {
            prepareJob(po, job);
        } else {
            log.info("Standby: job {} skipped - the active environment is {}, this instance is {}",
                    job.name, env.getActive(), env.getEnvironment());
        }
    }

    private void prepareJob(PostOffice po, ScheduledJob job)
            throws ExecutionException, InterruptedException {
        // check if a job has already started by a peer
        if (JobLoader.needLeaderElection()) {
            String me = Platform.getInstance().getOrigin();
            EventEmitter.getInstance().search(SCHEDULER_SERVICE, true)
                    .onSuccess(peers -> {
                        final String selected;
                        if (peers.size() > 1) {
                            peers.sort(Comparator.naturalOrder());
                            selected = peers.getFirst();
                        } else  {
                            selected = String.valueOf(me);
                        }
                        if (me.equals(selected)) {
                            log.info("Ready to run {}", job.name);
                            executeJob(po, job);
                        } else {
                            log.info("Skip {} because peer {} is handling it", job.name, selected);
                        }
                    });
        } else if (readyToExecute(po, job)) {
            executeJob(po, job);
        }
    }

    private void executeJob(PostOffice po, ScheduledJob job) {
        po.send(new EventEnvelope().setTo(job.resolver).setBody(resolverParameters(job))
                .setHeader(SCHEDULE, job.schedule)
                .setHeader(TYPE, START).setHeader(NAME, job.name).setHeader(SERVICE, job.service));
        // encode the job name in correlationId
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

    /**
     * Ask the state resolver whether the job's previous run has expired, which a resolver for more than one instance
     * answers by claiming the job. A timed-out question is asked again ({@link RetryRequest}) with the same origin id,
     * so the resolver can tell this instance's claim from a peer's. No answer, an error or "not expired" skips the job
     * for this firing.
     */
    private boolean readyToExecute(PostOffice po, ScheduledJob job)
            throws ExecutionException, InterruptedException {
        if (!po.exists(job.resolver)) {
            log.error("Resolver service {} does not exist", job.resolver);
            return false;
        }
        var reply = RetryRequest.send(po, () -> new EventEnvelope().setTo(job.resolver).setHeader(TYPE, EXPIRES)
                .setHeader(NAME, job.name).setBody(resolverParameters(job)),
                "State resolver " + job.resolver + " for job " + job.name);
        var result = reply.event();
        if (reply.timedOut()) {
            log.warn("Job {} skipped for this firing - state resolver {} did not answer", job.name, job.resolver);
            return false;
        }
        if (result.getStatus() != 200) {
            log.warn("Job {} skipped - state resolver {} answered status {}, {}", job.name, job.resolver,
                    result.getStatus(), result.getError());
            return false;
        }
        var ready = Boolean.TRUE.equals(result.getBody());
        if (!ready && reply.attempts() > 1) {
            log.warn("Job {} skipped - state resolver {} answered not expired after an attempt had timed out: " +
                    "a peer holds the claim, or the resolver did not recognise this instance's own claim, which it " +
                    "should answer true again for the same {} ({})",
                    job.name, job.resolver, ScheduledJob.ORIGIN, Platform.getInstance().getOrigin());
        }
        return ready;
    }

    /** The job's parameters as the state resolver receives them, with this instance's origin id under _origin. */
    private static Map<String, Object> resolverParameters(ScheduledJob job) {
        var parameters = new HashMap<>(job.parameters);
        parameters.put(ScheduledJob.ORIGIN, Platform.getInstance().getOrigin());
        return parameters;
    }
}
