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

import com.accenture.models.*;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.services.DistributedTrace;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
@EventInterceptor
@PreLoad(route = "task.executor")
public class TaskExecutor implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String SERVICE_NAME = "task.executor";
    private static final Logger log = LoggerFactory.getLogger(TaskExecutor.class);
    private static final ConcurrentMap<String, TaskReference> taskRefs = new ConcurrentHashMap<>();
    private static final Utility util = Utility.getInstance();
    private static final String FIRST_TASK = "first_task";
    private static final String FLOW_ID = "flow_id";
    private static final String PARENT = "parent";
    private static final String FLOW_PROTOCOL = "flow://";
    private static final String TYPE = "type";
    private static final String PUT = "put";
    private static final String KEY = "key";
    private static final String DATA = "data";
    private static final String REMOVE = "remove";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String INPUT = "input";
    private static final String OUTPUT_STATUS = "output.status";
    private static final String OUTPUT_BODY = "output.body";
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
    private static final String MAP_TYPE = "map(";
    private static final String CLOSE_BRACKET = ")";
    private static final String TEXT_FILE = "text:";
    private static final String BINARY_FILE = "binary:";
    private static final String MAP_TO = "->";
    private static final String ALL = "*";
    private static final String END = "end";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NULL = "null";
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
    private static final String INTEGER_SUFFIX = "int";
    private static final String LONG_SUFFIX = "long";
    private static final String FLOAT_SUFFIX = "float";
    private static final String DOUBLE_SUFFIX = "double";
    private static final String BOOLEAN_SUFFIX = "boolean";
    private static final String UUID_SUFFIX = "uuid";
    private static final String NEGATE_SUFFIX = "!";
    private static final String SUBSTRING_TYPE = "substring(";
    private static final String CONCAT_TYPE = "concat(";
    private static final String AND_TYPE = "and(";
    private static final String OR_TYPE = "or(";

    private enum OPERATION {
        SIMPLE_COMMAND,
        SUBSTRING_COMMAND,
        CONCAT_COMMAND,
        AND_COMMAND,
        OR_COMMAND,
        BOOLEAN_COMMAND
    }

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) throws IOException {
        String compositeCid = event.getCorrelationId();
        if (compositeCid == null) {
            log.error("Event {} dropped - missing correlation ID", event.getId());
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
        /*
         * Resolve unique task reference and release it to reduce memory use
         *
         * Two cases when task reference is not found:
         * 1. first task
         * 2. flow timeout
         */
        var ref = taskRefs.get(cid);
        if (ref != null) {
            taskRefs.remove(cid);
        }
        String refId = ref == null? cid : ref.flowInstanceId;
        FlowInstance flowInstance = Flows.getFlowInstance(refId);
        if (flowInstance == null) {
            log.warn("Flow instance {} is invalid or expired", refId);
            return null;
        }
        String flowName = flowInstance.getFlow().id;
        if (headers.containsKey(TIMEOUT)) {
            log.warn("Flow {}:{} expired", flowName, flowInstance.id);
            abortFlow(flowInstance, 408, "Flow timeout for "+ flowInstance.getFlow().ttl+" ms");
            return null;
        }
        try {
            String firstTask = headers.get(FIRST_TASK);
            if (firstTask != null) {
                executeTask(flowInstance, firstTask);
            } else {
                // handle callback from a task
                String from = ref != null ? ref.processId : event.getFrom();
                if (from == null) {
                    log.error("Unable to process callback {}:{} - task does not provide 'from' address", flowName, refId);
                    return null;
                }
                String caller = from.contains("@") ? from.substring(0, from.indexOf('@')) : from;
                Task task = flowInstance.getFlow().tasks.get(caller);
                if (task == null) {
                    log.error("Unable to process callback {}:{} - missing task in {}", flowName, refId, caller);
                    return null;
                }
                int statusCode = event.getStatus();
                if (statusCode >= 400 || event.isException()) {
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
                    String handler = task.getExceptionTask() != null ? task.getExceptionTask() : flowInstance.getFlow().exception;
                    if (handler != null) {
                        Map<String, Object> error = new HashMap<>();
                        error.put(CODE, statusCode);
                        error.put(MESSAGE, event.getError());
                        String stackTrace = event.getStackTrace();
                        if (stackTrace != null) {
                            error.put(STACK_TRACE, stackTrace);
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
        } catch (Exception e) {
            log.error("Unable to execute flow {}:{} - {}", flowName, flowInstance.id, e.getMessage());
            abortFlow(flowInstance, 500, e.getMessage());
        }
        return null;
    }

    private void abortFlow(FlowInstance flowInstance, int status, Object message) throws IOException {
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

    private void endFlow(FlowInstance flowInstance, boolean normal) throws IOException {
        flowInstance.close();
        Flows.closeFlowInstance(flowInstance.id);
        // clean up task references and release memory
        flowInstance.pendingTasks.keySet().forEach(taskRefs::remove);
        String traceId = flowInstance.getTraceId();
        String logId = traceId != null? traceId : flowInstance.id;
        long diff = Math.max(0, System.currentTimeMillis() - flowInstance.getStartMillis());
        String formatted = Utility.getInstance().elapsedTime(diff);
        List<String> taskList = new ArrayList<>(flowInstance.tasks);
        int totalExecutions = taskList.size();
        var payload = new HashMap<String, Object>();
        var metrics = new HashMap<String, Object>();
        var annotations = new HashMap<String, Object>();
        payload.put("trace", metrics);
        payload.put("annotations", annotations);
        metrics.put("origin", Platform.getInstance().getOrigin());
        metrics.put("id", logId);
        metrics.put("service", TaskExecutor.SERVICE_NAME);
        metrics.put("from", EventScriptManager.SERVICE_NAME);
        metrics.put("exec_time", diff);
        metrics.put("start", util.date2str(new Date(flowInstance.getStartMillis())));
        metrics.put("path", flowInstance.getTracePath());
        metrics.put(STATUS, normal? 200 : 400);
        metrics.put("success", normal);
        if (!normal) {
            metrics.put("exception", "Flow aborted");
        }
        annotations.put("execution", "Run " + totalExecutions +
                        " task" + (totalExecutions == 1? "" : "s") + " in " + formatted);
        annotations.put("tasks", taskList);
        annotations.put("flow", flowInstance.getFlow().id);
        EventEmitter.getInstance().send(new EventEnvelope()
                                    .setTo(DistributedTrace.DISTRIBUTED_TRACING).setBody(payload));
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
                if (isInput || lhs.startsWith(MODEL_NAMESPACE)
                        || lhs.equals(HEADER) || lhs.startsWith(HEADER_NAMESPACE)
                        || lhs.equals(STATUS)
                        || lhs.equals(RESULT) || lhs.startsWith(RESULT_NAMESPACE)) {
                    value = getLhsElement(lhs, consolidated);
                    if (value == null) {
                        removeModelElement(rhs, consolidated);
                    }
                } else {
                    value = getConstantValue(lhs);
                }
                if (rhs.startsWith(FILE_TYPE)) {
                    SimpleFileDescriptor fd = new SimpleFileDescriptor(rhs);
                    File f = new File(fd.fileName);
                    // automatically create parent folder
                    boolean fileFound = f.exists();
                    if (!fileFound) {
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
                    if (!fileFound || (!f.isDirectory() && f.canWrite())) {
                        switch (value) {
                            // delete the RHS' target file if LHS value is null
                            case null ->    {
                                                if (fileFound && f.delete()) {
                                                    log.debug("File {} deleted", f);
                                                }
                                            }
                            case byte[] b -> util.bytes2file(f, b);
                            case String str -> util.str2file(f, str);
                            // best effort to save as a JSON string
                            case Map map ->
                                util.str2file(f, SimpleMapper.getInstance().getMapper().writeValueAsString(map));
                            default -> util.str2file(f, String.valueOf(value));
                        }
                    }
                } else {
                    if (value != null) {
                        boolean required = true;
                        if (rhs.equals(OUTPUT_STATUS)) {
                            int status = value instanceof Integer v? v : util.str2int(String.valueOf(value));
                            if (status < 100 || status > 599) {
                                log.error("Invalid output mapping '{}' - expect: valid HTTP status code, actual: {}",
                                        entry, status);
                                required = false;
                            }
                        }
                        if (rhs.equals(OUTPUT_HEADER) && !(value instanceof Map)) {
                            log.error("Invalid output mapping '{}' - expect: Map, actual: {}",
                                    entry, value.getClass().getSimpleName());
                            required = false;
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
                            flowInstance.getFlow().id, flowInstance.id, seq, n + 1, pipeline.getTaskName(n));
                } else {
                    log.debug("Flow {}:{} pipeline #{} next step-{} {}",
                            flowInstance.getFlow().id, flowInstance.id, seq, n + 1, pipeline.getTaskName(n));
                }
                if (pipelineTask.conditions.isEmpty()) {
                    if (pipeline.isCompleted() && pipeline.isSingleton()) {
                        pipelineCompletion(flowInstance, pipeline, consolidated, seq);
                    } else {
                        executeTask(flowInstance, pipeline.getTaskName(n), seq);
                    }
                } else {
                    String action = null;
                    for (List<String> condition: pipelineTask.conditions) {
                        /*
                         * The first element of a loop condition is the model key.
                         * The second element is "continue" or "break".
                         */
                        var resolved = resolveCondition(condition, consolidated);
                        if (resolved != null) {
                            action = resolved;
                            if (CONTINUE.equals(resolved)) {
                                // clear condition
                                consolidated.setElement(condition.getFirst(), false);
                            }
                            break;
                        }
                    }
                    if (BREAK.equals(action)) {
                        flowInstance.pipeMap.remove(seq);
                        executeTask(flowInstance, pipeline.getExitTask());
                    } else if (CONTINUE.equals(action)) {
                        pipelineCompletion(flowInstance, pipeline, consolidated, seq);
                    } else {
                        if (pipeline.isCompleted() && pipeline.isSingleton()) {
                            pipelineCompletion(flowInstance, pipeline, consolidated, seq);
                        } else {
                            executeTask(flowInstance, pipeline.getTaskName(n), seq);
                        }
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

    private String resolveCondition(List<String> condition, MultiLevelMap consolidated) {
        if (Boolean.TRUE.equals(consolidated.getElement(condition.getFirst()))) {
            return condition.get(1);
        } else {
            return null;
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
            final int v = modelValue instanceof Integer? (int) modelValue : util.str2int(String.valueOf(modelValue));
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
            log.debug("Flow {}:{} pipeline #{} loop {}",
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
            decisionNumber = Math.max(1, util.str2int(String.valueOf(decisionValue)));
        } else {
            // decision number is not given
            decisionNumber = 0;
        }
        if (decisionNumber < 1 || decisionNumber > nextTasks.size()) {
            log.error("Flow {}:{} {} returned invalid decision ({})",
                    flowInstance.getFlow().id, flowInstance.id, task.service, decisionValue);
            abortFlow(flowInstance, 500,
                    "Task "+task.service+" returned invalid decision ("+decisionValue+")");
        } else {
            executeTask(flowInstance, nextTasks.get(decisionNumber - 1));
        }
    }

    private void queueSequentialTask(FlowInstance flowInstance, Task task) throws IOException {
        List<String> nextTasks = task.nextSteps;
        if (!nextTasks.isEmpty()) {
            executeTask(flowInstance, nextTasks.getFirst());
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
            case "<=" -> v <= value;
            case ">" -> v > value;
            case ">=" -> v >= value;
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
                Object headers = map.getElement(OUTPUT_HEADER);
                Object body = map.getElement(OUTPUT_BODY);
                Object status = map.getElement(OUTPUT_STATUS);
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

    private void executeTask(FlowInstance flowInstance, String processName) throws IOException {
        executeTask(flowInstance, processName, -1, null);
    }

    private void executeTask(FlowInstance flowInstance, String processName, int seq) throws IOException {
        executeTask(flowInstance, processName, seq, null);
    }

    @SuppressWarnings("unchecked")
    private void executeTask(FlowInstance flowInstance, String processName, int seq, Map<String, Object> error)
            throws IOException {
        Task task = flowInstance.getFlow().tasks.get(processName);
        if (task == null) {
            log.error("Unable to process flow {}:{} - missing task '{}'",
                    flowInstance.getFlow().id, flowInstance.id, processName);
            abortFlow(flowInstance, 500, SERVICE_AT +processName+" not defined");
            return;
        }
        // add the task to the flow-instance
        String functionRoute = task.getFunctionRoute();
        flowInstance.tasks.add(task.service.equals(functionRoute)? functionRoute : task.service+"("+functionRoute+")");
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
                boolean inputLike = lhs.startsWith(INPUT_NAMESPACE) || lhs.equalsIgnoreCase(INPUT) ||
                                    lhs.startsWith(MODEL_NAMESPACE) || lhs.startsWith(ERROR_NAMESPACE);
                if (lhs.startsWith(INPUT_HEADER_NAMESPACE)) {
                    lhs = lhs.toLowerCase();
                }
                if (rhs.startsWith(EXT_NAMESPACE)) {
                    final Object value;
                    if (inputLike) {
                        value = getLhsElement(lhs, source);
                    } else {
                        value = getConstantValue(lhs);
                    }
                    callExternalStateMachine(flowInstance, task, rhs, value);
                } else if (rhs.startsWith(MODEL_NAMESPACE)) {
                    // special case to set model variables
                    Map<String, Object> modelOnly = new HashMap<>();
                    modelOnly.put(MODEL, flowInstance.dataset.get(MODEL));
                    MultiLevelMap model = new MultiLevelMap(modelOnly);
                    if (inputLike) {
                        Object value = getLhsElement(lhs, source);
                        if (value == null) {
                            removeModelElement(rhs, model);
                        } else {
                            setRhsElement(value, rhs, model);
                        }
                    } else {
                        setConstantValue(lhs, rhs, model);
                    }
                } else if (inputLike) {
                    // normal case to input argument
                    Object value = getLhsElement(lhs, source);
                    // special cases for simple type matching for a non-exist model variable
                    if (value == null && lhs.startsWith(MODEL_NAMESPACE)) {
                        value = getValueFromNonExistModel(lhs);
                    }
                    if (value != null) {
                        boolean valid = true;
                        if (ALL.equals(rhs)) {
                            if (value instanceof Map) {
                                target.reload((Map<String, Object>) value);
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
                            setRhsElement(value, rhs, target);
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
                        Object v = getConstantValue(lhs);
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
                    long delay = Math.max(1, util.str2long(d.toString()));
                    if (delay < flowInstance.getFlow().ttl) {
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
        final String uuid = util.getDateUuid();
        final TaskReference ref = new TaskReference(flowInstance.id, task.service);
        taskRefs.put(uuid, ref);
        flowInstance.pendingTasks.put(uuid, true);
        final String compositeCid = seq > 0? uuid + "#" + seq : uuid;
        if (functionRoute.startsWith(FLOW_PROTOCOL)) {
            String flowId = functionRoute.substring(FLOW_PROTOCOL.length());
            Flow subFlow = Flows.getFlow(flowId);
            if (subFlow == null) {
                log.error("Unable to process flow {}:{} - missing sub-flow {}",
                        flowInstance.getFlow().id, flowInstance.id, functionRoute);
                abortFlow(flowInstance, 500, functionRoute+" not defined");
                return;
            }
            if (!optionalHeaders.isEmpty()) {
                target.setElement(HEADER, optionalHeaders);
            }
            EventEnvelope forward = new EventEnvelope().setTo(EventScriptManager.SERVICE_NAME)
                    .setHeader(PARENT, flowInstance.id)
                    .setHeader(FLOW_ID, flowId).setBody(target.getMap()).setCorrelationId(util.getUuid());
            PostOffice po = new PostOffice(functionRoute, flowInstance.getTraceId(), flowInstance.getTracePath());
            po.asyncRequest(forward, subFlow.ttl, false).onSuccess(response -> {
                EventEnvelope event = new EventEnvelope()
                        .setTo(TaskExecutor.SERVICE_NAME + "@" + platform.getOrigin())
                        .setCorrelationId(compositeCid).setStatus(response.getStatus())
                        .setHeaders(response.getHeaders()).setBody(response.getBody());
                try {
                    po.send(event);
                } catch (IOException e) {
                    // this should not occur
                    throw new IllegalArgumentException(e.getMessage());
                }
            });
        } else {
            PostOffice po = new PostOffice(TaskExecutor.SERVICE_NAME,
                                            flowInstance.getTraceId(), flowInstance.getTracePath());
            EventEnvelope event = new EventEnvelope().setTo(functionRoute)
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

    private Object getValueFromNonExistModel(String lhs) {
        int colon = lhs.lastIndexOf(':');
        if (colon > 0) {
            var qualifier = lhs.substring(colon+1).trim();
            if (UUID_SUFFIX.equals(qualifier)) {
                return util.getUuid4();
            } else {
                var parts = util.split(qualifier, "(= )");
                if (parts.size() == 3 && BOOLEAN_SUFFIX.equals(parts.getFirst()) && NULL.equals(parts.get(1))) {
                    if (TRUE.equals(parts.get(2))) {
                        return true;
                    }
                    if (FALSE.equals(parts.get(2))) {
                        return false;
                    }
                }
            }
        }
        return null;
    }

    private void callExternalStateMachine(FlowInstance flowInstance, Task task, String rhs, Object value)
            throws IOException {
        String key = rhs.substring(EXT_NAMESPACE.length()).trim();
        String externalStateMachine = flowInstance.getFlow().externalStateMachine;
        PostOffice po = new PostOffice(task.service,
                flowInstance.getTraceId(), flowInstance.getTracePath());
        if (externalStateMachine != null) {
            if (externalStateMachine.startsWith(FLOW_PROTOCOL)) {
                MultiLevelMap dataset = new MultiLevelMap();
                dataset.setElement("header.key", key);
                dataset.setElement("header.type", value == null? REMOVE : PUT);
                if (value != null) {
                    dataset.setElement("body", Map.of(DATA, value));
                }
                String flowId = externalStateMachine.substring(FLOW_PROTOCOL.length());
                EventEnvelope forward = new EventEnvelope().setTo(EventScriptManager.SERVICE_NAME)
                        .setHeader(PARENT, flowInstance.id)
                        .setHeader(FLOW_ID, flowId).setBody(dataset.getMap()).setCorrelationId(util.getUuid());
                po.send(forward);
            } else {
                if (value == null) {
                    // tell external state machine to remove key-value
                    po.send(externalStateMachine, new Kv(TYPE, REMOVE), new Kv(KEY, key));
                } else {
                    // tell external state machine to save key-value
                    po.send(externalStateMachine, Map.of(DATA, value), new Kv(TYPE, PUT), new Kv(KEY, key));
                }
            }
        }
    }

    private void removeModelElement(String rhs, MultiLevelMap model) {
        int colon = getModelTypeIndex(rhs);
        if (colon != -1) {
            String key = rhs.substring(0, colon);
            String type = rhs.substring(colon+1);
            Object value = getValueByType(type, null, "?", model);
            if (value != null) {
                setRhsElement(value, key, model);
            } else {
                model.removeElement(key);
            }
        } else {
            model.removeElement(rhs);
        }
    }

    private Object getLhsElement(String lhs, MultiLevelMap source) {
        int colon = getModelTypeIndex(lhs);
        String selector = colon == -1? lhs : lhs.substring(0, colon).trim();
        Object value = source.getElement(selector);
        if (colon != -1) {
            String type = lhs.substring(colon+1).trim();
            return getValueByType(type, value, "LHS '"+lhs+"'", source);
        }
        return value;
    }

    private int getModelTypeIndex(String text) {
        if (text.startsWith(MODEL_NAMESPACE)) {
            return text.indexOf(':');
        } else {
            return -1;
        }
    }

    private OPERATION getMappingType(String type) {
        if (type.startsWith(SUBSTRING_TYPE)) {
            return OPERATION.SUBSTRING_COMMAND;
        } else if (type.startsWith(CONCAT_TYPE)) {
            return OPERATION.CONCAT_COMMAND;
        } else if (type.startsWith(AND_TYPE)) {
            return OPERATION.AND_COMMAND;
        } else if (type.startsWith(OR_TYPE)) {
            return OPERATION.OR_COMMAND;
        } else if (type.startsWith(BOOLEAN_TYPE)) {
            return OPERATION.BOOLEAN_COMMAND;
        } else {
            return OPERATION.SIMPLE_COMMAND;
        }
    }

    @SuppressWarnings("rawtypes")
    private Object getValueByType(String type, Object value, String path, MultiLevelMap data) {
        var selection = getMappingType(type);
        if (selection == OPERATION.SIMPLE_COMMAND) {
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
                case BOOLEAN_SUFFIX -> {
                    return TRUE.equalsIgnoreCase(String.valueOf(value));
                }
                case NEGATE_SUFFIX -> {
                    return !(TRUE.equalsIgnoreCase(String.valueOf(value)));
                }
                case INTEGER_SUFFIX -> {
                    return util.str2int(String.valueOf(value));
                }
                case LONG_SUFFIX -> {
                    return util.str2long(String.valueOf(value));
                }
                case FLOAT_SUFFIX -> {
                    return util.str2float(String.valueOf(value));
                }
                case DOUBLE_SUFFIX -> {
                    return util.str2double(String.valueOf(value));
                }
                case UUID_SUFFIX -> {
                    return util.getUuid4();
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
                default -> log.error("Unable to do {} of {} - matching type must be " +
                                    "substring(start, end), concat, boolean, !, and, or, text, binary, uuid or b64",
                                    type, path);
            }
        } else {
            String error = "missing close bracket";
            if (type.endsWith(CLOSE_BRACKET)) {
                String command = type.substring(type.indexOf('(') + 1, type.length() - 1).trim();
                /*
                 * substring(start, end)]
                 * substring(start)
                 * concat(parameter...) where parameters are model variable or text constant
                 * boolean(value=true)
                 * boolean(value) is same as boolean(value=true)
                 * and(model.anotherKey)
                 * or(model.anotherKey)
                 */
                if (selection == OPERATION.SUBSTRING_COMMAND) {
                    List<String> parts = util.split(command, ", ");
                    if (!parts.isEmpty() && parts.size() < 3) {
                        if (value instanceof String str) {
                            int start = util.str2int(parts.getFirst());
                            int end = parts.size() == 1 ? str.length() : util.str2int(parts.get(1));
                            if (end > start && start >= 0 && end <= str.length()) {
                                return str.substring(start, end);
                            } else {
                                error = "index out of bound";
                            }
                        } else {
                            error = "value is not a string";
                        }
                    } else {
                        error = "invalid syntax";
                    }
                } else if (selection == OPERATION.CONCAT_COMMAND) {
                    List<String> parts = tokenizeConcatParameters(command);
                    if (parts.isEmpty()) {
                        error = "parameters must be model variables and/or text constants";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        var str = String.valueOf(value);
                        sb.append(str);
                        for (String p: parts) {
                            if (p.startsWith(TEXT_TYPE)) {
                                sb.append(p, TEXT_TYPE.length(), p.length()-1);
                            }
                            if (p.startsWith(MODEL_NAMESPACE)) {
                                var v = String.valueOf(data.getElement(p));
                                sb.append(v);
                            }
                        }
                        return sb.toString();
                    }
                } else if (selection == OPERATION.AND_COMMAND || selection == OPERATION.OR_COMMAND) {
                    if (command.startsWith(MODEL_NAMESPACE) && command.length() > MODEL_NAMESPACE.length()) {
                        boolean v1 = TRUE.equals(String.valueOf(value));
                        boolean v2 = TRUE.equals(String.valueOf(data.getElement(command)));
                        return selection == OPERATION.AND_COMMAND ? v1 && v2 : v1 || v2;
                    } else {
                        error = "'" + command + "' is not a model variable";
                    }
                } else if (selection == OPERATION.BOOLEAN_COMMAND) {
                    List<String> parts = util.split(command, ",=");
                    List<String> filtered = new ArrayList<>();
                    parts.forEach(d -> {
                        var txt = d.trim();
                        if (!txt.isEmpty()) {
                            filtered.add(txt);
                        }
                    });
                    if (!filtered.isEmpty() && filtered.size() < 3) {
                        // Enforce value to a text string where null value will become "null".
                        // Therefore, null value or "null" string in the command is treated as the same.
                        String str = String.valueOf(value);
                        boolean condition = filtered.size() == 1 || TRUE.equalsIgnoreCase(filtered.get(1));
                        String target = filtered.getFirst();
                        if (str.equals(target)) {
                            return condition;
                        } else {
                            return !condition;
                        }
                    } else {
                        error = "invalid syntax";
                    }
                }
            }
            log.error("Unable to do {} of {} - {}", type, path, error);
        }
        return value;
    }

    private List<String> tokenizeConcatParameters(String text) {
        List<String> result = new ArrayList<>();
        var command = text.trim();
        while (!command.isEmpty()) {
            if (command.startsWith(MODEL_NAMESPACE)) {
                int sep = command.indexOf(',');
                if (sep == -1) {
                    result.add(command);
                    break;
                } else {
                    var token = command.substring(0, sep).trim();
                    if (token.equals(MODEL_NAMESPACE)) {
                        return Collections.emptyList();
                    } else {
                        result.add(token);
                        command = command.substring(sep + 1).trim();
                    }
                }
            } else if (command.startsWith(TEXT_TYPE)) {
                int close = command.indexOf(CLOSE_BRACKET);
                if (close == 1) {
                    return Collections.emptyList();
                } else {
                    result.add(command.substring(0, close+1));
                    int sep = command.indexOf(',', close);
                    if (sep == -1) {
                        break;
                    } else {
                        command = command.substring(sep+1).trim();
                    }
                }
            } else {
                return Collections.emptyList();
            }
        }
        return result;
    }

    private void setRhsElement(Object value, String rhs, MultiLevelMap target) {
        boolean updated = false;
        int colon = getModelTypeIndex(rhs);
        String selector = colon == -1? rhs : rhs.substring(0, colon).trim();
        if (colon != -1) {
            String type = rhs.substring(colon+1).trim();
            Object matched = getValueByType(type, value, "RHS '"+rhs+"'", target);
            target.setElement(selector, matched);
            updated = true;
        }
        if (!updated) {
            target.setElement(selector, value);
        }
    }

    private Object getConstantValue(String lhs) {
        int last = lhs.lastIndexOf(CLOSE_BRACKET);
        if (last > 0) {
            if (lhs.startsWith(TEXT_TYPE)) {
                return lhs.substring(TEXT_TYPE.length(), last);
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
            if (lhs.startsWith(MAP_TYPE)) {
                String ref = lhs.substring(MAP_TYPE.length(), last).trim();
                if (ref.contains("=") || ref.contains(",")) {
                    List<String> keyValues = util.split(ref, ",");
                    Map<String, Object> map = new HashMap<>();
                    for (String kv: keyValues) {
                        int eq = kv.indexOf('=');
                        String k = eq == -1? kv.trim() : kv.substring(0, eq).trim();
                        String v = eq == -1? "" : kv.substring(eq+1).trim();
                        if (!k.isEmpty()) {
                            map.put(k, v);
                        }
                    }
                    return map;
                } else {
                    return AppConfigReader.getInstance().get(ref);
                }
            }
            if (lhs.startsWith(FILE_TYPE)) {
                SimpleFileDescriptor fd = new SimpleFileDescriptor(lhs);
                File f = new File(fd.fileName);
                if (f.exists() && !f.isDirectory() && f.canRead()) {
                    return fd.binary? util.file2bytes(f) : util.file2str(f);
                }
            }
            if (lhs.startsWith(CLASSPATH_TYPE)) {
                SimpleFileDescriptor fd = new SimpleFileDescriptor(lhs);
                InputStream in = this.getClass().getResourceAsStream(fd.fileName);
                if (in != null) {
                    return fd.binary? util.stream2bytes(in) : util.stream2str(in);
                }
            }
        }
        return null;
    }

    private void setConstantValue(String lhs, String rhs, MultiLevelMap target) {
        Object value = getConstantValue(lhs);
        if (value != null) {
            setRhsElement(value, rhs, target);
        } else {
            removeModelElement(rhs, target);
        }
    }

    private static class SimpleFileDescriptor {
        public final String fileName;
        public final boolean binary;

        public SimpleFileDescriptor(String value) {
            int last = value.lastIndexOf(CLOSE_BRACKET);
            int offset = 0;
            if (value.startsWith(FILE_TYPE)) {
                offset = FILE_TYPE.length();
            } else if (value.startsWith(CLASSPATH_TYPE)) {
                offset = CLASSPATH_TYPE.length();
            }
            String name;
            final String filePath = value.substring(offset, last).trim();
            if (filePath.startsWith(TEXT_FILE)) {
                name = filePath.substring(TEXT_FILE.length());
                binary = false;
            } else if (filePath.startsWith(BINARY_FILE)) {
                name = filePath.substring(BINARY_FILE.length());
                binary = true;
            } else {
                // default fileType is binary
                name = filePath;
                binary = true;
            }
            fileName = name.startsWith("/")? name : "/" + name;
        }
    }

    private record TaskReference(String flowInstanceId, String processId) { }
}
