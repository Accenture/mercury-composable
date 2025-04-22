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
import java.util.HashMap;
import java.util.Map;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class TraceInfo {
    public final String route;
    public final String id;
    public final String path;
    public final String startTime = Utility.getInstance().date2str(new Date());
    public final Map<String, Object> annotations = new HashMap<>();

    public TraceInfo(String route, String id, String path) {
        this.route = route;
        if (id == null) {
            this.id = null;
            this.path = null;
        } else {
            this.id = id;
            this.path = path;
        }
    }

    public void annotate(String key, Object value) {
        annotations.put(key, value);
    }

}
