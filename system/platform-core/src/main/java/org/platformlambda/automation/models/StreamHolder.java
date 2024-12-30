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

package org.platformlambda.automation.models;

import org.platformlambda.core.system.EventPublisher;

public class StreamHolder {
    private final EventPublisher publisher;

    public StreamHolder(int timeoutSeconds) {
        this.publisher = new EventPublisher(timeoutSeconds * 1000L);
    }

    public EventPublisher getPublisher() {
        return publisher;
    }

    public String getInputStreamId() {
        return publisher.getStreamId();
    }

    public void close() {
        publisher.publishCompletion();
    }
}
