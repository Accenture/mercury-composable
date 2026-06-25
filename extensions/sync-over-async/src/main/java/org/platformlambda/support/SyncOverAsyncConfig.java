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

package org.platformlambda.support;

/**
 * Tunables for the return-route mechanism. In production these are read from {@code application.properties}
 * (Phase 3); the per-request REST timeout is supplied separately (from the {@code rest.yaml} entry).
 *
 * @param returnChannelPrefix prefix for the per-pod Pub/Sub return channel, e.g. {@code svc-return}
 * @param routeTtlSeconds     TTL for {@code request:{cid}} (should cover REST timeout + buffer)
 * @param responseTtlSeconds  TTL for {@code response:{cid}} (short rendezvous window)
 * @param maxPendingRequests  per-pod ceiling on in-flight synchronous requests
 */
public record SyncOverAsyncConfig(
        String returnChannelPrefix,
        long routeTtlSeconds,
        long responseTtlSeconds,
        int maxPendingRequests) {

    public static SyncOverAsyncConfig defaults() {
        return new SyncOverAsyncConfig("svc-return", 90, 30, 10_000);
    }
}
