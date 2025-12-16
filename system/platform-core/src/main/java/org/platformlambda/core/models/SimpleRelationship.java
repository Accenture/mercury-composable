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

public class SimpleRelationship extends GraphProperties {
    private final String source;
    private final String target;
    private final String type;

    public SimpleRelationship(String type, String source, String target) {
        this.type = type;
        this.source = source;
        this.target = target;
    }

    public String getType() {
        return type;
    }

    public String getSourceAlias() {
        return source;
    }

    public String getTargetAlias() {
        return target;
    }
}
