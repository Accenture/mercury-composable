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

package com.accenture.models;

import org.platformlambda.core.serializers.SimpleMapper;

import java.util.ArrayList;
import java.util.List;

public class Task {
    public final List<String> init = new ArrayList<>();
    public final List<String> comparator = new ArrayList<>();
    public final List<String> sequencer = new ArrayList<>();
    public final List<List<String>> conditions = new ArrayList<>();
    public final List<String> input = new ArrayList<>();
    public final List<String> output = new ArrayList<>();
    public final List<String> nextSteps = new ArrayList<>();
    public final List<String> pipelineSteps = new ArrayList<>();
    public final String service;
    public final String functionRoute;

    // execution: decision, response, end, sequential, parallel
    public final String execution;
    private long delay = -1;
    private String delayVar = null;
    private String joinTask = null;
    private String exceptionTask = null;
    private String loopType = "none";
    private String whileModelKey = null;

    public Task(String service, String functionRoute, String execution) {
        this.service = service;
        this.functionRoute = functionRoute == null? service : functionRoute;
        this.execution = execution;
    }

    public void setJoinTask(String task) {
        this.joinTask = task;
    }

    public String getJoinTask() {
        return joinTask;
    }

    public void setExceptionTask(String task) {
        this.exceptionTask = task;
    }

    public String getExceptionTask() {
        return exceptionTask;
    }

    public String toString() {
        return SimpleMapper.getInstance().getMapper().writeValueAsString(this);
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public String getDelayVar() {
        return delayVar;
    }

    public void setDelayVar(String delayVar) {
        this.delayVar = delayVar;
    }

    public String getLoopType() {
        return loopType;
    }

    public void setLoopType(String loopType) {
        this.loopType = loopType;
    }

    public String getWhileModelKey() {
        return whileModelKey;
    }

    public void setWhileModelKey(String whileModelKey) {
        this.whileModelKey = whileModelKey;
    }
}
