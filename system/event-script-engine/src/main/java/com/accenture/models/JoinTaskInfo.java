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

package com.accenture.models;

import java.util.concurrent.atomic.AtomicInteger;

public class JoinTaskInfo extends PipeInfo {
    public final int forks;
    public final String joinTask;
    public final AtomicInteger resultCount = new AtomicInteger(0);

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param forks number of tasks
     * @param joinTask to be executed
     */
    public JoinTaskInfo(int forks, String joinTask) {
        super("join");
        this.forks = forks;
        this.joinTask = joinTask;
    }

}
