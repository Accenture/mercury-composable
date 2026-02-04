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

package com.accenture.dictionary.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service parameters for array question
 */
public class ServiceParameters {
    public String service;
    public DataDictionary item;
    public final List<Map<String, Object>> parameters = new ArrayList<>();

    public void addParameterMap(Map<String, Object> map) {
        parameters.add(map);
    }
}
