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

package com.accenture.mock;

import com.accenture.models.Flows;
import com.accenture.models.Flow;
import com.accenture.models.Task;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventScriptMock {
    private static final Logger log = LoggerFactory.getLogger(EventScriptMock.class);
    private final Flow flow;

    /**
     * This is a mock helper class to reroute a task to a unit test function
     *
     * @param flowId for an event flow template
     */
    public EventScriptMock(String flowId) {
        this.flow = flowId != null && !flowId.isEmpty()? Flows.getFlow(flowId) : null;
        if (this.flow == null) {
            throw new IllegalArgumentException("Flow "+flowId+" does not exist");
        }
    }

    /**
     * Retrieve the current function route name of a task
     *
     * @param taskName for a task
     * @return route name
     */
    public String getFunctionRoute(String taskName) {
        Task task = taskName != null && !taskName.isEmpty()? flow.tasks.get(taskName) : null;
        if (task == null) {
            throw new IllegalArgumentException("Task "+taskName+" does not exist");
        }
        return task.getFunctionRoute();
    }

    /**
     * Override the function route name of a task
     *
     * @param taskName for a task
     * @param mockFunction route name to override a function route
     * @return this
     */
    public EventScriptMock assignFunctionRoute(String taskName, String mockFunction) {
        Task task = taskName != null && !taskName.isEmpty()? flow.tasks.get(taskName) : null;
        if (task == null) {
            throw new IllegalArgumentException("Task "+taskName+" does not exist");
        }
        if (mockFunction == null || mockFunction.isEmpty()) {
            throw new IllegalArgumentException("Mock function route cannot be empty");
        }
        String previous = task.getFunctionRoute();
        task.reAssign(mockFunction);
        log.info("Reassigned '{}' to task({}) of flow({}), previous function '{}'",
                mockFunction, taskName, flow.id, previous);
        return this;
    }

    /**
     * Set a monitor function to capture state machine and input data mapping
     *
     * @param taskName for a task
     * @param monitor function route name
     * @return this
     */
    public EventScriptMock setMonitorBeforeTask(String taskName, String monitor) {
        setMonitorForTask(taskName, monitor, true);
        return this;
    }

    /**
     * Set a monitor function to capture state machine and output data mapping
     *
     * @param taskName for a task
     * @param monitor function route name
     * @return this
     */
    public EventScriptMock setMonitorAfterTask(String taskName, String monitor) {
        setMonitorForTask(taskName, monitor, false);
        return this;
    }

    /**
     * Clear monitors for the given task
     *
     * @param taskName for a task
     */
    public void clearMonitors(String taskName) {
        Task task = taskName != null && !taskName.isEmpty()? flow.tasks.get(taskName) : null;
        if (task == null) {
            throw new IllegalArgumentException("Task "+taskName+" does not exist");
        }
        var mBefore = task.getMonitorBeforeTask();
        var mAfter = task.getMonitorAfterTask();
        if (mBefore != null) {
            log.info("Release '{}' from input data mapping of task({}) of flow({})", mBefore, taskName, flow.id);
        }
        if (mAfter != null) {
            log.info("Release '{}' from output data mapping of task({}) of flow({})", mAfter, taskName, flow.id);
        }
        task.clearMonitors();
    }

    private void setMonitorForTask(String taskName, String monitor, boolean before) {
        Task task = taskName != null && !taskName.isEmpty()? flow.tasks.get(taskName) : null;
        if (task == null) {
            throw new IllegalArgumentException("Task "+taskName+" does not exist");
        }
        if (monitor == null || monitor.isEmpty()) {
            throw new IllegalArgumentException("Monitor function route cannot be empty");
        }
        if (!Utility.getInstance().validServiceName(monitor)) {
            throw new IllegalArgumentException("Invalid monitor function route name - e.g. my.monitor.function");
        }
        if (before) {
            task.setMonitorBeforeTask(monitor);
            log.info("Using '{}' to monitor input data mapping of task({}) of flow({})", monitor, taskName, flow.id);
        } else {
            task.setMonitorAfterTask(monitor);
            log.info("Using '{}' to monitor output data mapping of task({}) of flow({})", monitor, taskName, flow.id);
        }
    }
}
