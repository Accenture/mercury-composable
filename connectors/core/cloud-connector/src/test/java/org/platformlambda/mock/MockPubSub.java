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

package org.platformlambda.mock;

import org.platformlambda.cloud.EventProducer;
import org.platformlambda.cloud.ServiceLifeCycle;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.models.PubSubProvider;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MockPubSub implements PubSubProvider {
    private static final Logger log = LoggerFactory.getLogger(MockPubSub.class);
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
        return false;
    }

    @Override
    public void deleteQueue(String queue) {
        // no-op
    }

    @Override
    public void publish(String topic, Map<String, String> headers, Object body) {
        if (topic.equals("exception")) {
            throw new IllegalArgumentException("demo");
        }
    }

    @Override
    public void publish(String topic, int partition, Map<String, String> headers, Object body) {
        try {
            String route = topic + "." + partition;
            EventEmitter po = EventEmitter.getInstance();
            Map<String, String> eventHeaders = headers == null ? new HashMap<>() : headers;
            if (eventHeaders.containsKey(EventProducer.EMBED_EVENT) && body instanceof byte[]) {
                EventEnvelope event = new EventEnvelope();
                event.load((byte[]) body);
                String to = event.getTo();
                int sep = to.indexOf("@monitor");
                po.send(sep > 1 ? event.setTo(to.substring(0, sep)) : event);
            } else {
                po.send(new EventEnvelope().setTo(route).setHeaders(headers).setBody(body));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unable to delivery event to {} - {}", topic, e.getMessage());
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
        String route = topic+"."+partition;
        EventEmitter po = EventEmitter.getInstance();
        Platform platform = Platform.getInstance();
        platform.registerPrivate(route, listener, 1);
        subscriptions.put(topic, listener);
        if (parameters.length == 3 && parameters[2].equals("-100")) {
            final ServiceLifeCycle initialLoad = new ServiceLifeCycle(topic, partition, UUID.randomUUID().toString());
            initialLoad.start();
            LambdaFunction f = (headers, input, instance) -> {
                String topicPartition = partition < 0? topic : topic + "." + partition;
                String INIT_HANDLER =  "init." + topicPartition;
                po.send(INIT_HANDLER, "done");
                return true;
            };
            platform.registerPrivate(route+".mock", f, 1);
            po.sendLater(new EventEnvelope().setTo(route+".mock").setBody("done"),
                    new Date(System.currentTimeMillis()+8000));
        }
    }

    @Override
    public void send(String queue, Map<String, String> headers, Object body) {
        // no-op
    }

    @Override
    public void listen(String queue, LambdaFunction listener, String... parameters) {
        // no-op
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
