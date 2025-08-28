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

import org.platformlambda.core.system.EventEmitter;

public class TaskMetrics {
    private final long begin = System.nanoTime();
    private float elapsed = -1.0f;
    private final String route;

    public TaskMetrics(String service, String functionRoute) {
        this.route = service.equals(functionRoute)? functionRoute : service+"("+functionRoute+")";
    }

    public void complete() {
        float delta = (float) (System.nanoTime() - begin) / EventEmitter.ONE_MILLISECOND;
        // adjust precision to 3 decimal points
        elapsed = Float.parseFloat(String.format("%.3f", Math.max(0.0f, delta)));
    }

    public String getRoute() {
        return route;
    }

    public float getElapsed() {
        return elapsed;
    }
}
