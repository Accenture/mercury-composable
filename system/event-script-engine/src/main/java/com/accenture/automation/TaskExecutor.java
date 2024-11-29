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

import com.accenture.models.*;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ZeroTracing
@EventInterceptor
@PreLoad(route = "task.executor", envInstances = "task.executor.instances", instances = 200)
public class TaskExecutor implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutor.class);
    private static final Utility util = Utility.getInstance();
    public static final String SERVICE_NAME = "task.executor";
    private static final String FIRST_TASK = "first_task";
    private static final String FLOW_ID = "flow_id";
    private static final String FLOW_PROTOCOL = "flow://";
    private static final String TYPE = "type";
    private static final String PUT = "put";
    private static final String KEY = "key";
    private static final String REMOVE = "remove";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String INPUT = "input";
    private static final String OUTPUT_STATUS = "output.status";
    private static final String OUTPUT_HEADER = "output.header";
    private static final String MODEL = "model";
    private static final String RESULT = "result";
    private static final String HEADER = "header";
    private static final String CODE = "code";
    private static final String STACK_TRACE = "stack";
    private static final String DECISION = "decision";
    private static final String INPUT_NAMESPACE = "input.";
    private static final String MODEL_NAMESPACE = "model.";
    private static final String RESULT_NAMESPACE = "result.";
    private static final String ERROR_NAMESPACE = "error.";
    private static final String EXT_NAMESPACE = "ext:";
    private static final String INPUT_HEADER_NAMESPACE = "input.header.";
    private static final String HEADER_NAMESPACE = "header.";
    private static final String TEXT_TYPE = "text(";
    private static final String INTEGER_TYPE = "int(";
    private static final String LONG_TYPE = "long(";
    private static final String FLOAT_TYPE = "float(";
    private static final String DOUBLE_TYPE = "double(";
    private static final String BOOLEAN_TYPE = "boolean(";
    private static final String CLASSPATH_TYPE = "classpath(";
    private static final String FILE_TYPE = "file(";
    private static final String CLOSE_BRACKET = ")";
    private static final String TEXT_FILE = "text:";
    private static final String BINARY_FILE = "binary:";
    private static final String MAP_TO = "->";
    private static final String ALL = "*";
    private static final String END = "end";
    private static final String TRUE = "true";
    private static final String RESPONSE = "response";
    private static final String SEQUENTIAL = "sequential";
    private static final String PARALLEL = "parallel";
    private static final String FORK = "fork";
    private static final String JOIN = "join";
    private static final String PIPELINE = "pipeline";
    private static final String SERVICE_AT = "Service ";
    private static final String TIMEOUT = "timeout";
    private static final String FOR = "for";
    private static final String WHILE = "while";
    private static final String CONTINUE = "continue";
    private static final String BREAK = "break";
    private static final String INCREMENT = "++";
    private static final String DECREMENT = "--";
    private static final String TEXT_SUFFIX = "text";
    private static final String BINARY_SUFFIX = "binary";
    private static final String B64_SUFFIX = "b64";
    private static final String SUBSTRING_SUFFIX = "substring(";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) throws IOException {
        String compositeCid = event.getCorrelationId();
        if (compositeCid == null) {
            log.error("Event {} dropped because there is no correlation ID", event.getId());
            return null;
        }
        int sep = compositeCid.indexOf('#');
        final String cid;
        final int seq;
        if (sep > 0) {
            cid = compositeCid.substring(0, sep);
            seq = util.str2int(compositeCid.substring(sep+1));
        } else {
            cid = compositeCid;
            seq = -1;
        }
        FlowInstance flowInstance = Flows.getFlowInstance(cid);
        if (flowInstance == null) {
            log.warn("Flow instance {} is invalid or expired", cid);
            return null;
        }
        String flowName = flowInstance.getFlow().id;
        if (headers.containsKey(TIMEOUT)) {
            log.warn("Flow {}:{} expired", flowName, flowInstance.id);
            abortFlow(flowInstance, 408, "Flow timeout for "+ flowInstance.getFlow().ttl+" ms");
            return null;
        }
        String firstTask = headers.get(FIRST_TASK);
        if (firstTask != null) {
            executeTask(flowInstance, firstTask);
        } else {
            // handle callback from a task
            String from = event.getFrom();
            if (from == null) {
                log.error("Unable to process callback {}:{} - task does not provide 'from' address", flowName, cid);
                return null;
            }
            String caller = from.contains("@")? from.substring(0, from.indexOf('@')) : from;
            Task task = flowInstance.getFlow().tasks.get(caller);
            if (task == null) {
                log.error("Unable to process callback {}:{} - missing task in {}", flowName, cid, caller);
                return null;
            }
            int statusCode = event.getStatus();
            Throwable ex = event.getException();
            if (statusCode >= 400 || ex != null) {
                if (seq > 0) {
                    if (task.getExceptionTask() != null) {
                        // Clear this specific pipeline queue when task has its own exception handler
                        flowInstance.pipeMap.remove(seq);
                    } else {
                        /*
                         * Clear all pipeline queues when task does not have its own exception handler.
                         * System will route the exception to the generic exception handler.
                         */
                        flowInstance.pipeMap.clear();
                    }
                }
                String handler = task.getExceptionTask() != null? task.getExceptionTask() : flowInstance.getFlow().exception;
                if (handler != null) {
                    Map<String, Object> error = new HashMap<>();
                    error.put(CODE, statusCode);
                    error.put(MESSAGE, event.getRawBody());
                    if (event.getException() != null) {
                        error.put(STACK_TRACE, getStackTrace(ex));
                    }
                    executeTask(flowInstance, handler, -1, error);
                } else {
                    // when there are no task or flow exception handlers
                    abortFlow(flowInstance, statusCode, event.getError());
                }
                return null;
            }
            handleCallback(from, flowInstance, task, event, seq);
        }
        return null;
    }

    private String getStackTrace(Throwable ex) {
        try (StringWriter out = new StringWriter(); PrintWriter writer = new PrintWriter(out)) {
            ex.printStackTrace(writer);
            return out.toString();
        } catch (IOException e) {
            return ex.toString();
        }
    }

    private void abortFlow(FlowInstance flowInstance, int status, String message) throws IOException {
        if (flowInstance.isNotResponded()) {
            flowInstance.setResponded(true);
            Map<String, Object> result = new HashMap<>();
            result.put(STATUS, status);
            result.put(MESSAGE, message);
            result.put(TYPE, ERROR);
            EventEnvelope error = new EventEnvelope();
            // restore the original correlation-ID to the calling party
            error.setTo(flowInstance.replyTo).setCorrelationId(flowInstance.cid);
            error.setStatus(status).setBody(result);
            PostOffice po = new PostOffice(TaskExecutor.SERVICE_NAME,
                                            flowInstance.getTraceId(), flowInstance.getTracePath());
            po.send(error);
        }
        endFlow(flowInstance, false);
    }

    private void endFlow(FlowInstance flowInstance, boolean normal) {
        String traceId = flowInstance.getTraceId();
        String logId = traceId != null? traceId : flowInstance.id;
        long diff = Math.max(0, System.currentTimeMillis() - flowInstance.getStartMillis());
        String formatted = Utility.getInstance().elapsedTime(diff);
        log.info("Flow {} ({}) {} in {}", flowInstance.getFlow().id, logId, normal? "completed" : "aborted", formatted);
        flowInstance.close();
        Flows.closeFlowInstance(flowInstance.id);
    }

    @SuppressWarnings("rawtypes")
    private void handleCallback(String from, FlowInstance flowInstance, Task task, EventEnvelope event, int seq)
                                throws IOException {
        Map<String, Object> combined = new HashMap<>();
        combined.put(INPUT, flowInstance.dataset.get(INPUT));
        combined.put(MODEL, flowInstance.dataset.get(MODEL));
        combined.put(STATUS, event.getStatus());
        combined.put(HEADER, event.getHeaders());
        combined.put(RESULT, event.getRawBody());
        // consolidated dataset includes input, model and task result set
        MultiLevelMap consolidated = new MultiLevelMap(combined);
        // perform output data mapping //
        List<String> mapping = task.output;
        for (String entry: mapping) {
            int sep = entry.indexOf(MAP_TO);
            if (sep > 0) {
                String lhs = entry.substring(0, sep).trim();
                boolean isInput = lhs.startsWith(INPUT_NAMESPACE) || lhs.equalsIgnoreCase(INPUT);
                final Object value;
                String rhs = entry.substring(sep+2).trim();
                int colon = rhs.lastIndexOf(':');
                String rhsSelector = colon == -1? rhs : rhs.substring(0, colon);
                if (isInput || lhs.startsWith(MODEL_NAMESPACE)
                        || lhs.equals(HEADER) || lhs.startsWith(HEADER_NAMESPACE)
                        || lhs.equals(STATUS)
                        || lhs.equals(RESULT) || lhs.startsWith(RESULT_NAMESPACE)) {
                    value = getLhsElement(lhs, consolidated);
                    if (value == null) {
                        removeModelElement(rhsSelector, consolidated);
                    }
                } else {
                    value = getConstantValue(lhs, rhs);
                }
                if (value != null) {
                    boolean required = true;
                    if (rhs.startsWith(FILE_TYPE)) {
                        required = false;
                        SimpleFileDescriptor fd = new SimpleFileDescriptor(rhs);
                        File f = new File(fd.fileName);
                        // automatically create parent folder
                        if (!f.exists()) {
                            String parentPath = f.getParent();
                            if (!("/".equals(parentPath))) {
                                File parent = f.getParentFile();
                                if (!parent.exists()) {
                                    if (parent.mkdirs()) {
                                        log.info("Folder {} created", parentPath);
                                    } else {
                                        log.error("Unable to create folder {} - please check access rights", parentPath);
                                    }
                                }
                            }
                        }
                        if (!f.exists() || (!f.isDirectory() && f.canWrite())) {
                            switch (value) {
                                case byte[] b -> util.bytes2file(f, b);
                                case String str -> util.str2file(f, str);
                                case Map map ->
                                    // best effort to save as a JSON string
                                    util.str2file(f, SimpleMapper.getInstance().getMapper().writeValueAsString(map));
                                default -> util.str2file(f, value.toString());
                            }
                        } else {
                            log.warn("Failed data mapping {} -> {} - Cannot write {}", lhs, rhs, fd.fileName);
                        }
                    }
                    if (rhs.equals(OUTPUT_STATUS)) {
                        int status = value instanceof Integer ? (Integer) value : util.str2int(value.toString());
                        if (status < 100 || status > 599) {
                            log.error("Invalid output mapping '{}' - expect: valid HTTP status code, actual: {}",
                                    entry, value);
                            required = false;
                        }
                    }
                    if (rhs.equals(OUTPUT_HEADER)) {
                        if (!(value instanceof Map)) {
                            log.error("Invalid output mapping '{}' - expect: Map, actual: {}",
                                    entry, value.getClass().getSimpleName());
                            required = false;
                        }
                    }
                    if (rhs.startsWith(EXT_NAMESPACE)) {
                        required = false;
                        callExternalStateMachine(flowInstance, task, rhs, value);
                    }
                    if (required) {
                        setRhsElement(value, rhs, consolidated);
                    }
                } else {
                    if (rhs.startsWith(EXT_NAMESPACE)) {
                        callExternalStateMachine(flowInstance, task, rhs, null);
                    }
                }
            }
        }
        if (seq > 0 && flowInstance.pipeMap.containsKey(seq)) {
            PipeInfo pipe = flowInstance.pipeMap.get(seq);
            // this is a callback from a fork task
            if (JOIN.equals(pipe.getType())) {
                JoinTaskInfo joinInfo = (JoinTaskInfo) pipe;
                int callBackCount = joinInfo.resultCount.incrementAndGet();
                log.debug("Flow {}:{} fork-n-join #{} result {} of {} from {}",
                        flowInstance.getFlow().id, flowInstance.id, seq, callBackCount, joinInfo.forks, from);
                if (callBackCount >= joinInfo.forks) {
                    flowInstance.pipeMap.remove(seq);
                    log.debug("Flow {}:{} fork-n-join #{} done", flowInstance.getFlow().id, flowInstance.id, seq);
                    executeTask(flowInstance, joinInfo.joinTask);
                }
                return;
            }
            // this is a callback from a pipeline task
            if (PIPELINE.equals(pipe.getType())) {
                PipelineInfo pipeline = (PipelineInfo) pipe;
                Task pipelineTask = pipeline.getTask();
                if (pipeline.isCompleted()) {
                    pipelineCompletion(flowInstance, pipeline, consolidated, seq);
                    return;
                }
                int n = pipeline.nextStep();
                if (pipeline.isLastStep(n)) {
                    pipeline.setCompleted();
                    log.debug("Flow {}:{} pipeline #{} last step-{} {}",
                            flowInstance.getFlow().id, flowInstance.id, seq, n+1, pipeline.getTaskName(n));
                } else {
                    log.debug("Flow {}:{} pipeline #{} next step-{} {}",
                            flowInstance.getFlow().id, flowInstance.id, seq, n+1, pipeline.getTaskName(n));
                }
                if (pipelineTask.conditions.isEmpty()) {
                    executeTask(flowInstance, pipeline.getTaskName(n), seq);
                } else {
                    /*
                     * The first element of a condition is the model key.
                     * The second element is "continue" or "break".
                     */
                    boolean conditionMet = false;
                    for (List<String> condition: pipelineTask.conditions) {
                        Object o = consolidated.getElement(condition.getFirst());
                        if (Boolean.TRUE.equals(o)) {
                            String action = condition.get(1);
                            conditionMet = action != null;
                            if (BREAK.equals(action)) {
                                flowInstance.pipeMap.remove(seq);
                                executeTask(flowInstance, pipeline.getExitTask());
                            } else if (CONTINUE.equals(action)) {
                                pipeline.setCompleted();
                                pipelineCompletion(flowInstance, pipeline, consolidated, seq);
                            }
                            consolidated.removeElement(condition.getFirst());
                            break;
                        }
                    }
                    if (!conditionMet) {
                        executeTask(flowInstance, pipeline.getTaskName(n), seq);
                    }
                }
                return;
            }
        }
        String executionType = task.execution;
        // consolidated dataset would be mapped as output for "response", "end" and "decision" tasks
        if (RESPONSE.equals(executionType)) {
            handleResponseTask(flowInstance, task, consolidated);
        }
        if (END.equals(executionType)) {
            handleEndTask(flowInstance, task, consolidated);
        }
        if (DECISION.equals(executionType)) {
            handleDecisionTask(flowInstance, task, consolidated);
        }
        // consolidated dataset should be mapped to model for normal tasks
        if (SEQUENTIAL.equals(executionType)) {
            queueSequentialTask(flowInstance, task);
        }
        if (PARALLEL.equals(executionType)) {
            queueParallelTasks(flowInstance, task);
        }
        if (FORK.equals(executionType)) {
            handleForkAndJoin(flowInstance, task);
        }
        if (PIPELINE.equals(executionType)) {
            handlePipelineTask(flowInstance, task, consolidated);
        }
    }

    private void pipelineCompletion(FlowInstance flowInstance, PipelineInfo pipeline,
                                    MultiLevelMap consolidated, int seq) throws IOException {
        Task pipelineTask = pipeline.getTask();
        boolean iterate = false;
        if (WHILE.equals(pipelineTask.getLoopType()) && pipelineTask.getWhileModelKey() != null) {
            Object o = consolidated.getElement(pipelineTask.getWhileModelKey());
            iterate = Boolean.TRUE.equals(o);
        } else if (FOR.equals(pipelineTask.getLoopType())) {
            // execute sequencer in the for-statement
            Object modelValue = consolidated.getElement(pipelineTask.sequencer.getFirst());
            int v = modelValue instanceof Integer? (int) modelValue : util.str2int(modelValue.toString());
            String command = pipelineTask.sequencer.get(1);
            if (INCREMENT.equals(command)) {
                consolidated.setElement(pipelineTask.sequencer.getFirst(), v + 1);
            }
            if (DECREMENT.equals(command)) {
                consolidated.setElement(pipelineTask.sequencer.getFirst(), v - 1);
            }
            // evaluate for-condition
            iterate = evaluateForCondition(consolidated.getElement(pipelineTask.comparator.getFirst()),
                    pipelineTask.comparator.get(1), util.str2int(pipelineTask.comparator.get(2)));
        }
        if (iterate) {
            pipeline.resetPointer();
            log.debug("Flow {}:{} pipeline #{} first {}",
                    flowInstance.getFlow().id, flowInstance.id, seq, pipeline.getTaskName(0));
            executeTask(flowInstance, pipeline.getTaskName(0), seq);

        } else {
            flowInstance.pipeMap.remove(seq);
            executeTask(flowInstance, pipeline.getExitTask());
        }
    }

    private void handleResponseTask(FlowInstance flowInstance, Task task, MultiLevelMap map) throws IOException {
        sendResponse(flowInstance, task, map);
        queueSequentialTask(flowInstance, task);
    }

    private void handleEndTask(FlowInstance flowInstance, Task task, MultiLevelMap map) throws IOException {
        sendResponse(flowInstance, task, map);
        endFlow(flowInstance, true);
    }

    private void handleDecisionTask(FlowInstance flowInstance, Task task, MultiLevelMap map) throws IOException {
        Object decisionValue = map.getElement(DECISION);
        List<String> nextTasks = task.nextSteps;
        final int decisionNumber;
        if (decisionValue instanceof Boolean) {
            decisionNumber = Boolean.TRUE.equals(decisionValue) ? 1 : 2;
        } else if (decisionValue != null) {
            decisionNumber = Math.max(1, util.str2int(decisionValue.toString()));
        } else {
            // invalid decision number if value is not boolean or number
            decisionNumber = nextTasks.size() + 1;
        }
        if (decisionNumber > nextTasks.size()) {
            log.error("Flow {}:{} {} returned invalid decision ({})",
                    flowInstance.getFlow().id, flowInstance.id, task.service, decisionValue);
            abortFlow(flowInstance, 500,
                    "Task "+task.service+" returned invalid decision ("+decisionValue+")");
        } else {
            String next = nextTasks.get(decisionNumber - 1);
            executeTask(flowInstance, next);
        }
    }

    private void queueSequentialTask(FlowInstance flowInstance, Task task) throws IOException {
        List<String> nextTasks = task.nextSteps;
        if (!nextTasks.isEmpty()) {
            String next = nextTasks.getFirst();
            executeTask(flowInstance, next);
        }
    }

    private void queueParallelTasks(FlowInstance flowInstance, Task task) throws IOException {
        List<String> nextTasks = task.nextSteps;
        if (!nextTasks.isEmpty()) {
            for (String next: nextTasks) {
                executeTask(flowInstance, next);
            }
        }
    }

    private void handleForkAndJoin(FlowInstance flowInstance, Task task) throws IOException {
        List<String> steps = task.nextSteps;
        if (!steps.isEmpty() && task.getJoinTask() != null) {
            int seq = flowInstance.pipeCounter.incrementAndGet();
            int forks = steps.size();
            flowInstance.pipeMap.put(seq, new JoinTaskInfo(forks, task.getJoinTask()));
            for (String next: steps) {
                executeTask(flowInstance, next, seq);
            }
        }
    }

    private boolean evaluateForCondition(Object modelValue, String comparator, int value) {
        int v = modelValue instanceof Integer? (int) modelValue : util.str2int(modelValue.toString());
        return switch (comparator) {
            case "<" -> v < value;
            case ">" -> v > value;
            case ">=" -> v >= value;
            case "<=" -> v <= value;
            case null, default -> false;
        };
    }

    private void handlePipelineTask(FlowInstance flowInstance, Task task, MultiLevelMap map) throws IOException {
        if (!task.pipelineSteps.isEmpty()) {
            // evaluate initial condition
            boolean valid = true;
            if (WHILE.equals(task.getLoopType()) && task.getWhileModelKey() != null) {
                Object o = map.getElement(task.getWhileModelKey());
                valid = Boolean.TRUE.equals(o);
            } else if (FOR.equals(task.getLoopType())) {
                // execute initializer if any
                if (task.init.size() == 2) {
                    int n = util.str2int(task.init.get(1));
                    if (task.init.getFirst().startsWith(MODEL_NAMESPACE)) {
                        map.setElement(task.init.getFirst(), n);
                    }
                }
                valid = evaluateForCondition(map.getElement(task.comparator.getFirst()),
                                                        task.comparator.get(1), util.str2int(task.comparator.get(2)));
            }
            if (valid) {
                int seq = flowInstance.pipeCounter.incrementAndGet();
                PipelineInfo pipeline = new PipelineInfo(task);
                flowInstance.pipeMap.put(seq, pipeline);
                pipeline.resetPointer();
                log.debug("Flow {}:{} pipeline #{} begin {}",
                        flowInstance.getFlow().id, flowInstance.id, seq, pipeline.getTaskName(0));
                executeTask(flowInstance, pipeline.getTaskName(0), seq);
            } else {
                executeTask(flowInstance, task.nextSteps.getFirst());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sendResponse(FlowInstance flowInstance, Task task, MultiLevelMap map) throws IOException {
        PostOffice po = new PostOffice(TaskExecutor.SERVICE_NAME, flowInstance.getTraceId(), flowInstance.getTracePath());
        if (flowInstance.isNotResponded()) {
            flowInstance.setResponded(true);
            // is a response event required when the flow is completed?
            if (flowInstance.replyTo != null) {
                EventEnvelope result = new EventEnvelope();
                // restore the original correlation-ID to the calling party
                result.setTo(flowInstance.replyTo).setCorrelationId(flowInstance.cid);
                Object headers = map.getElement("output.header");
                Object body = map.getElement("output.body");
                Object status = map.getElement("output.status");
                if (status != null) {
                    int value = util.str2int(status.toString());
                    if (value > 0) {
                        result.setStatus(value);
                    } else {
                        log.warn("Unable to set status in response {}:{} - task {} return status is negative value",
                                flowInstance.getFlow().id, flowInstance.id, task.service);
                    }
                }
                if (headers instanceof Map) {
                    Map<String, Object> resHeaders = (Map<String, Object>) headers;
                    for (Map.Entry<String, Object> entry : resHeaders.entrySet()) {
                        result.setHeader(entry.getKey(), entry.getValue());
                    }
                }
                result.setBody(body);
                po.send(result);
            }
        }
    }

    private void executeTask(FlowInstance flowInstance, String routeName) throws IOException {
        executeTask(flowInstance, routeName, -1, null);
    }

    private void executeTask(FlowInstance flowInstance, String routeName, int seq) throws IOException {
        executeTask(flowInstance, routeName, seq, null);
    }

    @SuppressWarnings("unchecked")
    private void executeTask(FlowInstance flowInstance, String routeName, int seq, Map<String, Object> error)
            throws IOException {
        Task task = flowInstance.getFlow().tasks.get(routeName);
        if (task == null) {
            log.error("Unable to process flow {}:{} - missing task '{}'",
                    flowInstance.getFlow().id, flowInstance.id, routeName);
            abortFlow(flowInstance, 500, SERVICE_AT +routeName+" not defined");
            return;
        }

        Map<String, Object> combined = new HashMap<>();
        combined.put(INPUT, flowInstance.dataset.get(INPUT));
        combined.put(MODEL, flowInstance.dataset.get(MODEL));
        if (error != null) {
            combined.put(ERROR, error);
        }
        MultiLevelMap source = new MultiLevelMap(combined);
        MultiLevelMap target = new MultiLevelMap();
        Map<String, String> optionalHeaders = new HashMap<>();
        // perform input data mapping //
        List<String> mapping = task.input;
        for (String entry: mapping) {
            int sep = entry.indexOf(MAP_TO);
            if (sep > 0) {
                String lhs = entry.substring(0, sep).trim();
                String rhs = entry.substring(sep+2).trim();
                boolean isInput = lhs.startsWith(INPUT_NAMESPACE) || lhs.equalsIgnoreCase(INPUT);
                if (lhs.startsWith(INPUT_HEADER_NAMESPACE)) {
                    lhs = lhs.toLowerCase();
                }
                if (rhs.startsWith(EXT_NAMESPACE)) {
                    final Object value;
                    if (isInput || lhs.startsWith(MODEL_NAMESPACE)) {
                        value = getLhsElement(lhs, source);
                    } else {
                        value = getConstantValue(lhs, rhs);
                    }
                    callExternalStateMachine(flowInstance, task, rhs, value);
                } else if (rhs.startsWith(MODEL_NAMESPACE)) {
                    // special case to set model variables
                    Map<String, Object> modelOnly = new HashMap<>();
                    modelOnly.put(MODEL, flowInstance.dataset.get(MODEL));
                    MultiLevelMap model = new MultiLevelMap(modelOnly);
                    if (isInput || lhs.startsWith(MODEL_NAMESPACE)) {
                        Object value = getLhsElement(lhs, source);
                        if (value == null) {
                            removeModelElement(rhs, model);
                        } else {
                            setRhsElement(value, rhs, model);
                        }
                    } else {
                        setConstantValue(lhs, rhs, model);
                    }
                } else if (isInput || lhs.startsWith(MODEL_NAMESPACE) || lhs.startsWith(ERROR_NAMESPACE)) {
                    // normal case to input argument
                    Object value = getLhsElement(lhs, source);
                    if (value != null) {
                        boolean valid = true;
                        if (ALL.equals(rhs)) {
                            if (value instanceof Map) {
                                target = new MultiLevelMap((Map<String, Object>) value);
                            } else {
                                valid = false;
                            }
                        } else if (rhs.equals(HEADER)) {
                            if (value instanceof Map) {
                                Map<String, Object> headers = (Map<String, Object>) value;
                                headers.forEach((k,v) -> optionalHeaders.put(k, v.toString()));
                            } else {
                                valid = false;
                            }
                        } else if (rhs.startsWith(HEADER_NAMESPACE)) {
                            String k = rhs.substring(HEADER_NAMESPACE.length());
                            if (!k.isEmpty()) {
                                optionalHeaders.put(k, value.toString());
                            }
                        } else {
                            target.setElement(rhs, value);
                        }
                        if (!valid) {
                            log.error("Invalid input mapping '{}' - expect: Map, actual: {}",
                                    entry, value.getClass().getSimpleName());
                        }
                    }
                } else {
                    // Assume left hand side is a constant
                    if (rhs.startsWith(HEADER_NAMESPACE)) {
                        String k = rhs.substring(HEADER_NAMESPACE.length());
                        Object v = getConstantValue(lhs, rhs);
                        if (!k.isEmpty() && v != null) {
                            optionalHeaders.put(k, v.toString());
                        }
                    } else {
                        setConstantValue(lhs, rhs, target);
                    }
                }
            }
        }
        // need to send later?
        long deferred = 0;
        if (task.getDelay() > 0) {
            deferred = task.getDelay();
        } else {
            if (task.getDelayVar() != null) {
                Object d = source.getElement(task.getDelayVar());
                if (d != null) {
                    long delay = util.str2long(d.toString());
                    if (delay > 0 && delay < flowInstance.getFlow().ttl) {
                        deferred = delay;
                    } else {
                        log.warn("Unable to schedule future task for {} because {} is invalid (TTL={}, delay={})",
                                task.service, task.getDelayVar(), flowInstance.getFlow().ttl, delay);
                    }
                } else {
                    log.warn("Unable to schedule future task for {} because {} does not exist",
                            task.service, task.getDelayVar());
                }
            }
        }
        final Platform platform = Platform.getInstance();
        final String compositeCid = seq > 0? flowInstance.id + "#" + seq : flowInstance.id;
        if (task.service.startsWith(FLOW_PROTOCOL)) {
            String flowId = task.service.substring(FLOW_PROTOCOL.length());
            Flow subFlow = Flows.getFlow(flowId);
            if (subFlow == null) {
                log.error("Unable to process flow {}:{} - missing sub-flow {}",
                        flowInstance.getFlow().id, flowInstance.id, task.service);
                abortFlow(flowInstance, 500, task.service+" not defined");
                return;
            }
            if (!optionalHeaders.isEmpty()) {
                target.setElement(HEADER, optionalHeaders);
            }
            EventEnvelope forward = new EventEnvelope().setTo(EventScriptManager.SERVICE_NAME)
                    .setHeader(FLOW_ID, flowId).setBody(target.getMap()).setCorrelationId(util.getUuid());
            PostOffice po = new PostOffice(task.service,
                                            flowInstance.getTraceId(), flowInstance.getTracePath());
            po.asyncRequest(forward, subFlow.ttl, false).onSuccess(response -> {
                EventEnvelope event = new EventEnvelope()
                        .setTo(TaskExecutor.SERVICE_NAME + "@" + platform.getOrigin())
                        .setCorrelationId(compositeCid).setStatus(response.getStatus())
                        .setHeaders(response.getHeaders())
                        .setBody(response.getBody());
                try {
                    po.send(event);
                } catch (IOException e) {
                    // this should not occur
                    throw new RuntimeException(e);
                }
            });

        } else {
            PostOffice po = new PostOffice(TaskExecutor.SERVICE_NAME,
                                            flowInstance.getTraceId(), flowInstance.getTracePath());
            EventEnvelope event = new EventEnvelope().setTo(task.service)
                    .setCorrelationId(compositeCid)
                    .setReplyTo(TaskExecutor.SERVICE_NAME + "@" + platform.getOrigin())
                    .setBody(target.getMap());
            optionalHeaders.forEach(event::setHeader);
            // execute task by sending event
            if (deferred > 0) {
                po.sendLater(event, new Date(System.currentTimeMillis() + deferred));
            } else {
                po.send(event);
            }
        }
    }

    private void callExternalStateMachine(FlowInstance flowInstance, Task task, String rhs, Object value)
            throws IOException {
        String key = rhs.substring(EXT_NAMESPACE.length()).trim();
        String externalStateMachine = flowInstance.getFlow().externalStateMachine;
        PostOffice po = new PostOffice(task.service,
                flowInstance.getTraceId(), flowInstance.getTracePath());
        if (value == null) {
            // tell external state machine to remove key-value
            po.send(externalStateMachine, new Kv(TYPE, REMOVE), new Kv(KEY, key));
        } else {
            // tell external state machine to save key-value
            po.send(externalStateMachine, value, new Kv(TYPE, PUT), new Kv(KEY, key));
        }
    }

    private void removeModelElement(String rhs, MultiLevelMap model) {
        int colon = rhs.lastIndexOf(':');
        model.removeElement(colon == -1? rhs : rhs.substring(0, colon));
    }


    private Object getLhsElement(String lhs, MultiLevelMap source) {
        int colon = lhs.lastIndexOf(':');
        String selector = colon == -1? lhs : lhs.substring(0, colon).trim();
        Object value = source.getElement(selector);
        if (colon != -1 && lhs.startsWith(MODEL_NAMESPACE)) {
            String type = lhs.substring(colon+1).trim();
            if (value != null) {
                return getValueByType(type, value, "LHS '"+lhs+"'");
            }
        }
        return value;
    }

    @SuppressWarnings("rawtypes")
    private Object getValueByType(String type, Object value, String path) {
        if (type.startsWith(SUBSTRING_SUFFIX)) {
            if (value instanceof String str) {
                String error = "missing close bracket";
                if (type.endsWith(CLOSE_BRACKET)) {
                    // substring(0, 3) becomes [substring, 0, 3]
                    List<String> parts = util.split(type, "(, )");
                    if (parts.size() > 1 && parts.size() < 4) {
                        int start = util.str2int(parts.get(1));
                        int end = parts.size() == 2? str.length() : util.str2int(parts.get(2));
                        if (end > start && start != -1 && end <= str.length()) {
                            return str.substring(start, end);
                        } else {
                            error = "index out of bound";
                        }
                    } else {
                        error = "invalid substring format";
                    }
                }
                log.error("Unable to do {} of {} - {}", type, path, error);
            } else {
                log.error("Unable to do {} of {} - value is not a string", type, path);
            }
        } else {
            switch (type) {
                case TEXT_SUFFIX -> {
                    return switch (value) {
                        case String str -> str;
                        case byte[] b -> util.getUTF(b);
                        case Map map -> SimpleMapper.getInstance().getMapper().writeValueAsString(map);
                        default -> String.valueOf(value);
                    };
                }
                case BINARY_SUFFIX -> {
                    return switch (value) {
                        case byte[] b -> b;
                        case String str -> util.getUTF(str);
                        case Map map -> SimpleMapper.getInstance().getMapper().writeValueAsBytes(map);
                        default -> util.getUTF(String.valueOf(value));
                    };
                }
                case B64_SUFFIX -> {
                    if (value instanceof byte[] b) {
                        return util.bytesToBase64(b);
                    } else if (value instanceof String str) {
                        try {
                            return util.base64ToBytes(str);
                        } catch (IllegalArgumentException e) {
                            log.error("Unable to decode {} from text into B64 - {}", path, e.getMessage());
                        }
                    }
                }
                default -> log.error("Unable to do {} of {} - " +
                                "matching type must be substring(start, end), text, binary or b64", type, path);
            }
        }
        return value;
    }

    private void setRhsElement(Object value, String rhs, MultiLevelMap target) {
        boolean updated = false;
        int colon = rhs.lastIndexOf(':');
        String selector = colon == -1? rhs : rhs.substring(0, colon).trim();
        if (colon != -1 && rhs.startsWith(MODEL_NAMESPACE)) {
            String type = rhs.substring(colon+1).trim();
            Object matched = getValueByType(type, value, "RHS '"+rhs+"'");
            target.setElement(selector, matched);
            updated = true;
        }
        if (!updated) {
            target.setElement(selector, value);
        }
    }

    private Object getConstantValue(String lhs, String rhs) {
        int last = lhs.lastIndexOf(CLOSE_BRACKET);
        if (last > 0) {
            if (lhs.startsWith(TEXT_TYPE)) {
                return lhs.substring(TEXT_TYPE.length(), last).trim();
            }
            if (lhs.startsWith(INTEGER_TYPE)) {
                return util.str2int(lhs.substring(INTEGER_TYPE.length(), last).trim());
            }
            if (lhs.startsWith(LONG_TYPE)) {
                return util.str2long(lhs.substring(LONG_TYPE.length(), last).trim());
            }
            if (lhs.startsWith(FLOAT_TYPE)) {
                return util.str2float(lhs.substring(FLOAT_TYPE.length(), last).trim());
            }
            if (lhs.startsWith(DOUBLE_TYPE)) {
                return util.str2double(lhs.substring(DOUBLE_TYPE.length(), last).trim());
            }
            if (lhs.startsWith(BOOLEAN_TYPE)) {
                return TRUE.equalsIgnoreCase(lhs.substring(BOOLEAN_TYPE.length(), last).trim());
            }
            if (lhs.startsWith(FILE_TYPE)) {
                SimpleFileDescriptor fd = new SimpleFileDescriptor(lhs);
                File f = new File(fd.fileName);
                if (f.exists() && !f.isDirectory() && f.canRead()) {
                    if (fd.binary) {
                        return util.file2bytes(f);
                    } else {
                        return util.file2str(f);
                    }
                } else {
                    log.warn("Failed data mapping {} -> {} - Cannot read {}", lhs, rhs, fd.fileName);
                }
            }
            if (lhs.startsWith(CLASSPATH_TYPE)) {
                SimpleFileDescriptor fd = new SimpleFileDescriptor(lhs);
                InputStream in = this.getClass().getResourceAsStream(fd.fileName);
                if (in != null) {
                    if (fd.binary) {
                        return util.stream2bytes(in);
                    } else {
                        return util.stream2str(in);
                    }
                } else {
                    log.warn("Failed data mapping {} -> {} - Missing classpath {}", lhs, rhs, fd.fileName);
                }
            }
        }
        return null;
    }

    private void setConstantValue(String lhs, String rhs, MultiLevelMap target) {
        Object value = getConstantValue(lhs, rhs);
        if (value != null) {
            setRhsElement(value, rhs, target);
        }
    }

    private static class SimpleFileDescriptor {
        final public String fileName;
        final public boolean binary;

        public SimpleFileDescriptor(String value) {
            int last = value.lastIndexOf(CLOSE_BRACKET);
            final int offset;
            if (value.startsWith(FILE_TYPE)) {
                offset = FILE_TYPE.length();
            } else if (value.startsWith(CLASSPATH_TYPE)) {
                offset = CLASSPATH_TYPE.length();
            } else {
                // this should not occur
                offset = 0;
            }
            final String fileDescriptor = value.substring(offset, last).trim();
            if (fileDescriptor.startsWith(TEXT_FILE)) {
                fileName = fileDescriptor.substring(TEXT_FILE.length());
                binary = false;
            } else if (fileDescriptor.startsWith(BINARY_FILE)) {
                fileName = fileDescriptor.substring(BINARY_FILE.length());
                binary = true;
            } else {
                // default fileType is binary
                fileName = fileDescriptor;
                binary = true;
            }
        }
    }

}
