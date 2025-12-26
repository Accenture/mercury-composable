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

package org.platformlambda.scheduler;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.scheduler.models.ScheduledJob;
import org.platformlambda.scheduler.services.AssignJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

@MainApplication(sequence = 8)
public class JobLoader implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(JobLoader.class);
    private static final Utility util = Utility.getInstance();
    private static final ConcurrentMap<String, ScheduledJob> scheduledJobs = new ConcurrentHashMap<>();
    private static final String SCHEDULER_SERVICE = "cron.scheduler";
    private static final String JOBS_PREFIX = "jobs[";
    private static final String JOBS = "jobs";
    private static final AtomicBoolean deferredStart = new AtomicBoolean(false);
    private static final AtomicBoolean leaderElection = new AtomicBoolean(false);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    public static ScheduledJob getJob(String id) {
        return id == null? null : scheduledJobs.get(id);
    }

    public static boolean isDeferredStart() {
        return deferredStart.get();
    }

    public static boolean needLeaderElection() {
        return leaderElection.get();
    }

    @Override
    public void start(String[] args) throws SchedulerException {
        // load cron job configuration
        ConfigReader config = getConfig();
        deferredStart.set("true".equalsIgnoreCase(config.getProperty("deferred.start", "false")));
        leaderElection.set("true".equalsIgnoreCase(config.getProperty("leader.election", "false")));
        if (leaderElection.get()) {
            Platform platform = Platform.getInstance();
            // create a no-op service for leader election
            LambdaFunction f = (headers, input, instance) -> true;
            platform.register(SCHEDULER_SERVICE, f, 1);
        }
        Object o = config.get(JOBS);
        if (o instanceof List<?> list) {
            for (int i=0; i < list.size(); i++) {
                loadConfig(config, i);
            }
        }
        startScheduler();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig(ConfigReader config, int i) {
        String name = config.getProperty(JOBS_PREFIX +i+"].name");
        String service = config.getProperty(JOBS_PREFIX +i+"].service");
        String schedule = config.getProperty(JOBS_PREFIX +i+"].cron");
        String resolver = config.getProperty(JOBS_PREFIX +i+"].resolver");
        Object parameters = config.get(JOBS_PREFIX +i+"].parameters");
        if (schedule != null && resolver != null && service != null &&
                validJobName(name) && (service.startsWith("flow://") || util.validServiceName(service))) {
            ScheduledJob j = new ScheduledJob(name, service, resolver, schedule);
            if (parameters instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) parameters;
                for (var entry: map.entrySet()) {
                    j.addParameter(entry.getKey(), entry.getValue());
                }
            }
            scheduledJobs.put(name, j);
        } else {
            log.error("Invalid job entry#{} - check name, schedule and service", i + 1);
        }
    }

    private void startScheduler() throws SchedulerException {
        // Schedule jobs now
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        // load jobs
        for (var entry: scheduledJobs.entrySet()) {
            String id = entry.getKey();
            ScheduledJob j = entry.getValue();
            JobDetail job = JobBuilder.newJob(AssignJob.class).storeDurably(true)
                    .withIdentity(id, JOBS).build();
            scheduler.addJob(job, true);
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withSchedule(CronScheduleBuilder.cronSchedule(j.cronSchedule))
                    .forJob(id, JOBS).build();
            scheduler.scheduleJob(trigger);
            log.info("Scheduled job: {}, service: {}, statement: {}", id, j.service, j.cronSchedule);
        }
        scheduler.start();
        // shutdown schedule when app stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                log.error("Error while stopping scheduler - {}", e.getMessage());
            }
        }));
    }

    private ConfigReader getConfig() {
        AppConfigReader reader = AppConfigReader.getInstance();
        List<String> paths = Utility.getInstance().split(reader.getProperty("yaml.cron",
                "file:/tmp/config/cron.yaml, classpath:/cron.yaml"), ", ");
        for (String p: paths) {            
            try {
                ConfigReader config = new ConfigReader(p);
                log.info("Loaded config from {}", p);
                return config;
            } catch (IllegalArgumentException e) {
                log.warn("Skipping {} - {}", p, e.getMessage());
            }
        }
        throw new IllegalArgumentException("Scheduler configuration not found in "+paths);
    }

    private boolean validJobName(String str) {
        if (str == null || str.isEmpty()) return false;
        if (str.startsWith(".") || str.startsWith("_") || str.startsWith("-")
                || str.contains("..")
                || str.endsWith(".") || str.endsWith("_") || str.endsWith("-")) return false;
        for (int i=0; i < str.length(); i++) {
            if (!((str.charAt(i) >= '0' && str.charAt(i) <= '9') ||
                    (str.charAt(i) >= 'a' && str.charAt(i) <= 'z') ||
                    (str.charAt(i) >= 'A' && str.charAt(i) <= 'Z') ||
                    (str.charAt(i) == '.' || str.charAt(i) == '_' || str.charAt(i) == '-'))) {
                return false;
            }
        }
        return true;
    }
}
