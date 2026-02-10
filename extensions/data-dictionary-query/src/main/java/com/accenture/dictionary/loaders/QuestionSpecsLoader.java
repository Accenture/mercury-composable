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

package com.accenture.dictionary.loaders;

import com.accenture.dictionary.models.Question;
import com.accenture.dictionary.models.QuestionSpecs;
import com.accenture.util.DataMappingHelper;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestionSpecsLoader {
    private static final String ARROW = "->";
    private static final String RESULT = "result.";
    private static final DataMappingHelper helper = DataMappingHelper.getInstance();
    private static final QuestionSpecsLoader INSTANCE = new QuestionSpecsLoader();

    private QuestionSpecsLoader() {
        // singleton
    }

    public static QuestionSpecsLoader getInstance() {
        return INSTANCE;
    }

    public QuestionSpecs loadQuestionSpecs(String questionId, ConfigReader config) {
        var purpose = config.getProperty("purpose", "");
        var questions = config.get("question");
        var answer = config.get("answer");
        var concurrency = Utility.getInstance().str2int(config.getProperty("concurrency", "5"));
        if (!purpose.isEmpty() && questions instanceof List<?> questionList &&
                answer instanceof List<?> answerList && !questionList.isEmpty() && !answerList.isEmpty()) {
            QuestionSpecs qs = new QuestionSpecs(questionId, purpose, concurrency);
            int n = 0;
            for (Object question : questionList) {
                n++;
                qs.addQuestion(getEachQuestion(n, questionId, question));
            }
            qs.addAnswer(getValidatedAnswerList(questionId, answerList));
            return qs;
        } else {
            throw new IllegalArgumentException("Invalid syntax in "+questionId+" - check purpose, question and answer");
        }
    }

    private Question getEachQuestion(int n, String questionId, Object question) {
        if (question instanceof Map<?, ?> entry) {
            var id = entry.get("id");
            var input = entry.get("input");
            var output = entry.get("output");
            if (id instanceof String identifier && !identifier.isEmpty() &&
                    input instanceof List<?> inputList && output instanceof List<?> outputList &&
                    !inputList.isEmpty() && !outputList.isEmpty()) {
                return prepareEachQuestion(n, entry, questionId, identifier, inputList, outputList);
            }
        }
        throw new IllegalArgumentException("Invalid question entry -"+n+" for "+questionId);
    }

    private String getForEach(Object entry) {
        if (entry instanceof String text) {
            return text;
        } else if (entry == null) {
            return null;
        } else {
            throw new IllegalArgumentException("The 'for_each' entry must be text - "+entry);
        }
    }

    private Question prepareEachQuestion(int n, Map<?, ?> entry, String questionId, String identifier,
                                         List<?> inputList, List<?> outputList) {
        var forEachEntry = getForEach(entry.get("for_each"));
        validateForEachEntry(questionId, forEachEntry);
        Question q = new Question(identifier, forEachEntry);
        for (Object e : inputList) {
            var text = String.valueOf(e);
            validateInput(questionId, text);
            q.addInput(text);
        }
        for (Object e : outputList) {
            var text = String.valueOf(e);
            validateOutput(questionId, text);
            q.addOutput(text);
        }
        if (countInputParameters(q) == 0) {
            throw new IllegalArgumentException("Cannot resolve input parameter in entry-"+n+" for "+questionId);
        }
        if (forEachEntry != null && q.output.size() != 1) {
            throw new IllegalArgumentException("With 'for_each' enabled, output should only have one value in "+identifier);
        }
        if (q.output.isEmpty()) {
            throw new IllegalArgumentException("Missing output in entry-"+n+" for "+questionId);
        }
        return q;
    }

    private int countInputParameters(Question question) {
        int n = 0;
        for (String text : question.input) {
            var rhs = text.substring(text.indexOf(ARROW) + ARROW.length()).trim();
            if (!rhs.contains(".")) {
                n++;
            }
        }
        return n;
    }

    private List<String> getValidatedAnswerList(String questionId, List<?> answerList) {
        List<String> result = new ArrayList<>();
        for (Object answer : answerList) {
            var text = String.valueOf(answer);
            if (!text.contains(ARROW)) {
                throw new IllegalArgumentException("Invalid answer entry in "+questionId+" - missing '->'");
            }
            if (!helper.validOutput(text)) {
                throw new IllegalArgumentException("Invalid answer entry in "+questionId+
                        " - RHS must be output.status, or start with output.body. or output.header.");
            }
            result.add(text);
        }
        return result;
    }

    private void validateInput(String questionId, String text) {
        if (!text.contains(ARROW)) {
            throw new IllegalArgumentException("Invalid input in "+questionId+" - missing '->'");
        }
        var lhs = text.substring(0, text.indexOf(ARROW)).trim();
        if (invalidInputLhs(lhs)) {
            throw new IllegalArgumentException("Invalid input in "+questionId+
                                " - must start with input.body. or input.header. or prior result.");
        }
    }

    private void validateOutput(String questionId, String text) {
        if (text.contains(ARROW)) {
            throw new IllegalArgumentException("Invalid output in "+questionId+" - ("+text+") should not contain '->'");
        }
        if (text.contains(".")) {
            throw new IllegalArgumentException("Invalid output in "+questionId+" - ("+text+") should not contain '.'");
        }
    }

    private boolean invalidInputLhs(String text) {
        if (text.startsWith(RESULT)) {
            return false;
        } else {
            return !helper.validInput(text + " " + ARROW + " placeholder");
        }
    }

    private void validateForEachEntry(String questionId, String text) {
        if (text != null) {
            int sep = text.indexOf(ARROW);
            if (sep == -1) {
                throw new IllegalArgumentException("Invalid 'for_each' in "+questionId+" - missing '->'");
            }
            var lhs = text.substring(0, sep).trim();
            if (invalidInputLhs(lhs)) {
                throw new IllegalArgumentException("Invalid 'for_each' LHS in " + questionId +
                        " - must start with input.body. or input.header. or prior result.");
            }
        }
    }
}
