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

public class DataProvider {
    public final String id;
    public final String protocol;
    public final String service;
    public final String url;
    public final String method;
    public final List<String> skills = new ArrayList<>();
    public final List<String> headers = new ArrayList<>();
    public final List<String> input = new ArrayList<>();

    public DataProvider(String id, String protocol, String service, String url, String method) {
        this.id = id;
        this.protocol = protocol;
        this.service = service;
        this.url = url;
        this.method = method;
    }

    public void addInput(String entry) {
        this.input.add(entry);
    }

    public void addHeader(String header) {
        this.headers.add(header);
    }

    public void addSkill(String skill) {
        this.skills.add(skill);
    }
}
