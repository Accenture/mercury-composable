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

package org.platformlambda.core.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockPubSub implements PubSubProvider {

    private static final Map<String, Integer> topicStore = new HashMap<>();
    private static final Map<String, LambdaFunction> subscriptions = new HashMap<>();

    @Override
    public boolean createTopic(String topic) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        topicStore.put(topic, 1);
        return true;
    }

    @Override
    public boolean createTopic(String topic, int partitions) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        topicStore.put(topic, partitions);
        return true;
    }

    @Override
    public void deleteTopic(String topic) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        topicStore.remove(topic);
    }

    @Override
    public boolean createQueue(String queue) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void deleteQueue(String queue) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void publish(String topic, Map<String, String> headers, Object body) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
    }

    @Override
    public void publish(String topic, int partition, Map<String, String> headers, Object body) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
    }

    @Override
    public void subscribe(String topic, LambdaFunction listener, String... parameters) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        subscriptions.put(topic, listener);
    }

    @Override
    public void subscribe(String topic, int partition, LambdaFunction listener, String... parameters) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        subscriptions.put(topic, listener);
    }

    @Override
    public void send(String queue, Map<String, String> headers, Object body) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void listen(String queue, LambdaFunction listener, String... parameters) {
        throw new IllegalArgumentException("Not implemented");
    }

    @Override
    public void unsubscribe(String topic) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        subscriptions.remove(topic);
    }

    @Override
    public void unsubscribe(String topic, int partition) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        subscriptions.remove(topic);
    }

    @Override
    public boolean exists(String topic) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        return topicStore.containsKey(topic);
    }

    @Override
    public int partitionCount(String topic) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
        return topicStore.getOrDefault(topic, -1);
    }

    @Override
    public List<String> list() {
        return new ArrayList<>(topicStore.keySet());
    }

    @Override
    public boolean isStreamingPubSub() {
        return true;
    }

    @Override
    public void cleanup() {
        // no-op
    }
}
