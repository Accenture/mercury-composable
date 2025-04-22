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

package org.platformlambda.quartz.rest;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.quartz.MainScheduler;
import org.platformlambda.quartz.models.ScheduledJob;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;

@RestController
public class AdminEndpoint {
    private static final String JOB_ID = MainScheduler.JOB_ID;
    private static final String SCHEDULER_SERVICE = MainScheduler.SCHEDULER_SERVICE;
    private static final String TYPE = MainScheduler.TYPE;
    private static final String START = MainScheduler.START_COMMAND;
    private static final String STOP = MainScheduler.STOP_COMMAND;
    private static final String ORIGIN = MainScheduler.ORIGIN;

    @GetMapping(value = "/api/jobs", produces = {"application/json"})
    public Mono<ResponseEntity<Map<String, Object>>> jobListing() {
        return Mono.create(callback -> {
            Map<String, Object> result = new HashMap<>();
            Map<String, Map<String, Object>> list = new HashMap<>();
            List<String> all = MainScheduler.getJobs();
            int n = 0;
            for (String id : all) {
                ScheduledJob job = MainScheduler.getJob(id);
                if (job != null) {
                    list.put(id, job.toMap());
                    n++;
                }
            }
            List<Map<String, Object>> jobList = new ArrayList<>();
            List<String> names = new ArrayList<>(list.keySet());
            if (names.size() > 1) {
                Collections.sort(names);
            }
            for (String id : names) {
                jobList.add(list.get(id));
            }
            result.put("jobs", jobList);
            result.put("total", n);
            result.put("time", new Date());
            callback.success(ResponseEntity.status(200).body(result));
        });
    }

    @PutMapping(value = "/api/jobs/{name}", produces = {"application/json"})
    public Mono<ResponseEntity<Map<String, Object>>> startJob(@PathVariable("name") String name) {
        return Mono.create(callback -> {
            ScheduledJob job = MainScheduler.getJob(name);
            if (job == null) {
                callback.error(new AppException(404, "Job "+name+" not found"));
            } else {
                if (job.startTime != null) {
                    callback.error(new AppException(400, "Job " + name + " already started"));
                } else {
                    try {
                        EventEmitter.getInstance().broadcast(SCHEDULER_SERVICE,
                                new Kv(ORIGIN, Platform.getInstance().getOrigin()),
                                new Kv(TYPE, START), new Kv(JOB_ID, name));
                        try {
                            MainScheduler.startJob(job.name);
                            Map<String, Object> result = new HashMap<>();
                            result.put("type", "start");
                            result.put("message", "Job "+name+" started");
                            result.put("time", new Date());
                            callback.success(ResponseEntity.status(200).body(result));
                        } catch (SchedulerException e) {
                            callback.error(new AppException(400, e.getMessage()));
                        }
                    } catch (IOException e) {
                        callback.error(new AppException(400, e.getMessage()));
                    }
                }
            }
        });
    }

    @DeleteMapping(value = "/api/jobs/{name}", produces = {"application/json"})
    public Mono<ResponseEntity<Map<String, Object>>> stopJob(@PathVariable("name") String name) {
        return Mono.create(callback -> {
            ScheduledJob job = MainScheduler.getJob(name);
            if (job == null) {
                callback.error(new AppException(404, "Job " + name + " not found"));
            } else {
                if (job.startTime != null) {
                    callback.error(new AppException(400, "Job " + name + " already started"));
                } else {
                    try {
                        EventEmitter.getInstance().broadcast(SCHEDULER_SERVICE,
                                new Kv(ORIGIN, Platform.getInstance().getOrigin()),
                                new Kv(TYPE, STOP), new Kv(JOB_ID, name));
                        try {
                            MainScheduler.stopJob(job.name);
                            Map<String, Object> result = new HashMap<>();
                            result.put("type", "stop");
                            result.put("message", "Job " + name + " stopped");
                            result.put("time", new Date());
                            callback.success(ResponseEntity.status(200).body(result));
                        } catch (SchedulerException e) {
                            callback.error(new AppException(400, e.getMessage()));
                        }
                    } catch (IOException e) {
                        callback.error(new AppException(400, e.getMessage()));
                    }
                }
            }
        });
    }

    @PostMapping(value = "/api/jobs/{name}", produces = {"application/json"})
    public Object executeJob(@PathVariable("name") String name) {
        return Mono.create(callback -> {
            ScheduledJob job = MainScheduler.getJob(name);
            if (job == null) {
                callback.error(new AppException(404, "Job " + name + " not found"));
            } else {
                MainScheduler.executeJobNow(job.name);
                Map<String, Object> result = new HashMap<>();
                result.put("type", "execute");
                result.put("message", "Job " + name + " executed");
                result.put("time", new Date());
                callback.success(ResponseEntity.status(200).body(result));
            }
        });
    }

}
