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

import java.util.concurrent.atomic.AtomicInteger;

public class PipelineInfo extends PipeInfo {
    private final Task task;
    public final AtomicInteger ptr = new AtomicInteger(-1);
    private boolean completed = false;

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param task for a pipeline
     */
    public PipelineInfo(Task task) {
        super("pipeline");
        this.task = task;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return pointer to the next step
     */
    public int nextStep() {
        int n = ptr.get();
        if (n < task.pipelineSteps.size()) {
            return ptr.incrementAndGet();
        } else {
            return task.pipelineSteps.size() - 1;
        }
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     * <p>
     * Exit task is the one to be executed after completion of a pipeline
     * @return route name of the exit task
     */
    public String getExitTask() {
        return task.nextSteps.getFirst();
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     * <p>
     * Get route name of the task associated with a specific pipeline step
     * @param n is the index pointer
     * @return route name
     */
    public String getTaskName(int n) {
        return task.pipelineSteps.get(Math.min(n, task.pipelineSteps.size()-1));
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     * <p>
     * Indicates that this the last step of the pipeline
     * @param n is the index pointer
     * @return true or false
     */
    public boolean isLastStep(int n) {
        return n >= task.pipelineSteps.size() - 1;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     */
    public void resetPointer() {
        ptr.set(0);
        completed = false;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     */
    public void setCompleted() {
        this.completed = true;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return true if there is one and only one task
     */
    public boolean isSingleton() {
        return task.pipelineSteps.size() == 1;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return completion status
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return task for a pipeline
     */
    public Task getTask() {
        return task;
    }

}
