/*

    Copyright 2018-2024 Accenture Technology

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

package com.accenture.automation;

import com.accenture.models.Flow;
import com.accenture.models.Flows;
import com.accenture.models.Task;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

@BeforeApplication(sequence=3)
public class CompileFlows implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(CompileFlows.class);

    private static final String INPUT = "input";
    private static final String PROCESS = "process";
    private static final String OUTPUT = "output";
    private static final String DESCRIPTION = "description";
    private static final String EXECUTION = "execution";
    private static final String RESULT = "result";
    private static final String STATUS = "status";
    private static final String DECISION = "decision";
    private static final String DELAY = "delay";
    private static final String EXCEPTION = "exception";
    private static final String LOOP = "loop";
    private static final String FLOW_PROTOCOL = "flow://";
    private static final String HTTP_INPUT_NAMESPACE = "http.input.";
    private static final String HTTP_OUTPUT_NAMESPACE = "http.output.";
    private static final String INPUT_NAMESPACE = "input.";
    private static final String OUTPUT_NAMESPACE = "output.";
    private static final String MODEL_NAMESPACE = "model.";
    private static final String RESULT_NAMESPACE = "result.";
    private static final String HEADER_NAMESPACE = "header.";
    private static final String HEADER = "header";
    private static final String ERROR_NAMESPACE = "error.";
    private static final String TEXT_TYPE = "text(";
    private static final String INTEGER_TYPE = "int(";
    private static final String LONG_TYPE = "long(";
    private static final String FLOAT_TYPE = "float(";
    private static final String DOUBLE_TYPE = "double(";
    private static final String BOOLEAN_TYPE = "boolean(";
    private static final String CLASSPATH_TYPE = "classpath(";
    private static final String FILE_TYPE = "file(";
    private static final String CLOSE_BRACKET = ")";
    private static final String MAP_TO = "->";
    private static final String TASKS = "tasks";
    private static final String NEXT = "next";
    private static final String END = "end";
    private static final String RESPONSE = "response";
    private static final String SEQUENTIAL = "sequential";
    private static final String PARALLEL = "parallel";
    private static final String PIPELINE = "pipeline";
    private static final String FORK = "fork";
    private static final String JOIN = "join";
    private static final String SINK = "sink";
    private static final String STATEMENT = "statement";
    private static final String FOR = "for";
    private static final String WHILE = "while";
    private static final String CONTINUE = "continue";
    private static final String BREAK = "break";
    private static final String INCREMENT = "++";
    private static final String DECREMENT = "--";
    private static final String CONDITION = "condition";
    private static final String SKIP_INVALID_TASK = "Skip invalid task";
    private static final String[] EXECUTION_TYPES = {DECISION, RESPONSE, END,
                                                     SEQUENTIAL, PARALLEL, PIPELINE, FORK, SINK};

    /**
     * This main class is only used when testing the app from the IDE.
     *
     * @param args - command line arguments
     */
    public static void main(String[] args) {
        AppStarter.main(args);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void start(String[] args) {
        Utility util = Utility.getInstance();
        AppConfigReader config = AppConfigReader.getInstance();
        String locations = config.getProperty("yaml.flow.automation", "classpath:/flows.yaml");
        List<String> paths = util.split(locations, ", ");
        for (String p: paths) {
            ConfigReader reader = new ConfigReader();
            try {
                reader.load(p);
            } catch (IOException e) {
                log.error("Skipping some event scripts - {}", e.getMessage());
                continue;
            }
            log.info("Loading event scripts from {}", p);
            String prefix = reader.getProperty("location", "classpath:/flows/");
            Object flowConfig = reader.get("flows");
            if (flowConfig instanceof List flows) {
                Set<String> uniqueFlows = new HashSet<>();
                for (int i = 0; i < flows.size(); i++) {
                    String f = reader.getProperty("flows[" + i + "]");
                    if (f.endsWith(".yml") || f.endsWith(".yaml")) {
                        uniqueFlows.add(f);
                    } else {
                        log.error("Ignored {} because it does not have .yml or .yaml file extension", f);
                    }
                }
                List<String> ordered = new ArrayList<>(uniqueFlows);
                Collections.sort(ordered);
                for (String f: ordered) {
                    if (f.endsWith(".yml") || f.endsWith(".yaml")) {
                        ConfigReader flow = new ConfigReader();
                        try {
                            flow.load(prefix + f);
                            log.info("Parsing {}{}", prefix, f);
                            createFlow(f, flow);
                        } catch (IOException e) {
                            log.error("Ignored {} - {}", f, e.getMessage());
                        }
                    }
                }
            }
        }
        List<String> flows = Flows.getAllFlows();
        Collections.sort(flows);
        for (String f : flows) {
            log.info("Loaded {}", f);
        }
        log.info("Event scripts deployed: {}", flows.size());
    }

    @SuppressWarnings("unchecked")
    private void createFlow(String name, ConfigReader flow) {
        Utility util = Utility.getInstance();
        Object id = flow.get("flow.id");
        Object description = flow.get("flow.description");
        Object timeToLive = flow.get("flow.ttl");
        Object exceptionTask = flow.get("flow.exception");
        Object firstTask = flow.get("first.task");
        if (id instanceof String flowId && description instanceof String desc
                && timeToLive instanceof String ttl && firstTask instanceof String task1) {
            if (Flows.flowExists(flowId)) {
                log.error("Skip {} - Flow '{}' already exists", name, flowId);
                return;
            }
            String unhandledException = exceptionTask instanceof String et? et : null;
            // minimum 1 second for TTL
            long ttlSeconds = Math.max(1, util.getDurationInSeconds(ttl));
            Flow entry = new Flow(flowId, desc, task1, ttlSeconds * 1000L, unhandledException);
            int taskCount = 0;
            Object taskList = flow.get(TASKS);
            if (taskList instanceof List) {
                taskCount = ((List<?>) taskList).size();
            }
            if (taskCount == 0) {
                log.error("Unable to parse {} - 'tasks' section is empty or invalid", name);
                return;
            }
            boolean endTaskFound = false;
            for (int i=0; i < taskCount; i++) {
                // input or output are optional
                Object input = flow.get(TASKS+"["+i+"]."+INPUT, new ArrayList<>());
                Object output = flow.get(TASKS+"["+i+"]."+OUTPUT, new ArrayList<>());
                Object process = flow.get(TASKS+"["+i+"]."+PROCESS);
                Object taskDesc = flow.get(TASKS+"["+i+"]."+DESCRIPTION);
                Object execution = flow.get(TASKS+"["+i+"]."+EXECUTION);
                Object delay = flow.get(TASKS+"["+i+"]."+DELAY);
                Object taskException = flow.get(TASKS+"["+i+"]."+EXCEPTION);
                Object taskLoop = flow.get(TASKS+"["+i+"]."+LOOP);
                if (input instanceof List && output instanceof List &&
                        process instanceof String processName &&
                        taskDesc instanceof String taskDescription &&
                        execution instanceof String taskExecution && validExecutionType(taskExecution)) {
                    boolean validTask = true;
                    if (processName.contains("://") && !processName.startsWith(FLOW_PROTOCOL) &&
                            processName.length() <= FLOW_PROTOCOL.length()) {
                        log.error("{} {} in {}. check syntax for flow://{flow-name}", SKIP_INVALID_TASK, process, name);
                        return;
                    }
                    Task task = new Task(processName, taskDescription, taskExecution);
                    if (delay instanceof Integer) {
                        long n = util.str2long(delay.toString());
                        if (n < entry.ttl) {
                            task.setDelay(n);
                        } else {
                            log.error("{} {} in {}. delay must be less than TTL", SKIP_INVALID_TASK, process, name);
                            return;
                        }
                    } else if (delay instanceof String d) {
                        List<String> dParts = util.split(d, ".");
                        if (dParts.size() > 1 && d.startsWith(MODEL_NAMESPACE)) {
                            task.setDelayVar(d);
                        } else {
                            log.error("{} {} in {}. delay variable must starts with 'model.'",
                                    SKIP_INVALID_TASK, process, name);
                            return;
                        }
                    }
                    if (taskException instanceof String te) {
                        task.setExceptionTask(te);
                    }
                    if (END.equals(execution)) {
                        endTaskFound = true;
                    } else {
                        if (FORK.equals(execution)) {
                            Object join = flow.get(TASKS+"["+i+"]."+JOIN);
                            if (join instanceof String jt) {
                                task.setJoinTask(jt);
                            } else {
                                log.error("{} {} in {}. Missing a join task", SKIP_INVALID_TASK, process, name);
                                return;
                            }
                        }
                        /*
                         * A sink function does not have next steps.
                         * This task type is used in a "pipeline" or "fork" task.
                         *
                         * A sequential or pipeline must have one next task.
                         */
                        if (!SINK.equals(execution)) {
                            List<String> nextTasks = (List<String>) flow.get(TASKS+"["+i+"]."+NEXT, new ArrayList<>());
                            if (nextTasks.isEmpty()) {
                                log.error("{} {} in {}. Missing a list of next tasks",
                                        SKIP_INVALID_TASK, process, name);
                                return;
                            }
                            if (nextTasks.size() > 1 && (SEQUENTIAL.equals(execution) || PIPELINE.equals(execution))) {
                                log.error("Invalid {} task {} in {}. Expected one next task, Actual: {}",
                                            execution, process, name, nextTasks.size());
                                return;
                            }
                            task.nextSteps.addAll(nextTasks);
                        }
                        /*
                         * A pipeline task must have at least one pipeline step
                         */
                        if (PIPELINE.equals(execution)) {
                            List<String> pipelineSteps = (List<String>) flow.get(TASKS+"["+i+"]."+PIPELINE, new ArrayList<>());
                            if (pipelineSteps.isEmpty()) {
                                log.error("{} {} in {}. Missing a list of pipeline steps",
                                        SKIP_INVALID_TASK, process, name);
                                return;
                            }
                            task.pipelineSteps.addAll(pipelineSteps);
                            if (taskLoop != null) {
                                if (taskLoop instanceof Map) {
                                    MultiLevelMap loopMap = new MultiLevelMap((Map<String, Object>) taskLoop);
                                    Object statement = loopMap.getElement(STATEMENT);
                                    Object conditions = loopMap.getElement(CONDITION);
                                    if (statement instanceof String s) {
                                        int bracket = s.indexOf('(');
                                        if (bracket == -1) {
                                            log.error("{} {} in {}. Please check loop.statement",
                                                    SKIP_INVALID_TASK, process, name);
                                            return;
                                        }
                                        String type = s.substring(0, bracket).trim();
                                        if (!type.equals(FOR) && !type.equals(WHILE)) {
                                            log.error("{} {} in {}. loop.statement must be 'for' or 'while'",
                                                    SKIP_INVALID_TASK, process, name);
                                            return;
                                        }
                                        task.setLoopType(type);
                                        List<String> parts = util.split(s.substring(bracket+1), "(;)");
                                        if (type.equals(FOR)) {
                                            if (parts.size() < 2 || parts.size() > 3) {
                                                log.error("{} {} in {}. 'for' loop should have 2 or 3 segments",
                                                        SKIP_INVALID_TASK, process, name);
                                                return;
                                            }
                                            if (parts.size() == 2) {
                                                task.comparator.addAll(getForPart2(parts.getFirst()));
                                                task.sequencer.addAll(getForPart3(parts.get(1)));
                                            }
                                            if (parts.size() == 3) {
                                                List<String> initializer = getForPart1(parts.getFirst());
                                                if (initializer.isEmpty()) {
                                                    log.error("{} {} in {}. Please check for-loop initializer. e.g. " +
                                                                    "'for (model.n = 0; model.n < 3; model.n++)'",
                                                            SKIP_INVALID_TASK, process, name);
                                                    return;
                                                }
                                                task.init.addAll(initializer);
                                                task.comparator.addAll(getForPart2(parts.get(1)));
                                                task.sequencer.addAll(getForPart3(parts.get(2)));

                                            }
                                            if (task.comparator.isEmpty() || task.sequencer.isEmpty()) {
                                                log.error("{} {} in {}. Please check for-loop syntax. e.g. " +
                                                                "'for (model.n = 0; model.n < 3; model.n++)'",
                                                        SKIP_INVALID_TASK, process, name);
                                                return;
                                            }
                                            if (!validForStatement(task.comparator, task.sequencer)) {
                                                log.error("{} {} in {}. 'for' loop has invalid comparator or sequencer",
                                                        SKIP_INVALID_TASK, process, name);
                                                return;
                                            }

                                        } else {
                                            if (parts.size() != 1) {
                                                log.error("{} {} in {}. 'while' loop should have only one value",
                                                        SKIP_INVALID_TASK, process, name);
                                                return;
                                            }
                                            String modelKey = parts.getFirst().trim();
                                            if (!modelKey.startsWith(MODEL_NAMESPACE) ||
                                                    modelKey.contains("=") || modelKey.contains(" ")) {
                                                log.error("{} {} in {}. 'while' should use a model key",
                                                        SKIP_INVALID_TASK, process, name);
                                                return;
                                            }
                                            task.setWhileModelKey(modelKey);
                                        }

                                    } else {
                                        log.error("{} {} in {}. {} loop.statement", SKIP_INVALID_TASK,
                                                process, name, statement == null? "Missing" : "Please check");
                                        return;
                                    }
                                    if (conditions != null) {
                                        if (conditions instanceof List) {
                                            List<List<String>> conditionList = getConditionList((List<String>) conditions);
                                            if (conditionList.isEmpty()) {
                                                log.error("{} {} in {}. please check loop.condition",
                                                        SKIP_INVALID_TASK, process, name);
                                                return;
                                            }
                                            task.conditions.addAll(conditionList);

                                        } else {
                                            log.error("{} {} in {}. loop.condition should be " +
                                                    "a list of 'if' statements", SKIP_INVALID_TASK, process, name);
                                            return;
                                        }
                                    }

                                } else {
                                    log.error("{} {} in {}. 'loop' must be a map of for/while statement and conditions",
                                            SKIP_INVALID_TASK, process, name);
                                    return;
                                }
                            }
                        }
                    }
                    List<Object> inputList = (List<Object>) input;
                    for (int j=0; j < inputList.size(); j++) {
                        String line = flow.getProperty(TASKS+"["+i+"]."+INPUT+"["+j+"]");
                        String filtered = filterInputAlias(line);
                        if (validInput(filtered)) {
                            task.input.add(filtered);
                        } else {
                            log.error("Skip invalid task {} in {} has invalid input {}", process, name, line);
                            validTask = false;
                        }
                    }
                    boolean isDecisionTask = DECISION.equals(execution);
                    List<Object> outputList = (List<Object>) output;
                    for (int j=0; j < outputList.size(); j++) {
                        String line = flow.getProperty(TASKS+"["+i+"]."+OUTPUT+"["+j+"]");
                        String filtered = filterOutputAlias(line);
                        if (validOutput(filtered, isDecisionTask)) {
                            task.output.add(filtered);
                        } else {
                            log.error("Skip invalid task {} in {} has invalid output {}", process, name, line);
                            validTask = false;
                        }
                    }
                    if (isDecisionTask && task.nextSteps.size() < 2) {
                        log.error("Decision task {} in {} must have at least 2 next tasks", process, name);
                        validTask = false;
                    }
                    if (validTask) {
                        entry.addTask(task);
                    }

                } else {
                    log.error("Unable to parse {} - " +
                            "a task must contain input, process, output, description and execution", name);
                }
            }
            if (endTaskFound) {
                Flows.addFlow(entry);

            } else {
                log.error("Unable to parse {} - flow must have at least one end task", name);
            }

        } else {
            log.error("Unable to parse {} - check flow.id, flow.description, flow.ttl, first.task", name);
        }
    }

    private List<List<String>> getConditionList(List<String> rawList) {
        List<List<String>> result = new ArrayList<>();
        for (String item: rawList) {
            List<String> condition = getCondition(item);
            if (condition.isEmpty()) {
                return Collections.emptyList();
            } else {
                // ensure there is only one and only one condition
                int control = 0;
                for (String c: condition) {
                    if (c.equals(BREAK) || c.equals(CONTINUE)) {
                        control++;
                    }
                }
                if (control > 1) {
                    return Collections.emptyList();
                }
                result.add(condition);
            }
        }
        return result;
    }

    private List<String> getCondition(String statement) {
        Utility util = Utility.getInstance();
        List<String> parts = util.split(statement, " ()");
        if (parts.size() == 3 && parts.getFirst().equals("if") && parts.get(1).startsWith(MODEL_NAMESPACE) &&
                    (parts.get(2).equals(CONTINUE) || parts.get(2).equals(BREAK)) ) {
            List<String> result = new ArrayList<>();
            result.add(parts.get(1));
            result.add(parts.get(2));
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private List<String> getForPart1(String text) {
        Utility util = Utility.getInstance();
        // add spaces for easy parsing
        List<String> parts = util.split(text.replace("=", " = "), " ");
        List<String> result = new ArrayList<>();
        if (parts.size() == 3 && parts.getFirst().startsWith(MODEL_NAMESPACE) && util.isNumeric(parts.get(2)) &&
                parts.get(1).equals("=")) {
            result.add(parts.getFirst());
            result.add(parts.get(2));
        }
        return result;
    }

    private List<String> getForPart2(String text) {
        String s = text;
        // add spaces for easy parsing
        if (s.contains(">=")) {
            s = s.replace(">=", " >= ");
        } else if (s.contains("<=")) {
            s = s.replace("<=", " <= ");
        } else if (s.contains("<")) {
            s = s.replace("<", " < ");
        } else if (s.contains(">")) {
            s = s.replace(">", " > ");
        }
        Utility util = Utility.getInstance();
        List<String> parts = util.split(s, " ");
        List<String> result = new ArrayList<>();
        if (parts.size() == 3 &&
                (parts.getFirst().startsWith(MODEL_NAMESPACE) || util.isNumeric(parts.getFirst())) &&
                (parts.get(2).startsWith(MODEL_NAMESPACE) || util.isNumeric(parts.get(2))) &&
                (parts.get(1).equals(">=") || parts.get(1).equals("<=") ||
                        parts.get(1).equals(">") || parts.get(1).equals("<"))) {
            result.add(parts.getFirst());
            result.add(parts.get(1));
            result.add(parts.get(2));
        }
        return result;
    }

    private List<String> getForPart3(String text) {
        String s = text.trim();
        List<String> result = new ArrayList<>();
        final boolean plus;
        if (s.endsWith("++") || s.startsWith("++")) {
            plus = true;
        } else if (s.endsWith("--") || s.startsWith("--")) {
            plus = false;
        } else {
            return result;
        }
        if ((s.startsWith("+") || s.startsWith("-")) && (s.endsWith("+") || s.endsWith("-"))) {
            return result;
        }
        String key = s.replace('+', ' ').replace('-', ' ').trim();
        if (key.startsWith(MODEL_NAMESPACE)) {
            result.add(key);
            result.add(plus? INCREMENT : DECREMENT);
        }
        return result;
    }

    private boolean validForStatement(List<String> comparator, List<String> sequencer) {
        List<String> keys = new ArrayList<>();
        for (String k: comparator) {
            if (k.startsWith(MODEL_NAMESPACE)) {
                keys.add(k);
            }
        }
        if (keys.isEmpty()) {
            return false;
        }
        if (keys.size() == 2 && keys.getFirst().equals(keys.get(1))) {
            return false;
        }
        boolean found = false;
        for (String k: keys) {
            if (k.equals(sequencer.getFirst())) {
                found = true;
            }
        }
        return found;
    }

    private boolean validExecutionType(String execution) {
        for (String s: EXECUTION_TYPES) {
            if (s.equals(execution)) {
                return true;
            }
        }
        return false;
    }

    private String filterInputAlias(String mapping) {
        String text = mapping.trim();
        int sep = text.indexOf(MAP_TO);
        if (sep == -1) {
            return mapping;
        }
        String lhs = text.substring(0, sep).trim();
        String rhs = text.substring(sep+2).trim();
        if (lhs.startsWith(HTTP_INPUT_NAMESPACE)) {
            lhs = INPUT_NAMESPACE + lhs.substring(HTTP_INPUT_NAMESPACE.length());
        }
        return lhs + " " + MAP_TO + " " + rhs;
    }

    private String filterOutputAlias(String mapping) {
        String text = mapping.trim();
        int sep = text.indexOf(MAP_TO);
        if (sep == -1) {
            return mapping;
        }
        String lhs = text.substring(0, sep).trim();
        String rhs = text.substring(sep+2).trim();
        if (rhs.startsWith(HTTP_OUTPUT_NAMESPACE)) {
            rhs = OUTPUT_NAMESPACE + rhs.substring(HTTP_OUTPUT_NAMESPACE.length());
        }
        return lhs + " " + MAP_TO + " " + rhs;
    }

    private boolean validInput(String input) {
        int sep = input.indexOf(MAP_TO);
        if (sep > 0) {
            String lhs = input.substring(0, sep).trim();
            String rhs = input.substring(sep+2).trim();
            if (!rhs.isEmpty()) {
                if (lhs.equals(INPUT) || lhs.startsWith(INPUT_NAMESPACE) ||
                        lhs.startsWith(MODEL_NAMESPACE) || lhs.startsWith(ERROR_NAMESPACE)) {
                    return true;
                } else {
                    return (lhs.startsWith(TEXT_TYPE) ||
                            lhs.startsWith(FILE_TYPE) || lhs.startsWith(CLASSPATH_TYPE) ||
                            lhs.startsWith(INTEGER_TYPE) || lhs.startsWith(LONG_TYPE) ||
                            lhs.startsWith(FLOAT_TYPE) || lhs.startsWith(DOUBLE_TYPE) ||
                            lhs.startsWith(BOOLEAN_TYPE)) && lhs.endsWith(CLOSE_BRACKET);
                }
            }
        }
        return false;
    }

    private boolean validOutput(String output, boolean isDecision) {
        int sep = output.indexOf(MAP_TO);
        if (sep > 0) {
            String lhs = output.substring(0, sep).trim();
            String rhs = output.substring(sep+2).trim();
            return validOutputLhs(lhs) && validOutputRhs(rhs, isDecision);
        }
        return false;
    }

    private boolean validOutputLhs(String lhs) {
        if (lhs.equals(INPUT)
                || lhs.startsWith(INPUT_NAMESPACE)
                || lhs.startsWith(MODEL_NAMESPACE)
                || lhs.equals(RESULT) || lhs.startsWith(RESULT_NAMESPACE)
                || lhs.equals(STATUS)
                || lhs.equals(HEADER) || lhs.startsWith(HEADER_NAMESPACE)) {
            return true;
        } else {
            return (lhs.startsWith(TEXT_TYPE) || lhs.startsWith(FILE_TYPE) ||
                    lhs.startsWith(INTEGER_TYPE) || lhs.startsWith(LONG_TYPE) ||
                    lhs.startsWith(FLOAT_TYPE) || lhs.startsWith(DOUBLE_TYPE) ||
                    lhs.startsWith(BOOLEAN_TYPE)) && lhs.endsWith(CLOSE_BRACKET);
        }
    }

    private boolean validOutputRhs(String rhs, boolean isDecision) {
        return (rhs.equals(DECISION) && isDecision) || rhs.startsWith(FILE_TYPE) ||
                rhs.startsWith(OUTPUT_NAMESPACE) || rhs.startsWith(MODEL_NAMESPACE);
    }

}
