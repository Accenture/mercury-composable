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
    private static final String SOURCE = "source";
    private static final String DATA_TYPE = "datatype";
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
    private static final String SPACED_MAP_TO = " -> ";
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
    private static final String INVALID_TASK = "invalid task";
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
                Object allFlows = reader.get("flows");
                if (allFlows instanceof List<?> flows) {
                    List<String> ordered = getUniqueFlows(reader, flows);
                    for (String f: ordered) {
                        createOneFlow(prefix, f);
                    }
                }
            } catch (IllegalArgumentException e) {
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

    private List<String> getUniqueFlows(ConfigReader reader, List<?> flows) {
        Set<String> uniqueFlows = new HashSet<>();
        for (int i = 0; i < flows.size(); i++) {
            String f = reader.getProperty("flows[" + i + "]");
            if (f.endsWith(".yml") || f.endsWith(".yaml")) {
                uniqueFlows.add(f);
            } else {
                log.error("Ignored {} because it does not have .yml or .yaml file extension", f);
            }
        }
        var result = new ArrayList<>(uniqueFlows);
        Collections.sort(result);
        return result;
    }

    private void createOneFlow(String prefix, String name) {
        try {
            createFlow(name, new ConfigReader(prefix + name));
        } catch (IllegalArgumentException e) {
            log.error("Unable to parse {} - {}", name, e.getMessage());
        }
    }

    private void createFlow(String name, ConfigReader reader) {
        log.info("Parsing {}", name);
        Utility util = Utility.getInstance();
        Object id = reader.get("flow.id");
        Object description = reader.get("flow.description");
        Object timeToLive = reader.get("flow.ttl");
        Object exceptionTask = reader.get("flow.exception");
        Object firstTask = reader.get("first.task");
        Object ext = reader.get("external.state.machine");
        /*
         * Flow description is enforced at compile time for documentation purpose.
         * It is not used in flow processing.
         */
        if (id instanceof String flowId && description instanceof String
                && timeToLive instanceof String ttl && firstTask instanceof String start) {
            if (Flows.flowExists(flowId)) {
                throw new IllegalArgumentException(String.format("Flow '%s' already exists", flowId));
            }
            String unhandledException = exceptionTask instanceof String et? et : null;
            // minimum 1 second for TTL
            long ttlSeconds = Math.max(1, util.getDurationInSeconds(ttl));
            String extState = ext instanceof String es? es : null;
            Flow entry = new Flow(flowId, start, extState, ttlSeconds * 1000L, unhandledException);
            Object taskList = reader.get(TASKS);
            int taskCount = taskList instanceof List<?> tList? tList.size() : 0;
            if (taskCount == 0) {
                throw new IllegalArgumentException("'tasks' section is empty or invalid");
            }
            validateEntry(name, entry, reader, taskCount);
        } else {
            throw new IllegalArgumentException("check flow.id, flow.description, flow.ttl, first.task");
        }
    }

    private void validateEntry(String name, Flow entry, ConfigReader reader, int taskCount) {
        boolean endTaskFound = false;
        for (int i=0; i < taskCount; i++) {
            var md = getConfigMetadata(name, reader, i);
            Task task = new Task(md.uniqueTaskName, md.functionRoute, md.execution);
            validateDelayParameter(md, entry, task);
            if (md.taskException instanceof String te) {
                task.setExceptionTask(te);
            }
            if (END.equals(md.execution)) {
                endTaskFound = true;
            } else {
                setForkJoinIfAny(task, md, reader, i);
                setNonSinkTaskIfAny(task, md, reader, i);
                setPipelineIfAny(task, md, reader, i);
            }
            validateInputOutput(name, entry, task, md, reader, i);
        }
        if (endTaskFound) {
            finalizeEntry(entry);
        } else {
            throw new IllegalArgumentException("flow must have at least one end task");
        }
    }

    private void validateInputOutput(String name, Flow entry, Task task, FlowConfigMetadata md,
                                     ConfigReader reader, int i) {
        List<String> inputList = new ArrayList<>();
        // ensure data mapping entries are text strings
        for (int j = 0; j < md.input.size(); j++) {
            inputList.add(reader.getProperty(TASKS + "[" + i + "]." + INPUT + "[" + j + "]"));
        }
        List<String> outputList = new ArrayList<>();
        // ensure data mapping entries are text strings
        for (int j = 0; j < md.output.size(); j++) {
            outputList.add(reader.getProperty(TASKS + "[" + i + "]." + OUTPUT + "[" + j + "]"));
        }
        if (validInputMapping(name, inputList, task, md) && validOutputMapping(name, outputList, task, md)) {
            entry.addTask(task);
        }
    }

    private boolean validOutputMapping(String name, List<String> outputList, Task task, FlowConfigMetadata md) {
        boolean isDecisionTask = DECISION.equals(md.execution);
        List<String> filteredOutputMapping = filterDataMapping(outputList);
        for (String line : filteredOutputMapping) {
            if (validOutput(line, isDecisionTask)) {
                task.output.add(line);
            } else {
                log.error("Skip invalid task {} in {} that has invalid output mapping - {}",
                        md.uniqueTaskName, name, line);
                return false;
            }
        }
        if (isDecisionTask && task.nextSteps.size() < 2) {
            log.error("Decision task {} in {} must have at least 2 next tasks", md.uniqueTaskName, name);
            return false;
        }
        return true;
    }

    private boolean validInputMapping(String name, List<String> inputList, Task task, FlowConfigMetadata md) {
        List<String> filteredInputMapping = filterDataMapping(inputList);
        for (String line : filteredInputMapping) {
            if (validInput(line)) {
                int sep = line.lastIndexOf(MAP_TO);
                String rhs = line.substring(sep + 2).trim();
                if (rhs.startsWith(INPUT_NAMESPACE) || rhs.equals(INPUT)) {
                    log.warn("Task {} in {} uses input namespace in right-hand-side - {}",
                            md.uniqueTaskName, name, line);
                }
                task.input.add(line);
            } else {
                log.error("Skip invalid task {} in {} that has invalid input mapping - {}",
                        md.uniqueTaskName, name, line);
                return false;
            }
        }
        return true;
    }

    private void finalizeEntry(Flow entry) {
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
            throw new IllegalArgumentException("flow is missing external.state.machine");
        } else if (incomplete) {
            throw new IllegalArgumentException("flow has invalid data mappings");
        } else {
            Flows.addFlow(entry);
        }
    }

    private void setPipelineIfAny(Task task, FlowConfigMetadata md, ConfigReader reader, int i) {
        if (PIPELINE.equals(md.execution)) {
            Object pipelineList = reader.get(TASKS + "[" + i + "]." + PIPELINE, new ArrayList<>());
            if (pipelineList instanceof List<?> list) {
                if (list.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("%s %s. Missing a list of pipeline steps",
                                    INVALID_TASK, md.uniqueTaskName));
                }
                List<String> pipelineSteps = new ArrayList<>();
                list.forEach(v -> pipelineSteps.add(String.valueOf(v)));
                task.pipelineSteps.addAll(pipelineSteps);
                handleLoopStatementIfAny(task, md);
                handleLoopConditions(task, md);
            } else {
                throw new IllegalArgumentException(
                        String.format("%s %s. 'pipeline' should be a list",
                                INVALID_TASK, md.uniqueTaskName));
            }
        }
    }

    private void handleLoopStatementIfAny(Task task, FlowConfigMetadata md) {
        if (md.loopStatement != null) {
            var util = Utility.getInstance();
            int bracket = md.loopStatement.indexOf('(');
            if (bracket == -1) {
                throw new IllegalArgumentException(
                        String.format("%s %s. Please check loop.statement",
                                INVALID_TASK, md.uniqueTaskName));
            }
            String type = md.loopStatement.substring(0, bracket).trim();
            if (!type.equals(FOR) && !type.equals(WHILE)) {
                throw new IllegalArgumentException(
                        String.format("%s %s. loop.statement must be 'for' or 'while'",
                                INVALID_TASK, md.uniqueTaskName));
            }
            task.setLoopType(type);
            List<String> parts = util.split(md.loopStatement.substring(bracket + 1), "(;)");
            if (type.equals(FOR)) {
                handleForLoop(task, md, parts);
            } else {
                handleWhileLoop(task, md, parts);
            }
        }
    }

    private void handleLoopConditions(Task task, FlowConfigMetadata md) {
        if (md.loopCondition instanceof String oneCondition) {
            List<String> condition = getCondition(oneCondition);
            if (condition.size() == 2) {
                task.conditions.add(condition);
            } else {
                throw new IllegalArgumentException(
                        String.format("%s %s. loop condition syntax error - %s",
                                INVALID_TASK, md.uniqueTaskName, md.loopCondition));
            }
        }
        if (md.loopCondition instanceof List<?> multiConditions) {
            for (Object c : multiConditions) {
                List<String> condition = getCondition(String.valueOf(c));
                if (condition.size() == 2) {
                    task.conditions.add(condition);
                } else {
                    throw new IllegalArgumentException(
                            String.format("%s %s. loop conditions syntax error - %s",
                                    INVALID_TASK, md.uniqueTaskName, md.loopCondition));
                }
            }
        }
    }

    private void handleWhileLoop(Task task, FlowConfigMetadata md, List<String> parts) {
        if (parts.size() != 1) {
            throw new IllegalArgumentException(
                    String.format("%s %s. 'while' loop should have only one value",
                            INVALID_TASK, md.uniqueTaskName));
        }
        String modelKey = parts.getFirst().trim();
        if (!modelKey.startsWith(MODEL_NAMESPACE) ||
                modelKey.contains("=") || modelKey.contains(" ")) {
            throw new IllegalArgumentException(
                    String.format("%s %s. 'while' should use a model key",
                            INVALID_TASK, md.uniqueTaskName));
        }
        task.setWhileModelKey(modelKey);
    }

    private void handleForLoop(Task task, FlowConfigMetadata md, List<String> parts) {
        if (parts.size() < 2 || parts.size() > 3) {
            throw new IllegalArgumentException(
                    String.format("%s %s. 'for' loop should have 2 or 3 segments",
                            INVALID_TASK, md.uniqueTaskName));
        }
        if (parts.size() == 2) {
            task.comparator.addAll(getForPart2(parts.getFirst()));
            task.sequencer.addAll(getForPart3(parts.get(1)));
        }
        if (parts.size() == 3) {
            List<String> initializer = getForPart1(parts.getFirst());
            if (initializer.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("%s %s. check for-loop initializer. " +
                                        "e.g. 'for (model.n = 0; model.n < 3; model.n++)'",
                                INVALID_TASK, md.uniqueTaskName));
            }
            task.init.addAll(initializer);
            task.comparator.addAll(getForPart2(parts.get(1)));
            task.sequencer.addAll(getForPart3(parts.get(2)));
        }
        if (task.comparator.isEmpty() || task.sequencer.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("%s %s. check for-loop syntax. " +
                                    "e.g. 'for (model.n = 0; model.n < 3; model.n++)'",
                            INVALID_TASK, md.uniqueTaskName));
        }
        if (!validForStatement(task.comparator, task.sequencer)) {
            throw new IllegalArgumentException(
                    String.format("%s %s. 'for' loop has invalid comparator or sequencer",
                            INVALID_TASK, md.uniqueTaskName));
        }
    }

    private void setForkJoinIfAny(Task task, FlowConfigMetadata md, ConfigReader reader, int i) {
        if (FORK.equals(md.execution)) {
            Object join = reader.get(TASKS + "[" + i + "]." + JOIN);
            if (join instanceof String jt) {
                task.setJoinTask(jt);
            } else {
                throw new IllegalArgumentException(String.format("%s %s. Missing a join task",
                        INVALID_TASK, md.uniqueTaskName));
            }
        }
    }

    private void setNonSinkTaskIfAny(Task task, FlowConfigMetadata md, ConfigReader reader, int i) {
        if (!SINK.equals(md.execution)) {
            Object nextList = reader.get(TASKS + "[" + i + "]." + NEXT, new ArrayList<>());
            if (nextList instanceof List<?> list) {
                if (list.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("Invalid task %s. Missing a list of next tasks", md.uniqueTaskName));
                }
                List<String> nextTasks = new ArrayList<>();
                list.forEach(v -> nextTasks.add(String.valueOf(v)));
                if (nextTasks.size() > 1 &&
                        (SEQUENTIAL.equals(md.execution) || PIPELINE.equals(md.execution))) {
                    throw new IllegalArgumentException(
                            String.format("Invalid %s task %s. Expected one next task, Actual: %s",
                                    md.execution, md.uniqueTaskName, nextTasks.size()));
                }
                validateSourceModelKey(task, md, nextTasks);
                task.nextSteps.addAll(nextTasks);
            } else {
                throw new IllegalArgumentException(
                        String.format("%s %s. 'next' should be a list", INVALID_TASK, md.uniqueTaskName));
            }
        }
    }

    private void validateSourceModelKey(Task task, FlowConfigMetadata md, List<String> nextTasks) {
        if (nextTasks.size() > 1 &&
                (md.source != null && !md.source.isEmpty())) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s task %s. " +
                                    "Expected one next task if dynamic model source is used, Actual: %s",
                            md.execution, md.uniqueTaskName, nextTasks.size()));
        }
        if (md.source != null && !md.source.isEmpty()) {
            if (md.source.startsWith(MODEL_NAMESPACE) && !md.source.endsWith(".")) {
                task.setSourceModelKey(md.source);
            } else {
                throw new IllegalArgumentException(
                        String.format("Invalid %s task %s. " +
                                        "Source must start with model namespace, Actual: %s",
                                md.execution, md.uniqueTaskName, md.source));
            }
        }
    }

    private static FlowConfigMetadata getConfigMetadata(String name, ConfigReader reader, int i) {
        var md = new FlowConfigMetadata(name, reader, i);
        if (md.uniqueTaskName.contains("://") && !md.uniqueTaskName.startsWith(FLOW_PROTOCOL)) {
            throw new IllegalArgumentException(String.format("%s name=%s. Syntax is flow://{flow-name}",
                    INVALID_TASK, md.uniqueTaskName));
        }
        if (md.functionRoute != null && md.functionRoute.contains("://") &&
                !md.functionRoute.startsWith(FLOW_PROTOCOL)) {
            throw new IllegalArgumentException(String.format("%s process=%s. Syntax is flow://{flow-name}",
                    INVALID_TASK, md.functionRoute));
        }
        if (md.uniqueTaskName.startsWith(FLOW_PROTOCOL) && md.functionRoute != null &&
                !md.functionRoute.startsWith(FLOW_PROTOCOL)) {
            throw new IllegalArgumentException(
                    String.format("%s process=%s. process tag not allowed when name is a sub-flow",
                            INVALID_TASK, md.uniqueTaskName));
        }
        return md;
    }

    private void validateDelayParameter(FlowConfigMetadata md, Flow entry, Task task) {
        var util = Utility.getInstance();
        if (md.delay != null && !md.delay.isEmpty()) {
            if (md.delay.endsWith("ms")) {
                // the "ms" suffix is used for documentation purpose only
                md.delay = md.delay.substring(0, md.delay.length() - 2).trim();
            }
            if (util.isNumeric(md.delay)) {
                setNumericDelay(md, entry, task);
            } else {
                setDelayFromModelVar(md, task);
            }
        }
    }

    private void setNumericDelay(FlowConfigMetadata md, Flow entry, Task task) {
        var util = Utility.getInstance();
        // delay must be positive
        long n = Math.max(1, util.str2long(md.delay));
        if (n < entry.ttl) {
            task.setDelay(n);
        } else {
            throw new IllegalArgumentException(String.format("%s %s. delay must be less than TTL",
                    INVALID_TASK, md.uniqueTaskName));
        }
    }

    private void setDelayFromModelVar(FlowConfigMetadata md, Task task) {
        var util = Utility.getInstance();
        List<String> dParts = util.split(md.delay, ".");
        if (dParts.size() > 1 && md.delay.startsWith(MODEL_NAMESPACE)) {
            task.setDelayVar(md.delay);
        } else {
            throw new IllegalArgumentException(
                    String.format("%s %s. delay variable must starts with 'model.'",
                            INVALID_TASK, md.uniqueTaskName));
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

    private List<String> filterDataMapping(List<String> entries) {
        List<String> result = new ArrayList<>();
        for (String line: entries) {
            var entry = line.trim();
            if (entry.startsWith(TEXT_TYPE)) {
                // text constant supports 2-part mapping format only because text constant can include any characters
                result.add(filterMapping(entry));
            } else {
                List<String> parts = new ArrayList<>();
                while (entry.contains(MAP_TO)) {
                    var sep = entry.indexOf(MAP_TO);
                    var first = entry.substring(0, sep).trim();
                    parts.add(first);
                    entry = entry.substring(sep + 2).trim();
                }
                parts.add(entry);
                if (parts.size() == 2) {
                    result.add(filterMapping(parts.getFirst() + SPACED_MAP_TO + parts.get(1)));
                } else if (parts.size() == 3) {
                    handleThreePartMapping(parts, result);
                } else {
                    result.add("Syntax must be (LHS -> RHS) or (LHS -> model.variable -> RHS)");
                }
            }
        }
        return result;
    }

    private void handleThreePartMapping(List<String> parts, List<String> result) {
        /*
         * 3-part mapping format handling:
         * 1. The middle part must be a model variable
         * 2. It will decompose into two entries of 2-part mappings
         * 3. Any type information of the LHS of the second entry will be dropped
         *
         * For example,
         *
         * BEFORE
         * - 'boolean(true) -> !model.bool -> negate_value'
         * AFTER
         * - 'boolean(true) -> model.bool:!'
         * - 'model.bool -> negate_value'
         */
        if (parts.get(1).startsWith(MODEL_NAMESPACE) || parts.get(1).startsWith(NEGATE_MODEL)) {
            result.add(filterMapping(parts.getFirst() + SPACED_MAP_TO + parts.get(1)));
            var secondLhs = trimTypeQualifier(parts.get(1));
            result.add(filterMapping(secondLhs + SPACED_MAP_TO + parts.get(2)));
        } else {
            result.add("3-part data mapping must have model variable as the middle part");
        }
    }

    private String filterMapping(String mapping) {
        var text = mapping.trim();
        int sep = text.lastIndexOf(MAP_TO);
        if (sep == -1) {
            return mapping;
        }
        var lhs = text.substring(0, sep).trim();
        var rhs = text.substring(sep+2).trim();
        if (lhs.startsWith(NEGATE_MODEL)) {
            lhs = normalizedNegateTypeMapping(lhs);
        }
        if (rhs.startsWith(NEGATE_MODEL)) {
            rhs = normalizedNegateTypeMapping(rhs);
        }
        return lhs + SPACED_MAP_TO + rhs;
    }

    private String normalizedNegateTypeMapping(String negate) {
        /*
         * Convert convenient negate (!) to internal format (:!)
         * e.g. !model.something becomes model.something:!
         */
        return (negate.contains(":")? negate.substring(1, negate.indexOf(':')) : negate.substring(1)) + ":!";
    }

    private String trimTypeQualifier(String lhs) {
        var step1 = lhs.startsWith("!")? lhs.substring(1) : lhs;
        return step1.contains(":")? step1.substring(0, step1.indexOf(':')) : step1;
    }

    private boolean validInput(String input) {
        int sep = input.lastIndexOf(MAP_TO);
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
        int sep = output.lastIndexOf(MAP_TO);
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
        if (lhs.equals(INPUT) || lhs.startsWith(INPUT_NAMESPACE) ||
                lhs.startsWith(MODEL_NAMESPACE) || lhs.equals(DATA_TYPE) ||
                lhs.equals(RESULT) || lhs.startsWith(RESULT_NAMESPACE) ||
                lhs.equals(STATUS) ||
                lhs.equals(HEADER) || lhs.startsWith(HEADER_NAMESPACE)) {
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

    private static class FlowConfigMetadata {
        String name;
        List<?> input;
        List<?> output;
        String taskName;
        String functionRoute;
        String taskDesc;
        String execution;
        String delay;
        String taskException;
        String loopStatement;
        Object loopCondition;
        String uniqueTaskName;
        String source;

        FlowConfigMetadata(String name, ConfigReader reader, int i) {
            this.name = name;
            Object vInput = reader.get(TASKS+"["+i+"]."+INPUT, new ArrayList<>());
            Object vOutput = reader.get(TASKS+"["+i+"]."+OUTPUT, new ArrayList<>());
            String vTaskName = reader.getProperty(TASKS+"["+i+"]."+NAME);
            String vFunctionRoute = reader.getProperty(TASKS+"["+i+"]."+PROCESS);
            Object vTaskDesc = reader.get(TASKS+"["+i+"]."+DESCRIPTION);
            Object vExecution = reader.get(TASKS+"["+i+"]."+EXECUTION);
            String vDelay = reader.getProperty(TASKS+"["+i+"]."+DELAY);
            Object vTaskException = reader.get(TASKS+"["+i+"]."+EXCEPTION);
            String vLoopStatement = reader.getProperty(TASKS+"["+i+"]."+LOOP+"."+STATEMENT);
            Object vLoopCondition = reader.get(TASKS+"["+i+"]."+LOOP+"."+CONDITION);
            String vUniqueTaskName = vTaskName == null? vFunctionRoute : vTaskName;
            String vSource = reader.getProperty(TASKS+"["+i+"]."+SOURCE);
            isValidTaskConfiguration(vInput, vOutput, vUniqueTaskName, vTaskDesc, vExecution, i);
            this.input = (List<?>) vInput;
            this.output = (List<?>) vOutput;
            this.taskName = vTaskName;
            this.functionRoute = vFunctionRoute;
            this.taskDesc = String.valueOf(vTaskDesc);
            this.execution = String.valueOf(vExecution);
            this.delay = vDelay;
            if (vTaskException instanceof String e) {
                this.taskException = e;
            }
            this.loopStatement = vLoopStatement;
            this.loopCondition = vLoopCondition;
            this.uniqueTaskName = vUniqueTaskName;
            this.source = vSource;
        }

        private void isValidTaskConfiguration(Object input, Object output, String uniqueTaskName,
                                              Object taskDesc, Object execution, int taskIndex) {
            if (uniqueTaskName == null || uniqueTaskName.isBlank()) {
                throw new IllegalArgumentException(String.format("task[%s]. task name must not be empty", taskIndex));
            }
            if (!(input instanceof List)) {
                throw new IllegalArgumentException(String.format("task %s. input must be a list", uniqueTaskName));
            }
            if (!(output instanceof List)) {
                throw new IllegalArgumentException(String.format("task %s. output must be a list", uniqueTaskName));
            }

            if (!(taskDesc instanceof String taskDescription) || taskDescription.isBlank()) {
                throw new IllegalArgumentException(
                        String.format("task %s. description must not be empty", uniqueTaskName));
            }
            if (!(execution instanceof String taskExecution) || taskExecution.isBlank()) {
                throw new IllegalArgumentException(
                        String.format("task %s. execution type must not be empty", uniqueTaskName));
            }
            if(!validExecutionType(taskExecution)) {
                throw new IllegalArgumentException(
                        String.format("task %s. execution type '%s' must be one of %s", uniqueTaskName, taskExecution,
                                Arrays.stream(EXECUTION_TYPES).toList()));
            }
        }

        private boolean validExecutionType(String execution) {
            for (String s: EXECUTION_TYPES) {
                if (s.equals(execution)) {
                    return true;
                }
            }
            return false;
        }
    }
}
