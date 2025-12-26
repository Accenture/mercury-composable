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

package com.accenture.models;

public class PipeInfo {
    protected String type;

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param type for a pipeline
     */
    public PipeInfo(String type) {
        this.type = type;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return type for a pipeline
     */
    public String getType() {
        return type;
    }

}
