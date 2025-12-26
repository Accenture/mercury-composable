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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleConnection {
    private final Map<String, SimpleRelationship> relationships = new HashMap<>();
    private final String id;
    private final SimpleNode source;
    private final SimpleNode target;

    public SimpleConnection(String id, SimpleNode source, SimpleNode target) {
        this.id = id;
        this.source = source;
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public SimpleNode getSource() {
        return source;
    }

    public SimpleNode getTarget() {
        return target;
    }

    public SimpleRelationship addRelation(String type) {
        var relation = new SimpleRelationship(type, source.getAlias(), target.getAlias());
        relationships.put(type.toLowerCase(), relation);
        return relation;
    }

    public SimpleRelationship getRelation(String type) {
        return relationships.get(type.toLowerCase());
    }

    public List<SimpleRelationship> getRelations() {
        return new ArrayList<>(relationships.values());
    }
}
