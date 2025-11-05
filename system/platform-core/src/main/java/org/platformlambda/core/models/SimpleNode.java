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

import java.util.HashSet;
import java.util.Set;

public class SimpleNode extends GraphProperties {
    private final Set<String> types = new HashSet<>();
    private final String id;
    private final String alias;

    public SimpleNode(String id, String alias, String type) {
        validateName(alias);
        validateName(type);
        this.id = id;
        this.alias = alias;
        addType(type);
    }

    public String getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public Set<String> getTypes() {
        return types;
    }

    public void addType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type cannot be empty");
        }
        validateName(type);
        types.add(type);
    }

    public void removeType(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("type cannot be empty");
        }
        types.remove(type);
    }
}
