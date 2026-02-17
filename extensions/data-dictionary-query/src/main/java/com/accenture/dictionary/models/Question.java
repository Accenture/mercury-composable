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

public class Question {
    public final String id;
    public final List<String> forEach = new ArrayList<>();
    public final List<String> input = new ArrayList<>();
    public final List<String> output = new ArrayList<>();

    public Question(String id) {
        this.id = id;
    }

    public void addForEach(String entry) {
        if (entry != null) {
            forEach.add(entry.trim());
        }
    }

    public void addInput(String entry) {
        if (entry != null) {
            this.input.add(entry.trim());
        }
    }

    public void addOutput(String entry) {
        if (entry != null) {
            this.output.add(entry);
        }
    }
}
