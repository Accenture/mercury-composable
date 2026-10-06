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

package com.accenture.minigraph.packager;

import org.platformlambda.core.serializers.SimpleMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prints how this JVM's SimpleMapper writes a map holding a null value: with serializer.null.transport=true the
 * null is kept, by default it is dropped. GraphPackagerTest runs it in a second JVM to prove the switch took effect
 * there before it compares that JVM's package with the default one.
 */
public class NullTransportProbe {

    public static void main(String[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", null);
        map.put("b", "");
        System.out.println(SimpleMapper.getInstance().getMapper().writeValueAsString(map));
    }
}
