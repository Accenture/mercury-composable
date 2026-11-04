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

package com.accenture.minigraph.progress;

import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * The relay route of one node in a watched run: the {@code x-return-path} the engine passes to the node's function or
 * synchronous subgraph. It hands each progress event to the run's channel, which frames it for the caller's
 * transport. One instance, so a sender's events keep their order through it, and a drain request is answered only
 * after every event queued before it.
 */
final class ProgressRelay implements TypedLambdaFunction<Object, Object> {
    private final ReturnChannel channel;
    private final String node;

    ProgressRelay(ReturnChannel channel, String node) {
        this.channel = channel;
        this.node = node;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        if (headers.containsKey(ReturnChannel.DRAIN)) {
            // every event queued before this request has been handed to the channel
            return true;
        }
        if (input instanceof Map<?, ?> event) {
            channel.relay(node, event);
        }
        return null;
    }
}
