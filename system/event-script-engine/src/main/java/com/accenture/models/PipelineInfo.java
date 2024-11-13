/*

    Copyright 2018-2024 Accenture Technology

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

    public PipelineInfo(Task task) {
        super("pipeline");
        this.task = task;
    }

    public int nextStep() {
        int n = ptr.get();
        if (n < task.pipelineSteps.size()) {
            return ptr.incrementAndGet();
        } else {
            return task.pipelineSteps.size() - 1;
        }
    }

    /**
     * Exit task is the one to be executed after completion of a pipeline
     * @return route name of the exit task
     */
    public String getExitTask() {
        return task.nextSteps.getFirst();
    }

    /**
     * Get route name of the task associated with a specific pipeline step
     * @param n is the index pointer
     * @return route name
     */
    public String getTaskName(int n) {
        return task.pipelineSteps.get(n);
    }

    /**
     * Indicates that this the last step of the pipeline
     * @param n is the index pointer
     * @return true or false
     */
    public boolean isLastStep(int n) {
        return n >= task.pipelineSteps.size() - 1;
    }

    public void resetPointer() {
        ptr.set(0);
        completed = false;
    }

    public void restorePrev(int prevPointer, boolean completed) {
        this.ptr.set(prevPointer);
        this.completed = completed;
    }

    public void setCompleted() {
        this.completed = true;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Task getTask() {
        return task;
    }

}
