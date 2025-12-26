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

package org.platformlambda.core.models;

import java.util.List;
import java.util.Map;

/**
 * This interface is reserved for cloud connector.
 * <p>
 * You should not implement this method unless you are writing your own cloud connector.
 */
public interface PubSubProvider {

    boolean createTopic(String topic);

    boolean createTopic(String topic, int partitions);

    void deleteTopic(String topic);

    boolean createQueue(String queue);

    void deleteQueue(String queue);

    void publish(String topic, Map<String, String> headers, Object body);

    void publish(String topic, int partition, Map<String, String> headers, Object body);

    void subscribe(String topic, LambdaFunction listener, String... parameters);

    void subscribe(String topic, int partition, LambdaFunction listener, String... parameters);

    void send(String queue, Map<String, String> headers, Object body);

    void listen(String queue, LambdaFunction listener, String... parameters);

    void unsubscribe(String topic);

    void unsubscribe(String topic, int partition);

    boolean exists(String topic);

    int partitionCount(String topic);

    List<String> list();

    boolean isStreamingPubSub();

    void cleanup();

}
