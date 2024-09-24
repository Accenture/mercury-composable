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

package org.platformlambda.core.serializers;

import org.platformlambda.core.models.TypedPayload;
import org.platformlambda.core.util.Utility;

import java.util.*;

public class PayloadMapper {
    public static final String MAP = "M";
    public static final String LIST = "L";
    public static final String PRIMITIVE = "P";
    public static final String NOTHING = "N";

    private static final PayloadMapper PAYLOAD_MAPPER_INSTANCE = new PayloadMapper();

    public static PayloadMapper getInstance() {
        return PAYLOAD_MAPPER_INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public TypedPayload encode(Object obj, boolean binary) {
        if (obj == null) {
            return new TypedPayload(NOTHING, null);
        } else if (obj instanceof Map) {
            return new TypedPayload(MAP, obj);
        } else if (obj instanceof Object[] objArray) {
            return encodeList(Arrays.asList(objArray), binary);
        } else if (obj instanceof List) {
            return encodeList((List<Object>) obj, binary);
        } else if (obj instanceof Date d) {
            return new TypedPayload(PRIMITIVE, Utility.getInstance().date2str(d));
        } else if (isPrimitive(obj)) {
            return new TypedPayload(PRIMITIVE, obj);
        } else {
            return getTypedPayload(obj, binary);
        }
    }

    private TypedPayload encodeList(List<Object> objects, boolean binary) {
        SimpleObjectMapper mapper = SimpleMapper.getInstance().getMapper();
        Utility util = Utility.getInstance();
        List<Object> list = new ArrayList<>();
        for (Object o: objects) {
            if (o == null) {
                list.add(null);
            } else if (isPrimitive(o)) {
                list.add(o);
            } else if (isPrimitive(o) || o instanceof Map) {
                list.add(o);
            } else {
                if (util.isPoJo(o)) {
                    if (binary) {
                        list.add(mapper.readValue(o, Map.class));
                    } else {
                        list.add(mapper.writeValueAsBytes(o));
                    }
                } else {
                    throw new IllegalArgumentException("Unable to serialize because it is not a list of PoJo");
                }
            }
        }
        return new TypedPayload(LIST, list);
    }

    private TypedPayload getTypedPayload(Object obj, boolean binary) {
        SimpleObjectMapper mapper = SimpleMapper.getInstance().getMapper();
        if (binary) {
            return new TypedPayload(obj.getClass().getName(), mapper.readValue(obj, Map.class));
        } else {
            return new TypedPayload(obj.getClass().getName(), mapper.writeValueAsBytes(obj));
        }
    }

    public Object decode(TypedPayload typed) {
        return typed.getPayload();
    }

    public boolean isPrimitive(Object obj) {
        return (obj instanceof String || obj instanceof byte[] || obj instanceof Number || obj instanceof Boolean);
    }

}
