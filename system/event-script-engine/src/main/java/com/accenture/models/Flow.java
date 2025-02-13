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

import java.util.HashMap;
import java.util.Map;

public class Flow {

    public final Map<String, Task> tasks = new HashMap<>();

    public final String id;
    public final long ttl;

    public final String firstTask;
    public final String externalStateMachine;
    public final String exception;

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param id for the event flow configuration
     * @param firstTask for the flow
     * @param externalStateMachine if any
     * @param duration in milliseconds
     * @param exception handler to catch unhandled exceptions
     */
    public Flow(String id, String firstTask, String externalStateMachine, long duration, String exception) {
        this.id = id;
        this.firstTask = firstTask;
        this.externalStateMachine = externalStateMachine;
        this.exception = exception;
        this.ttl = duration;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param entry of a task
     */
    public void addTask(Task entry) {
        tasks.put(entry.service, entry);
    }

}
