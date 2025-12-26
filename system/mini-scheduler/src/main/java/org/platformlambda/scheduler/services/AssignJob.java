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

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.JobLoader;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class AssignJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(AssignJob.class);
    private static final CryptoApi crypto = new CryptoApi();
    private static final String JOBS = "jobs.";
    private static final String JOB = "job";

    @Override
    public void execute(JobExecutionContext context) {
        var key = String.valueOf(context.getJobDetail().getKey());
        if (key.startsWith(JOBS)) {
            var name = key.substring(JOBS.length());
            var uuid = Utility.getInstance().getUuid();
            var po = new PostOffice("job.scheduler", uuid, "JOB "+name);
            var event = new EventEnvelope().setTo(JobExecutor.JOB_EXECUTOR).setHeader(JOB, name);
            if (JobLoader.isDeferredStart()) {
                po.sendLater(event, new Date(System.currentTimeMillis() + getRandomizedMillis(name)));
            } else {
                po.send(event);
            }
        }
    }

    private long getRandomizedMillis(String name) {
        // get an integer between 0 and 9
        var seconds = crypto.nextInt(0, 1000) % 10;
        log.info("Defer execution of {} for {} seconds", name, seconds);
        return seconds * 1000;
    }
}
