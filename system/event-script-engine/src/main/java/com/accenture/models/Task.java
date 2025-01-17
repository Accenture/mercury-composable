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
    // execution: decision, response, end, sequential, parallel
    public final String execution;
    private long delay = -1;
    private String functionRoute;
    private String delayVar = null;
    private String joinTask = null;
    private String exceptionTask = null;
    private String loopType = "none";
    private String whileModelKey = null;

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param service name for a task
     * @param functionRoute composable function route for the task
     * @param execution type
     */
    public Task(String service, String functionRoute, String execution) {
        this.service = service;
        this.functionRoute = functionRoute == null? service : functionRoute;
        this.execution = execution;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return function route
     */
    public String getFunctionRoute() {
        return this.functionRoute;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     * <p>
     * This function is used by the EventScriptMock class
     *
     * @param functionRoute to reassign
     */
    public void reAssign(String functionRoute) {
        this.functionRoute = functionRoute;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param task for joining fork tasks
     */
    public void setJoinTask(String task) {
        this.joinTask = task;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return the join task
     */
    public String getJoinTask() {
        return joinTask;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param task for the exception handler
     */
    public void setExceptionTask(String task) {
        this.exceptionTask = task;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return route name of the exception handler task
     */
    public String getExceptionTask() {
        return exceptionTask;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return text representation of a task
     */
    public String toString() {
        return SimpleMapper.getInstance().getMapper().writeValueAsString(this);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return task completion delay in milliseconds
     */
    public long getDelay() {
        return delay;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param delay at the end of a task
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return delay variable in the state machine
     */
    public String getDelayVar() {
        return delayVar;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param delayVar delay variable in the state machine
     */
    public void setDelayVar(String delayVar) {
        this.delayVar = delayVar;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return loop type
     */
    public String getLoopType() {
        return loopType;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param loopType for the pipeline task
     */
    public void setLoopType(String loopType) {
        this.loopType = loopType;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return model variable that controls a while loop
     */
    public String getWhileModelKey() {
        return whileModelKey;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param whileModelKey model variable that controls a while loop
     */
    public void setWhileModelKey(String whileModelKey) {
        this.whileModelKey = whileModelKey;
    }
}
