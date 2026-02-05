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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestionSpecs {
    public final String id;
    public final String purpose;
    public final int concurrency;
    public final List<Question> questions = new ArrayList<>();
    public final List<String> answers = new ArrayList<>();

    public QuestionSpecs(String id, String purpose, int concurrency) {
        this.id = id;
        this.purpose = purpose;
        this.concurrency = Math.clamp(concurrency, 1, 30);
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public void addAnswer(List<String> answer) {
        this.answers.addAll(answer);
    }

    public Set<String> getExpectedOutput() {
        var result = new HashSet<String>();
        for (Question question : questions) {
            result.addAll(question.output);
        }
        return result;
    }
}
