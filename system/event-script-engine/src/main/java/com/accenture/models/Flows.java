/*

    Copyright 2018-2024 Accenture Technology

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
    private Flows() {}
    private static final ConcurrentMap<String, Flow> allFlows = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, FlowInstance> flowInstances = new ConcurrentHashMap<>();

    public static Flow getFlow(String id) {
        return allFlows.get(id);
    }

    public static List<String> getAllFlows() {
        return new ArrayList<>(allFlows.keySet());
    }

    public static FlowInstance getFlowInstance(String id) {
        return flowInstances.get(id);
    }

    public static boolean flowExists(String id) {
        return allFlows.containsKey(id);
    }

    public static void addFlow(Flow flow) {
        allFlows.put(flow.id, flow);
    }

    public static void addFlowInstance(FlowInstance instance) {
        flowInstances.put(instance.id, instance);
    }

    public static boolean hasFlowInstance(String id) {
        return flowInstances.containsKey(id);
    }

    public static void closeFlowInstance(String id) {
        FlowInstance instance = flowInstances.get(id);
        if (instance != null) {
            flowInstances.remove(id);
        }
    }

}
