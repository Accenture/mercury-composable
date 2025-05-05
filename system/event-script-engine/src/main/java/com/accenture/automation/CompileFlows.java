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

package com.accenture.automation;

import com.accenture.models.Flow;
import com.accenture.models.Flows;
import com.accenture.models.Task;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AppStarter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 * <p>
 * Event Script should start right after essential services
 * Therefore, we set sequence number to 2 and essential services to 0.
 * <p>
 * If you have a reason to execute another BeforeApplication module before
 * Event Script starts, you can set it to 1.
 */
@BeforeApplication(sequence=2)
public class CompileFlows implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(CompileFlows.class);
    private static final String INPUT = "input";
    private static final String PROCESS = "process";
    private static final String NAME = "name";
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
    private static final String INPUT_NAMESPACE = "input.";
    private static final String OUTPUT_NAMESPACE = "output.";
    private static final String MODEL = "model";
    private static final String PARENT = "parent";
    private static final String MODEL_NAMESPACE = "model.";
    private static final String NEGATE_MODEL = "!model.";
    private static final String RESULT_NAMESPACE = "result.";
    private static final String HEADER_NAMESPACE = "header.";
    private static final String HEADER = "header";
    private static final String ERROR_NAMESPACE = "error.";
    private static final String EXT_NAMESPACE = "ext:";
    private static final String TEXT_TYPE = "text(";
    private static final String INTEGER_TYPE = "int(";
    private static final String LONG_TYPE = "long(";
    private static final String FLOAT_TYPE = "float(";
    private static final String DOUBLE_TYPE = "double(";
    private static final String BOOLEAN_TYPE = "boolean(";
    private static final String CLASSPATH_TYPE = "classpath(";
    private static final String FILE_TYPE = "file(";
    private static final String MAP_TYPE = "map(";
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
            final ConfigReader reader;            
            try {
                reader = new ConfigReader(p);
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
                        try {
                            createFlow(f, new ConfigReader(prefix + f));
                        } catch (IOException e) {
                            log.error("Ignored {} - {}", f, e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("Unable to load Event Scripts from {} - {}", p, e.getMessage());
            }
        }
        List<String> flows = Flows.getAllFlows();
        Collections.sort(flows);
        for (String f : flows) {
            log.info("Loaded {}", f);
        }
        log.info("Event scripts deployed: {}", flows.size());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void createFlow(String name, ConfigReader flow) {
        log.info("Parsing {}", name);
        Utility util = Utility.getInstance();
        Object id = flow.get("flow.id");
        Object description = flow.get("flow.description");
        Object timeToLive = flow.get("flow.ttl");
        Object exceptionTask = flow.get("flow.exception");
        Object firstTask = flow.get("first.task");
        Object ext = flow.get("external.state.machine");
        /*
         * Flow description is enforced at compile time for documentation purpose.
         * It is not used in flow processing.
         */
        if (id instanceof String flowId && description instanceof String
                && timeToLive instanceof String ttl && firstTask instanceof String start) {
            if (Flows.flowExists(flowId)) {
                log.error("Skip {} - Flow '{}' already exists", name, flowId);
                return;
            }
            String unhandledException = exceptionTask instanceof String et? et : null;
            // minimum 1 second for TTL
            long ttlSeconds = Math.max(1, util.getDurationInSeconds(ttl));
            String extState = ext instanceof String es? es : null;
            Flow entry = new Flow(flowId, start, extState, ttlSeconds * 1000L, unhandledException);
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
                /*
                 * When "name" is given, it is used for event routing and the "process" can be used
                 * to point to the original composable function.
                 *
                 * When "name" is not provided, the "process" value will be used instead.
                 *
                 * Task description is enforced at compile time for documentation purpose.
                 * It is not used in flow processing.
                 */
                String taskName = flow.getProperty(TASKS+"["+i+"]."+NAME);
                String functionRoute = flow.getProperty(TASKS+"["+i+"]."+PROCESS);
                Object taskDesc = flow.get(TASKS+"["+i+"]."+DESCRIPTION);
                Object execution = flow.get(TASKS+"["+i+"]."+EXECUTION);
                String delay = flow.getProperty(TASKS+"["+i+"]."+DELAY);
                Object taskException = flow.get(TASKS+"["+i+"]."+EXCEPTION);
                String loopStatement = flow.getProperty(TASKS+"["+i+"]."+LOOP+"."+STATEMENT);
                Object loopCondition = flow.get(TASKS+"["+i+"]."+LOOP+"."+CONDITION);
                String uniqueTaskName = taskName == null? functionRoute : taskName;
                if (input instanceof List && output instanceof List && uniqueTaskName != null &&
                        taskDesc instanceof String && execution instanceof String taskExecution &&
                        validExecutionType(taskExecution)) {
                    boolean validTask = true;
                    if (uniqueTaskName.contains("://") && !uniqueTaskName.startsWith(FLOW_PROTOCOL)) {
                        log.error("{} {} in {}. Syntax is flow://{flow-name}", SKIP_INVALID_TASK, uniqueTaskName, name);
                        return;
                    }
                    if (functionRoute != null && functionRoute.contains("://") &&
                            !functionRoute.startsWith(FLOW_PROTOCOL)) {
                        log.error("{} process={} in {}. Syntax is flow://{flow-name}",
                                SKIP_INVALID_TASK, functionRoute, name);
                        return;
                    }
                    if (uniqueTaskName.startsWith(FLOW_PROTOCOL) && functionRoute != null &&
                            !functionRoute.startsWith(FLOW_PROTOCOL)) {
                        log.error("{} process={} in {}. process tag not allowed when name is a sub-flow",
                                    SKIP_INVALID_TASK, uniqueTaskName, name);
                        return;
                    }
                    Task task = new Task(uniqueTaskName, functionRoute, taskExecution);
                    if (delay != null && !delay.isEmpty()) {
                        if (delay.endsWith("ms")) {
                            // the "ms" suffix is used for documentation purpose only
                            delay = delay.substring(0, delay.length() - 2).trim();
                        }
                        if (util.isNumeric(delay)) {
                            // delay must be positive
                            long n = Math.max(1, util.str2long(delay));
                            if (n < entry.ttl) {
                                task.setDelay(n);
                            } else {
                                log.error("{} {} in {}. delay must be less than TTL",
                                        SKIP_INVALID_TASK, uniqueTaskName, name);
                                return;
                            }
                        } else {
                            List<String> dParts = util.split(delay, ".");
                            if (dParts.size() > 1 && delay.startsWith(MODEL_NAMESPACE)) {
                                task.setDelayVar(delay);
                            } else {
                                log.error("{} {} in {}. delay variable must starts with 'model.'",
                                        SKIP_INVALID_TASK, uniqueTaskName, name);
                                return;
                            }
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
                                log.error("{} {} in {}. Missing a join task", SKIP_INVALID_TASK, uniqueTaskName, name);
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
                            Object nextList = flow.get(TASKS+"["+i+"]."+NEXT, new ArrayList<>());
                            if (nextList instanceof List list) {
                                if (list.isEmpty()) {
                                    log.error("{} {} in {}. Missing a list of next tasks",
                                            SKIP_INVALID_TASK, uniqueTaskName, name);
                                    return;
                                }
                                List<String> nextTasks = new ArrayList<>();
                                list.forEach(v -> nextTasks.add(String.valueOf(v)));
                                if (nextTasks.size() > 1 &&
                                        (SEQUENTIAL.equals(execution) || PIPELINE.equals(execution))) {
                                    log.error("Invalid {} task {} in {}. Expected one next task, Actual: {}",
                                            execution, uniqueTaskName, name, nextTasks.size());
                                    return;
                                }
                                task.nextSteps.addAll(nextTasks);
                            } else {
                                log.error("{} {} in {}. 'next' should be a list",
                                        SKIP_INVALID_TASK, uniqueTaskName, name);
                                return;
                            }
                        }
                        /*
                         * A pipeline task must have at least one pipeline step
                         */
                        if (PIPELINE.equals(execution)) {
                            Object pipelineList = flow.get(TASKS+"["+i+"]."+PIPELINE, new ArrayList<>());
                            if (pipelineList instanceof List list) {
                                if (list.isEmpty()) {
                                    log.error("{} {} in {}. Missing a list of pipeline steps",
                                            SKIP_INVALID_TASK, uniqueTaskName, name);
                                    return;
                                }
                                List<String> pipelineSteps = new ArrayList<>();
                                list.forEach(v -> pipelineSteps.add(String.valueOf(v)));
                                task.pipelineSteps.addAll(pipelineSteps);
                                if (loopStatement != null) {
                                    int bracket = loopStatement.indexOf('(');
                                    if (bracket == -1) {
                                        log.error("{} {} in {}. Please check loop.statement",
                                                SKIP_INVALID_TASK, uniqueTaskName, name);
                                        return;
                                    }
                                    String type = loopStatement.substring(0, bracket).trim();
                                    if (!type.equals(FOR) && !type.equals(WHILE)) {
                                        log.error("{} {} in {}. loop.statement must be 'for' or 'while'",
                                                SKIP_INVALID_TASK, uniqueTaskName, name);
                                        return;
                                    }
                                    task.setLoopType(type);
                                    List<String> parts = util.split(loopStatement.substring(bracket+1), "(;)");
                                    if (type.equals(FOR)) {
                                        if (parts.size() < 2 || parts.size() > 3) {
                                            log.error("{} {} in {}. 'for' loop should have 2 or 3 segments",
                                                    SKIP_INVALID_TASK, uniqueTaskName, name);
                                            return;
                                        }
                                        if (parts.size() == 2) {
                                            task.comparator.addAll(getForPart2(parts.getFirst()));
                                            task.sequencer.addAll(getForPart3(parts.get(1)));
                                        }
                                        if (parts.size() == 3) {
                                            List<String> initializer = getForPart1(parts.getFirst());
                                            if (initializer.isEmpty()) {
                                                log.error("{} {} in {}. Please check for-loop initializer. " +
                                                            "e.g. 'for (model.n = 0; model.n < 3; model.n++)'",
                                                        SKIP_INVALID_TASK, uniqueTaskName, name);
                                                return;
                                            }
                                            task.init.addAll(initializer);
                                            task.comparator.addAll(getForPart2(parts.get(1)));
                                            task.sequencer.addAll(getForPart3(parts.get(2)));
                                        }
                                        if (task.comparator.isEmpty() || task.sequencer.isEmpty()) {
                                            log.error("{} {} in {}. Please check for-loop syntax. e.g. " +
                                                            "for (model.n = 0; model.n < 3; model.n++)",
                                                    SKIP_INVALID_TASK, uniqueTaskName, name);
                                            return;
                                        }
                                        if (!validForStatement(task.comparator, task.sequencer)) {
                                            log.error("{} {} in {}. 'for' loop has invalid comparator or sequencer",
                                                    SKIP_INVALID_TASK, uniqueTaskName, name);
                                            return;
                                        }
                                    } else {
                                        if (parts.size() != 1) {
                                            log.error("{} {} in {}. 'while' loop should have only one value",
                                                    SKIP_INVALID_TASK, uniqueTaskName, name);
                                            return;
                                        }
                                        String modelKey = parts.getFirst().trim();
                                        if (!modelKey.startsWith(MODEL_NAMESPACE) ||
                                                modelKey.contains("=") || modelKey.contains(" ")) {
                                            log.error("{} {} in {}. 'while' should use a model key",
                                                    SKIP_INVALID_TASK, uniqueTaskName, name);
                                            return;
                                        }
                                        task.setWhileModelKey(modelKey);
                                    }
                                }
                                if (loopCondition instanceof String oneCondition) {
                                    List<String> condition = getCondition(oneCondition);
                                    if (condition.size() == 2) {
                                        task.conditions.add(condition);
                                    } else {
                                        log.error("{} {} in {}. loop condition syntax error - {}",
                                                SKIP_INVALID_TASK, uniqueTaskName, name, loopCondition);
                                        return;
                                    }
                                }
                                if (loopCondition instanceof List multiConditions) {
                                    for (Object c: multiConditions) {
                                        List<String> condition = getCondition(String.valueOf(c));
                                        if (condition.size() == 2) {
                                            task.conditions.add(condition);
                                        } else {
                                            log.error("{} {} in {}. loop conditions syntax error - {}",
                                                    SKIP_INVALID_TASK, uniqueTaskName, name, loopCondition);
                                            return;
                                        }
                                    }
                                }
                            } else {
                                log.error("{} {} in {}. 'pipeline' should be a list",
                                        SKIP_INVALID_TASK, uniqueTaskName, name);
                                return;
                            }
                        }
                    }
                    List<String> inputList = new ArrayList<>();
                    // ensure data mapping entries are text strings
                    int inputListSize = ((List<Object>) input).size();
                    for (int j=0; j < inputListSize; j++) {
                        inputList.add(flow.getProperty(TASKS + "[" + i + "]." + INPUT + "[" + j + "]"));
                    }
                    // Support two data mapping formats and convert the 3-part syntax into two entries of 2-part syntax
                    // LHS -> RHS
                    // LHS -> model -> RHS
                    List<String> filteredInputMapping = filterDataMapping(inputList);
                    for (String line: filteredInputMapping) {
                        if (validInput(line)) {
                            int sep = line.indexOf(MAP_TO);
                            String rhs = line.substring(sep+2).trim();
                            if (rhs.startsWith(INPUT_NAMESPACE) || rhs.equals(INPUT)) {
                                log.warn("Task {} in {} uses input namespace in right-hand-side - {}", uniqueTaskName, name, line);
                            }
                            task.input.add(line);
                        } else {
                            log.error("Skip invalid task {} in {} that has invalid input mapping - {}", uniqueTaskName, name, line);
                            validTask = false;
                        }
                    }
                    boolean isDecisionTask = DECISION.equals(execution);
                    List<String> outputList = new ArrayList<>();
                    // ensure data mapping entries are text strings
                    int outputListSize = ((List<Object>) output).size();
                    for (int j=0; j < outputListSize; j++) {
                        outputList.add(flow.getProperty(TASKS + "[" + i + "]." + OUTPUT + "[" + j + "]"));
                    }
                    // Support two data mapping formats and convert the 3-part syntax into two entries of 2-part syntax
                    // LHS -> RHS
                    // LHS -> model -> RHS
                    List<String> filteredOutputMapping = filterDataMapping(outputList);
                    for (String line: filteredOutputMapping) {
                        if (validOutput(line, isDecisionTask)) {
                            task.output.add(line);
                        } else {
                            log.error("Skip invalid task {} in {} that has invalid output mapping - {}", uniqueTaskName, name, line);
                            validTask = false;
                        }
                    }
                    if (isDecisionTask && task.nextSteps.size() < 2) {
                        log.error("Decision task {} in {} must have at least 2 next tasks", uniqueTaskName, name);
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
                // final validation pass to check if the flow is missing external.state.machine
                boolean extFound = false;
                boolean incomplete = false;
                for (String t: entry.tasks.keySet()) {
                    Task task = entry.tasks.get(t);
                    if (hasExternalState(task.input) || hasExternalState(task.output)) {
                        extFound = true;
                        break;
                    }
                }
                for (String t: entry.tasks.keySet()) {
                    Task task = entry.tasks.get(t);
                    if (hasIncompleteMapping(task.input) || hasIncompleteMapping(task.output)) {
                        incomplete = true;
                        break;
                    }
                }
                if (extFound && entry.externalStateMachine == null) {
                    log.error("Unable to parse {} - flow is missing external.state.machine", name);
                } else if (incomplete) {
                    log.error("Unable to parse {} - flow has invalid data mappings", name);
                } else {
                    Flows.addFlow(entry);
                }
            } else {
                log.error("Unable to parse {} - flow must have at least one end task", name);
            }
        } else {
            log.error("Unable to parse {} - check flow.id, flow.description, flow.ttl, first.task", name);
        }
    }

    private boolean hasExternalState(List<String> mapping) {
        for (String m: mapping) {
            int sep = m.indexOf(MAP_TO);
            if (sep != -1) {
                String rhs = m.substring(sep+2).trim();
                if (rhs.startsWith(EXT_NAMESPACE) && !EXT_NAMESPACE.equals(rhs)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasIncompleteMapping(List<String> mapping) {
        for (String m: mapping) {
            int sep = m.indexOf(MAP_TO);
            if (sep != -1) {
                String lhs = m.substring(0, sep).trim();
                String rhs = m.substring(sep+2).trim();
                if (lhs.endsWith(".") || rhs.endsWith(".") || rhs.endsWith(":")) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private List<String> getCondition(String statement) {
        Utility util = Utility.getInstance();
        List<String> result = new ArrayList<>();
        List<String> parts = util.split(statement, " ()");
        if (parts.size() == 3 && parts.getFirst().equals("if") && parts.get(1).startsWith(MODEL_NAMESPACE) &&
            (parts.get(2).equals(CONTINUE) || parts.get(2).equals(BREAK)) ) {
            result.add(parts.get(1));
            result.add(parts.get(2));
        }
        return result;
    }

    private List<String> getForPart1(String text) {
        Utility util = Utility.getInstance();
        // add spaces for easy parsing
        List<String> result = new ArrayList<>();
        List<String> parts = util.split(text.replace("=", " = "), " ");
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
        List<String> result = new ArrayList<>();
        List<String> parts = util.split(s, " ");
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

    private List<String> filterDataMapping(List<String> entries) {
        List<String> result = new ArrayList<>();
        for (String line: entries) {
            List<String> parts = new ArrayList<>();
            var entry = line;
            while (entry.contains(MAP_TO)) {
                var sep = entry.indexOf(MAP_TO);
                var first = entry.substring(0, sep).trim();
                parts.add(first);
                entry = entry.substring(sep+2).trim();
            }
            parts.add(entry);
            if (parts.size() == 2) {
                result.add(filterMapping(parts.getFirst() + " " + MAP_TO + " " + parts.get(1)));
            } else if (parts.size() == 3) {
                if (parts.get(1).startsWith(MODEL_NAMESPACE) || parts.get(1).startsWith(NEGATE_MODEL)) {
                    result.add(filterMapping(parts.getFirst() + " " + MAP_TO + " " + parts.get(1)));
                    result.add(removeNegate(parts.get(1)) + " " + MAP_TO + " " + parts.get(2));
                } else {
                    result.add("3-part data mapping must have model variable as the middle part");
                }
            } else {
                result.add("Syntax must be (LHS -> RHS) or (LHS -> model.variable -> RHS)");
            }
        }
        return result;
    }

    private String filterMapping(String mapping) {
        var text = mapping.trim();
        int sep = text.indexOf(MAP_TO);
        if (sep == -1) {
            return mapping;
        }
        var lhs = text.substring(0, sep).trim();
        var rhs = text.substring(sep+2).trim();
        // Detect and reformat "negate" of a model value in LHS and RHS
        // !model.key becomes model.key:! for consistent processing by TaskExecutor
        if (lhs.startsWith(NEGATE_MODEL)) {
            lhs = normalizedTypeMapping(lhs);
        }
        if (rhs.startsWith(NEGATE_MODEL)) {
            rhs = normalizedTypeMapping(rhs);
        }
        return lhs + " " + MAP_TO + " " + rhs;
    }

    private String normalizedTypeMapping(String negate) {
        // convert the leading negate character with a trailing colon format
        // e.g. !model.something becomes model.something:!
        return (negate.contains(":")? negate.substring(1, negate.indexOf(':')) : negate.substring(1)) + ":!";
    }

    private String removeNegate(String negate) {
        var step1 = negate.startsWith("!")? negate.substring(1) : negate;
        return step1.contains(":")? step1.substring(0, step1.indexOf(':')) : step1;
    }

    private boolean validInput(String input) {
        int sep = input.indexOf(MAP_TO);
        if (sep > 0) {
            String lhs = input.substring(0, sep).trim();
            String rhs = input.substring(sep+2).trim();
            if (validModel(lhs) && validModel(rhs) && !lhs.equals(rhs)) {
                if (lhs.equals(INPUT) || lhs.startsWith(INPUT_NAMESPACE) ||
                        lhs.startsWith(MODEL_NAMESPACE) || lhs.startsWith(ERROR_NAMESPACE)) {
                    return true;
                } else if (lhs.startsWith(MAP_TYPE) && lhs.endsWith(CLOSE_BRACKET)) {
                    return validKeyValues(lhs);
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

    private boolean validModel(String key) {
        Utility util = Utility.getInstance();
        List<String> parts = util.split(key, "!: ()");
        if (parts.isEmpty()) {
            return false;
        } else {
            // "model" alone to access the whole model dataset is not allowed
            if (MODEL.equals(parts.getFirst())) {
                return false;
            }
            // model.parent to access the whole parent namespace is not allowed
            if (parts.getFirst().startsWith(MODEL_NAMESPACE)) {
                List<String> segments = util.split(parts.getFirst(), ".");
                return segments.size() != 1 && (segments.size() != 2 || !PARENT.equals(segments.get(1)));
            }
            return true;
        }
    }

    private boolean validKeyValues(String text) {
        int last = text.lastIndexOf(CLOSE_BRACKET);
        String ref = text.substring(MAP_TYPE.length(), last).trim();
        if (ref.contains("=") || ref.contains(",")) {
            List<String> keyValues = Utility.getInstance().split(ref, ",");
            Set<String> keys = new HashSet<>();
            for (String kv: keyValues) {
                int eq = kv.indexOf('=');
                String k = eq == -1? kv.trim() : kv.substring(0, eq).trim();
                if (k.isEmpty()) {
                    return false;
                } else {
                    keys.add(k);
                }
            }
            return keys.size() == keyValues.size();
        } else {
            return !ref.isEmpty();
        }
    }

    private boolean validOutput(String output, boolean isDecision) {
        int sep = output.indexOf(MAP_TO);
        if (sep > 0) {
            String lhs = output.substring(0, sep).trim();
            String rhs = output.substring(sep+2).trim();
            if (validModel(lhs) && validModel(rhs) && !lhs.equals(rhs)) {
                return validOutputLhs(lhs) && validOutputRhs(rhs, isDecision);
            }
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
        } else if (lhs.startsWith(MAP_TYPE) && lhs.endsWith(CLOSE_BRACKET)) {
            return validKeyValues(lhs);
        } else {
            return (lhs.startsWith(TEXT_TYPE) ||
                    lhs.startsWith(FILE_TYPE) || lhs.startsWith(CLASSPATH_TYPE) ||
                    lhs.startsWith(INTEGER_TYPE) || lhs.startsWith(LONG_TYPE) ||
                    lhs.startsWith(FLOAT_TYPE) || lhs.startsWith(DOUBLE_TYPE) ||
                    lhs.startsWith(BOOLEAN_TYPE)) && lhs.endsWith(CLOSE_BRACKET);
        }
    }

    private boolean validOutputRhs(String rhs, boolean isDecision) {
        return (rhs.equals(DECISION) && isDecision) || rhs.startsWith(FILE_TYPE) ||
                rhs.startsWith(OUTPUT_NAMESPACE) || rhs.startsWith(MODEL_NAMESPACE) ||
                rhs.startsWith(EXT_NAMESPACE);
    }
}
