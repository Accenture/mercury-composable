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

package com.accenture.benchmark.reporter;

import java.util.Map;

/**
 * The outcome of one workload run.
 *
 * @param name        short display name (e.g. "Callback (async)")
 * @param category    grouping heading in the report (e.g. normal operation vs overload)
 * @param description one-line explanation of what the workload exercises
 * @param params      display parameters (ops, concurrency, payload, …) shown in the report
 * @param attempted   number of operations attempted
 * @param failures    number that failed (timeout / error) and were excluded from the latency stats
 * @param elapsedSec  wall-clock duration of the timed phase
 * @param stats       latency summary over the successful operations
 */
public record WorkloadResult(String name, String category, String description, Map<String, String> params,
                             long attempted, long failures, double elapsedSec, Stats stats) {

    /** Successful operations per second over the timed phase. */
    public double throughput() {
        long ok = attempted - failures;
        return elapsedSec > 0 ? ok / elapsedSec : 0;
    }
}
