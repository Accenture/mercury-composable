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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Flows {
    private static final ConcurrentMap<String, Flow> allFlows = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, FlowInstance> flowInstances = new ConcurrentHashMap<>();

    private Flows() {}
    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param id for an event flow configuration
     * @return event flow config
     */
    public static Flow getFlow(String id) {
        return allFlows.get(id);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return all flow configurations
     */
    public static List<String> getAllFlows() {
        return new ArrayList<>(allFlows.keySet());
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param id for an event flow instance
     * @return specific flow instance
     */
    public static FlowInstance getFlowInstance(String id) {
        return id == null? null : flowInstances.get(id);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param id for an event flow instance
     * @return true if exists
     */
    public static boolean flowExists(String id) {
        return allFlows.containsKey(id);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param flow configuration
     */
    public static void addFlow(Flow flow) {
        allFlows.put(flow.id, flow);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param instance of an event flow
     */
    public static void addFlowInstance(FlowInstance instance) {
        flowInstances.put(instance.id, instance);
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param id of an event flow instance
     */
    public static void closeFlowInstance(String id) {
        FlowInstance instance = flowInstances.get(id);
        if (instance != null) {
            flowInstances.remove(id);
        }
    }
}
