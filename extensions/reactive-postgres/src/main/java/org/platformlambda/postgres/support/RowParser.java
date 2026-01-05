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

package org.platformlambda.postgres.support;

import io.r2dbc.spi.Row;
import org.platformlambda.core.serializers.SimpleMapper;

import java.util.HashMap;
import java.util.Map;

public class RowParser {

    private RowParser() { /* hidden constructor */ }

    /**
     * Convert a Row into a Map
     * @param row read from a reactive database
     * @return map of key-values
     */
    public static Map<String, Object> toMap(Row row) {
        Map<String, Object> rec = new HashMap<>();
        var md = row.getMetadata();
        var columns = md.getColumnMetadatas();
        for (var c : columns) {
            var value = row.get(c.getName());
            rec.put(c.getName(), value);
        }
        return rec;
    }

    /**
     * Convert a Row into a PoJo
     *
     * @param row read from a reactive database
     * @param toValueType class name of the PoJo
     * @return poJo
     * @param <T> class
     */
    public static <T> T toPoJo(Row row, Class<T> toValueType) {
        return SimpleMapper.getInstance().getMapper().readValue(toMap(row), toValueType);
    }
}
