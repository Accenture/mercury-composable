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

import org.platformlambda.core.util.Utility;

import java.util.Date;

public class Kv {

    public final String key;
    public final String value;

    /**
     * Key-Value Pair
     *
     * @param key in string
     * @param value object will be converted to string
     */
    public Kv(String key, Object value) {
        this.key = key;
        // null value is transported as an empty string
        switch (value) {
            case null -> this.value = "";
            case String str -> this.value = str;
            case Date d -> this.value = Utility.getInstance().date2str(d);
            default -> this.value = String.valueOf(value);
        }
    }

}
