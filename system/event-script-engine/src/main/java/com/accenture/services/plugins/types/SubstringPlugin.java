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

package com.accenture.services.plugins.types;

import com.accenture.utils.TypeConversionUtils;
import com.accenture.models.SimplePlugin;
import com.accenture.models.PluginFunction;

@SimplePlugin
public class SubstringPlugin implements PluginFunction {

    @Override
    public String getName() {
        return "substring";
    }

    @Override
    public Object calculate(Object... input) {
        String value = TypeConversionUtils.getTextValue(input[0]);
        int start = (input.length > 1)?  TypeConversionUtils.convertInteger(input[1]) : -1;
        int end = (input.length > 2)?  TypeConversionUtils.convertInteger(input[2]) : -1;
        if (isOutOfBounds(value, start, end)) {
            throw new IllegalArgumentException("Substring indexes are out of bounds: [" + start + ", " + end + "]");
        }
        if (start >= 0 && end >= 0) { // Start and end indexes are in bound
            return value.substring(start, end);
        } else if (start >= 0 && start < value.length()) { //Start index is in bound
            return value.substring(start);
        }
        return value;
    }

    private boolean isOutOfBounds(String value, int start, int end){
        return (end >= 0 && end > value.length()) || // End is out of bounds
                (start >= 0 && start > value.length()) || // Start is out of bounds
                (start > end && start >= 0 && end >= 0); // start and end are flipped
    }
}
