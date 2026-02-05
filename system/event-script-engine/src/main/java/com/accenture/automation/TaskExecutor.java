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

package com.accenture.automation;

import com.accenture.models.*;
import com.accenture.util.DataMappingHelper;
import com.accenture.models.SimpleFileDescriptor;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
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
    private static final DataMappingHelper helper = DataMappingHelper.getInstance();
    private static final ConcurrentMap<String, TaskReference> taskRefs = new ConcurrentHashMap<>();
    private static final Utility util = Utility.getInstance();
    private static final String DISTRIBUTED_TRACING = "distributed.tracing";
    private static final String FIRST_TASK = "first_task";
    private static final String FLOW_ID = "flow_id";
    private static final String PARENT = "parent";
    private static final String ROOT = "root";
    private static final String STATE_MACHINE = "state_machine";
    private static final String INPUT_MAPPING = "input_mapping";
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
    private static final String DATA_TYPE = "datatype";
    private static final String HEADER = "header";
    private static final String BODY = "body";
    private static final String RETRY = "@retry";
    private static final String TASK = "task";
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
    private static final String SIMPLE_PLUGIN_PREFIX = "f:";
    private static final String DOLLAR_TYPE = "$";
    private static final String FILE_TYPE = "file(";
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
    private static final String BOOLEAN_SUFFIX = "boolean";
    private static final String UUID_SUFFIX = "uuid";
    private static final String ITEM_SUFFIX = ".ITEM";
    private static final String INDEX_SUFFIX = ".INDEX";
    private final int maxModelArraySize;

    public TaskExecutor() {
        AppConfigReader config = AppConfigReader.getInstance();
        maxModelArraySize = util.str2int(config.getProperty("max.model.array.size", "1000"));
    }

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) {
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
                handleFunctionCallback(ref, event, flowInstance, refId, seq);
            }
        } catch (Exception e) {
            log.error("Unable to execute flow {}:{} - {}", flowName, flowInstance.id, e.getMessage());
            abortFlow(flowInstance, 500, e.getMessage());
        }
        return null;
    }

    private void handleFunctionCallback(TaskReference ref, EventEnvelope event, FlowInstance flowInstance,
                                           String refId, int seq) {
        String flowName = flowInstance.getFlow().id;
        String from = ref != null ? ref.processId : event.getFrom();
        if (from == null) {
            log.error("Unable to process callback {}:{} - task does not provide 'from' address", flowName, refId);
            return;
        }
        String caller = from.contains("@") ? from.substring(0, from.indexOf('@')) : from;
        Task task = flowInstance.getFlow().tasks.get(caller);
        if (task == null) {
            log.error("Unable to process callback {}:{} - missing task in {}", flowName, refId, caller);
            return;
        }
        if (ref != null) {
            var taskMetrics = flowInstance.metrics.get(ref.uuid);
            if (taskMetrics != null) {
                taskMetrics.complete();
            }
        }
        int statusCode = event.getStatus();
        if (statusCode >= 400 || event.isException()) {
            handleFunctionException(event, flowInstance, task, seq, statusCode);
            return;
        }
        // clear top level exception state
        flowInstance.setExceptionAtTopLevel(false);
        handleCallback(ref, from, flowInstance, task, event, seq);
    }

    private void handleFunctionException(EventEnvelope event, FlowInstance flowInstance, Task task,
                                         int seq, int statusCode) {
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
        boolean isTaskLevel = task.getExceptionTask() != null;
        String handler = isTaskLevel? task.getExceptionTask() : flowInstance.getFlow().exception;
        /*
         * Top level exception handler catches all unhandled exceptions.
         *
         * To exception loops at the top level exception handler,
         * abort the flow if top level exception handler throws exception.
         */
        if (handler != null && !flowInstance.topLevelExceptionHappened()) {
            if (!isTaskLevel) {
                flowInstance.setExceptionAtTopLevel(true);
            }
            Map<String, Object> error = new HashMap<>();
            error.put(TASK, task.service);
            error.put(CODE, statusCode);
            error.put(MESSAGE, event.getError());
            String stackTrace = event.getStackTrace();
            if (stackTrace != null) {
                error.put(STACK_TRACE, stackTrace);
            }
            executeTask(flowInstance, handler, error);
        } else {
            // when there are no task or flow exception handlers
            abortFlow(flowInstance, statusCode, event.getError());
        }
    }

    private void abortFlow(FlowInstance flowInstance, int status, Object message) {
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
        Flows.closeFlowInstance(flowInstance.id);
        // clean up task references and release memory
        flowInstance.metrics.keySet().forEach(taskRefs::remove);
        long diff = Math.max(0, System.currentTimeMillis() - flowInstance.getStartMillis());
        String formatted = Utility.getInstance().elapsedTime(diff);
        List<TaskMetrics> taskList = new ArrayList<>(flowInstance.tasks);
        List<Map<String, Object>> taskInfo = new ArrayList<>();
        taskList.forEach(info ->
                taskInfo.add(Map.of("name", info.getRoute(), "spent", info.getElapsed())));
        // clean up flowInstance states
        flowInstance.close();
        // Print event flow summary if tracing is enabled
        if (flowInstance.getTraceId() != null) {
            int totalExecutions = taskList.size();
            var payload = new HashMap<String, Object>();
            var metrics = new HashMap<String, Object>();
            var annotations = new HashMap<String, Object>();
            payload.put("trace", metrics);
            payload.put("annotations", annotations);
            metrics.put("origin", Platform.getInstance().getOrigin());
            metrics.put("id", flowInstance.getTraceId());
            metrics.put("service", TaskExecutor.SERVICE_NAME);
            metrics.put("from", EventScriptManager.SERVICE_NAME);
            metrics.put("exec_time", (float) diff);
            metrics.put("start", util.date2str(new Date(flowInstance.getStartMillis())));
            metrics.put("path", flowInstance.getTracePath());
            metrics.put(STATUS, normal ? 200 : 400);
            metrics.put("success", normal);
            if (!normal) {
                metrics.put("exception", "Flow aborted");
            }
            annotations.put("execution", "Run " + totalExecutions +
                    " task" + (totalExecutions == 1 ? "" : "s") + " in " + formatted);
            annotations.put("tasks", taskInfo);
            annotations.put("flow", flowInstance.getFlow().id);
            EventEmitter.getInstance().send(new EventEnvelope().setTo(DISTRIBUTED_TRACING).setBody(payload));
        }
    }

    private void handleCallback(TaskReference ref, String from, FlowInstance flowInstance, Task task, EventEnvelope event, int seq) {
        Map<String, Object> combined = new HashMap<>();
        combined.put(INPUT, flowInstance.dataset.get(INPUT));
        combined.put(MODEL, flowInstance.dataset.get(MODEL));
        combined.put(STATUS, event.getStatus());
        combined.put(HEADER, event.getHeaders());
        combined.put(RESULT, event.getRawBody());
        if (event.getType() != null) {
            combined.put(DATA_TYPE, event.getType());
        }
        // consolidated dataset includes input, model and task result set
        var md = new OutputMappingMetadata(combined);
        performOutputDataMapping(md, flowInstance, task);
        if (seq > 0 && flowInstance.pipeMap.containsKey(seq)) {
            PipeInfo pipe = flowInstance.pipeMap.get(seq);
            // this is a callback from a fork task
            if (JOIN.equals(pipe.getType())) {
                handleCallbackFromForkTask(from, (JoinTaskInfo) pipe, flowInstance, seq);
                return;
            }
            // this is a callback from a pipeline task
            if (PIPELINE.equals(pipe.getType())) {
                handlePipeline(md, flowInstance, (PipelineInfo) pipe, seq);
                return;
            }
        }
        String executionType = task.execution;
        // consolidated dataset would be mapped as output for "response", "end" and "decision" tasks
        if (RESPONSE.equals(executionType)) {
            handleResponseTask(flowInstance, task, md.consolidated);
        }
        if (END.equals(executionType)) {
            handleEndTask(flowInstance, task, md.consolidated);
        }
        if (DECISION.equals(executionType)) {
            handleDecisionTask(ref, flowInstance, task, md.consolidated.getElement(DECISION));
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
            handlePipelineTask(flowInstance, task, md.consolidated);
        }
    }

    private void performOutputDataMapping(OutputMappingMetadata md, FlowInstance flowInstance, Task task) {
        /*
         * Java virtual thread system is backed by multiple kernel threads.
         * Therefore, to ensure the state machine is updated in a thread safe manner,
         * this block applies a thread-safety lock per flow instance.
         */
        var ancestor = flowInstance.resolveAncestor();
        var useParentModel = task.hasOutputParentRef() && !ancestor.id.equals(flowInstance.id);
        flowInstance.modelSafety.lock();
        if (useParentModel) {
            ancestor.modelSafety.lock();
        }
        try {
            List<String> mapping = task.output;
            for (String entry : mapping) {
                md.sep = entry.lastIndexOf(MAP_TO);
                if (md.sep > 0) {
                    doOutputDataMappingEntry(md, entry, flowInstance, task);
                }
            }
        } finally {
            if (useParentModel) {
                ancestor.modelSafety.unlock();
            }
            flowInstance.modelSafety.unlock();
        }
        // has output data mapping monitor?
        var monitor = task.getMonitorAfterTask();
        if (monitor != null) {
            EventEmitter.getInstance().send(monitor, filterModelRoot(md.consolidated.getMap()));
        }
    }

    private void doOutputDataMappingEntry(OutputMappingMetadata md, String entry,
                                          FlowInstance flowInstance, Task task) {
        final Object value = getOutputDataMappingLhsValue(md, entry);
        if (md.rhs.startsWith(FILE_TYPE)) {
            setOutputDataMappingFile(md, value);
        } else {
            if (value != null) {
                setOutputDataMappingRhs(md, value, entry, flowInstance, task);
            } else {
                if (md.rhs.startsWith(EXT_NAMESPACE)) {
                    callExternalStateMachine(flowInstance, task, md.rhs, null);
                }
            }
        }
    }

    private void handlePipeline(OutputMappingMetadata md, FlowInstance flowInstance, PipelineInfo pipeline, int seq) {
        Task pipelineTask = pipeline.getTask();
        if (pipeline.isCompleted()) {
            pipelineCompletion(flowInstance, pipeline, md.consolidated, seq);
            return;
        }
        int n = pipeline.nextStep();
        var taskName = pipeline.getTaskName(n);
        if (pipeline.isLastStep(n)) {
            pipeline.setCompleted();
            log.debug("Flow {}:{} pipeline #{} last step-{} {}",
                    flowInstance.getFlow().id, flowInstance.id, seq, n + 1, taskName);
        } else {
            log.debug("Flow {}:{} pipeline #{} next step-{} {}",
                    flowInstance.getFlow().id, flowInstance.id, seq, n + 1, taskName);
        }
        if (pipelineTask.conditions.isEmpty()) {
            if (pipeline.isCompleted() && pipeline.isSingleton()) {
                pipelineCompletion(flowInstance, pipeline, md.consolidated, seq);
            } else {
                executeTask(flowInstance, pipeline.getTaskName(n), seq);
            }
        } else {
            evaluatePipelineCondition(md, flowInstance, pipelineTask, pipeline, seq, n);
        }
    }

    private void evaluatePipelineCondition(OutputMappingMetadata md, FlowInstance flowInstance, Task pipelineTask,
                                           PipelineInfo pipeline, int seq, int n) {
        String action = null;
        for (List<String> condition: pipelineTask.conditions) {
            /*
             * The first element of a loop condition is the model key.
             * The second element is "continue" or "break".
             */
            var resolved = resolveCondition(condition, md.consolidated);
            if (resolved != null) {
                action = resolved;
                if (CONTINUE.equals(resolved)) {
                    // clear condition
                    md.consolidated.setElement(condition.getFirst(), false);
                }
                break;
            }
        }
        if (BREAK.equals(action)) {
            flowInstance.pipeMap.remove(seq);
            executeTask(flowInstance, pipeline.getExitTask());
        } else if (CONTINUE.equals(action)) {
            pipelineCompletion(flowInstance, pipeline, md.consolidated, seq);
        } else {
            if (pipeline.isCompleted() && pipeline.isSingleton()) {
                pipelineCompletion(flowInstance, pipeline, md.consolidated, seq);
            } else {
                executeTask(flowInstance, pipeline.getTaskName(n), seq);
            }
        }
    }

    private void handleCallbackFromForkTask(String from, JoinTaskInfo joinInfo, FlowInstance flowInstance, int seq) {
        int callBackCount = joinInfo.resultCount.incrementAndGet();
        log.debug("Flow {}:{} fork-n-join #{} result {} of {} from {}",
                flowInstance.getFlow().id, flowInstance.id, seq, callBackCount, joinInfo.forks, from);
        if (callBackCount >= joinInfo.forks) {
            flowInstance.pipeMap.remove(seq);
            log.debug("Flow {}:{} fork-n-join #{} done", flowInstance.getFlow().id, flowInstance.id, seq);
            executeTask(flowInstance, joinInfo.joinTask);
        }
    }

    private Object getOutputDataMappingLhsValue(OutputMappingMetadata md, String entry) {
        md.lhs = substituteDynamicIndex(entry.substring(0, md.sep).trim(), md.consolidated, false);
        md.rhs = substituteDynamicIndex(entry.substring(md.sep+2).trim(), md.consolidated, true);
        final Object value;
        boolean isInput = md.lhs.startsWith(INPUT_NAMESPACE) || md.lhs.equalsIgnoreCase(INPUT);
        if (isInput || md.lhs.startsWith(MODEL_NAMESPACE) ||
                md.lhs.startsWith(DOLLAR_TYPE) ||
                md.lhs.equals(HEADER) || md.lhs.startsWith(HEADER_NAMESPACE) ||
                md.lhs.equals(STATUS) || md.lhs.equals(DATA_TYPE) ||
                md.lhs.equals(RESULT) || md.lhs.startsWith(RESULT_NAMESPACE)) {
            value = helper.getLhsElement(md.lhs, md.consolidated);
            if (value == null) {
                removeModelElement(md.rhs, md.consolidated);
            }
        } else {
            value = helper.getConstantValue(md.lhs);
        }
        return value;
    }

    private void setOutputDataMappingFile(OutputMappingMetadata md, Object value) {
        SimpleFileDescriptor fd = new SimpleFileDescriptor(md.rhs);
        boolean append = fd.mode == SimpleFileDescriptor.FILE_MODE.APPEND;
        File f = new File(fd.fileName);
        // automatically create parent folder
        boolean fileFound = f.exists();
        if (!fileFound) {
            createParentFolder(f);
        }
        if (!fileFound || (!f.isDirectory() && f.canWrite())) {
            switch (value) {
                // delete the RHS' target file if LHS value is null
                case null -> {
                    if (fileFound) {
                        try {
                            Files.delete(f.toPath());
                        } catch (IOException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                }
                case byte[] b -> util.bytes2file(f, b, append);
                case String str -> util.str2file(f, str, append);
                // best effort to save as a JSON string
                case List<?> list ->
                        util.str2file(f, SimpleMapper.getInstance().getMapper().writeValueAsString(list), append);
                case Map<?, ?> map ->
                        util.str2file(f, SimpleMapper.getInstance().getMapper().writeValueAsString(map), append);
                default -> util.str2file(f, String.valueOf(value), append);
            }
        }
    }

    private void setOutputDataMappingRhs(OutputMappingMetadata md, Object value, String entry,
                                         FlowInstance flowInstance, Task task) {
        boolean required = true;
        if (md.rhs.equals(OUTPUT_STATUS)) {
            int status = value instanceof Integer v? v : util.str2int(String.valueOf(value));
            if (status < 100 || status > 599) {
                log.error("Invalid output mapping '{}' - expect: valid HTTP status code, actual: {}",
                        entry, status);
                required = false;
            }
        }
        if (md.rhs.equals(OUTPUT_HEADER) && !(value instanceof Map)) {
            log.error("Invalid output mapping '{}' - expect: Map, actual: {}",
                    entry, value.getClass().getSimpleName());
            required = false;
        }
        if (md.rhs.startsWith(EXT_NAMESPACE)) {
            required = false;
            callExternalStateMachine(flowInstance, task, md.rhs, value);
        }
        if (required) {
            setRhsElement(value, md.rhs, md.consolidated);
        }
    }

    private void createParentFolder(File f) {
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

    private String resolveCondition(List<String> condition, MultiLevelMap consolidated) {
        if (Boolean.TRUE.equals(consolidated.getElement(condition.getFirst()))) {
            return condition.get(1);
        } else {
            return null;
        }
    }

    private void pipelineCompletion(FlowInstance flowInstance, PipelineInfo pipeline,
                                    MultiLevelMap consolidated, int seq) {
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
            iterate = evaluateForCondition(consolidated, pipelineTask.comparator.getFirst(),
                                            pipelineTask.comparator.get(1), pipelineTask.comparator.get(2));
        }
        if (iterate) {
            pipeline.resetPointer();
            var taskName = pipeline.getTaskName(0);
            log.debug("Flow {}:{} pipeline #{} loop {}",
                    flowInstance.getFlow().id, flowInstance.id, seq, taskName);
            executeTask(flowInstance, pipeline.getTaskName(0), seq);
        } else {
            flowInstance.pipeMap.remove(seq);
            executeTask(flowInstance, pipeline.getExitTask());
        }
    }

    private void handleResponseTask(FlowInstance flowInstance, Task task, MultiLevelMap map) {
        sendResponse(flowInstance, task, map);
        queueSequentialTask(flowInstance, task);
    }

    private void handleEndTask(FlowInstance flowInstance, Task task, MultiLevelMap map) {
        sendResponse(flowInstance, task, map);
        endFlow(flowInstance, true);
    }

    private void handleDecisionTask(TaskReference ref, FlowInstance flowInstance, Task task, Object decisionValue) {
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
            decideNextTask(ref, flowInstance, task, nextTasks, decisionNumber);
        }
    }

    private void decideNextTask(TaskReference ref, FlowInstance flowInstance, Task task,
                                List<String> nextTasks, int decisionNumber) {
        var nextList = getDecision(nextTasks.get(decisionNumber - 1));
        if (decisionNumber == 1 && RETRY.equals(nextList.getFirst())) {
            if (ref.errorTask != null) {
                executeTask(flowInstance, ref.errorTask);
            } else if (nextList.size() > 1) {
                executeTask(flowInstance, nextList.get(1));
            } else {
                log.error("Flow {}:{} {} does not have a previous task {}",
                        flowInstance.getFlow().id, flowInstance.id, task.service, nextList);
                abortFlow(flowInstance, 500,
                        "Task "+task.service+" does not have a previous task "+nextList);
            }
        } else {
            executeTask(flowInstance, nextList.getFirst());
        }
    }

    private List<String> getDecision(String text) {
        if (text.contains("|")) {
            var nextList = util.split(text, "| ");
            Collections.sort(nextList);
            return nextList;
        } else {
            return List.of(text);
        }
    }

    private void queueSequentialTask(FlowInstance flowInstance, Task task) {
        List<String> nextTasks = task.nextSteps;
        if (!nextTasks.isEmpty()) {
            executeTask(flowInstance, nextTasks.getFirst());
        }
    }

    private void queueParallelTasks(FlowInstance flowInstance, Task task) {
        List<String> nextTasks = task.nextSteps;
        if (!nextTasks.isEmpty()) {
            for (String next: nextTasks) {
                executeTask(flowInstance, next);
            }
        }
    }

    private void handleForkAndJoin(FlowInstance flowInstance, Task task) {
        List<String> steps = new ArrayList<>(task.nextSteps);
        boolean isList = false;
        var dynamicListKey = task.getSourceModelKey();
        if (dynamicListKey != null && !dynamicListKey.isBlank() && steps.size() == 1) {
            Map<String, Object> modelOnly = new HashMap<>();
            modelOnly.put(MODEL, flowInstance.dataset.get(MODEL));
            MultiLevelMap model = new MultiLevelMap(modelOnly);
            var o = model.getElement(dynamicListKey);
            if (o instanceof List<?> list) {
                isList = true;
                if (list.size() > 1) {
                    var singleStep = task.nextSteps.getFirst();
                    for (int i=1; i < list.size(); i++) {
                        steps.add(singleStep);
                    }
                }
            } else {
                throw new IllegalArgumentException("Flow "+flowInstance.getFlow().id+":"+flowInstance.id +
                            " " + task.service + " - " + dynamicListKey + " is not a list");
            }
        }
        if (!steps.isEmpty() && task.getJoinTask() != null) {
            executeForkAndJoin(flowInstance, task, steps, isList, dynamicListKey);
        }
    }

    private void executeForkAndJoin(FlowInstance flowInstance, Task task, List<String> steps,
                                    boolean isList, String dynamicListKey) {
        int seq = flowInstance.pipeCounter.incrementAndGet();
        int forks = steps.size();
        flowInstance.pipeMap.put(seq, new JoinTaskInfo(forks, task.getJoinTask()));
        for (int i = 0; i < steps.size(); i++) {
            String next = steps.get(i);
            executeTask(flowInstance, next, seq, isList ? i : -1, isList ? dynamicListKey : null);
        }
    }

    private boolean evaluateForCondition(MultiLevelMap mm, String modelVar1, String comparator, String modelVar2) {
        Object value1 = modelVar1.startsWith(MODEL_NAMESPACE)? mm.getElement(modelVar1) : modelVar1;
        Object value2 = modelVar2.startsWith(MODEL_NAMESPACE)? mm.getElement(modelVar2) : modelVar2;
        int v1 = value1 instanceof Integer? (int) value1 : util.str2int(String.valueOf(value1));
        int v2 = value2 instanceof Integer? (int) value2 : util.str2int(String.valueOf(value2));
        return switch (comparator) {
            case "<" -> v1 < v2;
            case "<=" -> v1 <= v2;
            case ">" -> v1 > v2;
            case ">=" -> v1 >= v2;
            case null, default -> false;
        };
    }

    private void handlePipelineTask(FlowInstance flowInstance, Task task, MultiLevelMap map) {
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
                valid = evaluateForCondition(map,
                            task.comparator.getFirst(), task.comparator.get(1), task.comparator.get(2));
            }
            if (valid) {
                int seq = flowInstance.pipeCounter.incrementAndGet();
                PipelineInfo pipeline = new PipelineInfo(task);
                flowInstance.pipeMap.put(seq, pipeline);
                pipeline.resetPointer();
                var taskName = pipeline.getTaskName(0);
                log.debug("Flow {}:{} pipeline #{} begin {}",
                        flowInstance.getFlow().id, flowInstance.id, seq, taskName);
                executeTask(flowInstance, pipeline.getTaskName(0), seq);
            } else {
                executeTask(flowInstance, task.nextSteps.getFirst());
            }
        }
    }

    private void sendResponse(FlowInstance flowInstance, Task task, MultiLevelMap map) {
        PostOffice po = new PostOffice(TaskExecutor.SERVICE_NAME, flowInstance.getTraceId(), flowInstance.getTracePath());
        if (flowInstance.isNotResponded()) {
            flowInstance.setResponded(true);
            // is a response event required when the flow is completed?
            if (flowInstance.replyTo != null) {
                sendResponseFromTask(po, flowInstance, task, map);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sendResponseFromTask(PostOffice po, FlowInstance flowInstance, Task task, MultiLevelMap map) {
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

    private void executeTask(FlowInstance flowInstance, String processName) {
        executeTask(flowInstance, processName, -1);
    }

    private void executeTask(FlowInstance flowInstance, String processName, int seq) {
        executeTask(flowInstance, processName, seq, null, -1, null);
    }

    private void executeTask(FlowInstance flowInstance, String processName, Map<String, Object> error) {
        executeTask(flowInstance, processName, -1, error, -1, null);
    }

    private void executeTask(FlowInstance flowInstance, String processName, int seq, int dynamicListIndex, String dynamicListKey) {
        executeTask(flowInstance, processName, seq, null, dynamicListIndex, dynamicListKey);
    }

    private void executeTask(FlowInstance flowInstance, String processName, int seq, Map<String, Object> error, int dynamicListIndex, String dynamicListKey) {
        var task = flowInstance.getFlow().tasks.get(processName);
        if (task == null) {
            log.error("Unable to process flow {}:{} - missing task '{}'",
                    flowInstance.getFlow().id, flowInstance.id, processName);
            abortFlow(flowInstance, 500, SERVICE_AT +processName+" not defined");
            return;
        }
        String errorTask = null;
        Map<String, Object> combined = new HashMap<>();
        combined.put(INPUT, flowInstance.dataset.get(INPUT));
        combined.put(MODEL, flowInstance.dataset.get(MODEL));
        if (error != null && error.get(TASK) instanceof String et) {
            errorTask = et;
            combined.put(ERROR, error);
        }
        var md = new InputMappingMetadata(combined);
        performInputDataMapping(md, flowInstance, task, dynamicListIndex, dynamicListKey);
        // need to send later?
        long deferred = 0;
        if (task.getDelay() > 0) {
            deferred = task.getDelay();
        } else if (task.getDelayVar() != null) {
            deferred = getDelayedVariable(md, flowInstance, task);
        }
        final var uuid = util.getDateUuid();
        final var ref = new TaskReference(uuid, flowInstance.id, task.service, errorTask);
        taskRefs.put(uuid, ref);
        // add task metrics and pending status to the flow-instance
        var functionRoute = task.getFunctionRoute();
        var taskMetrics = new TaskMetrics(task.service, functionRoute);
        flowInstance.metrics.put(uuid, taskMetrics);
        flowInstance.tasks.add(taskMetrics);
        final var compositeCid = seq > 0? uuid + "#" + seq : uuid;
        if (functionRoute.startsWith(FLOW_PROTOCOL)) {
            var flowId = functionRoute.substring(FLOW_PROTOCOL.length());
            var subFlow = Flows.getFlow(flowId);
            if (subFlow == null) {
                log.error("Unable to process flow {}:{} - missing sub-flow {}",
                        flowInstance.getFlow().id, flowInstance.id, functionRoute);
                abortFlow(flowInstance, 500, functionRoute+" not defined");
                return;
            }
            Map<String, Object> dataset = new HashMap<>();
            dataset.put(BODY, unwrapBodyIfWildcard(md));
            if (!md.optionalHeaders.isEmpty()) {
                dataset.put(HEADER, md.optionalHeaders);
            }
            // execute a subflow
            var forward = new EventEnvelope().setTo(EventScriptManager.SERVICE_NAME)
                                                .setReplyTo(TaskExecutor.SERVICE_NAME)
                                                .setHeader(PARENT, flowInstance.id)
                                                .setHeader(FLOW_ID, flowId).setBody(dataset)
                                                .setCorrelationId(compositeCid);
            var po = new PostOffice(functionRoute, flowInstance.getTraceId(), flowInstance.getTracePath());
            po.send(forward);
        } else {
            var po = new PostOffice(TaskExecutor.SERVICE_NAME,
                                            flowInstance.getTraceId(), flowInstance.getTracePath());
            var event = new EventEnvelope().setTo(functionRoute).setReplyTo(TaskExecutor.SERVICE_NAME)
                                            .setCorrelationId(compositeCid)
                                            .setBody(unwrapBodyIfWildcard(md));
            md.optionalHeaders.forEach(event::setHeader);
            // execute task by sending event
            if (deferred > 0) {
                po.sendLater(event, new Date(System.currentTimeMillis() + deferred));
            } else {
                po.send(event);
            }
        }
    }

    private Object unwrapBodyIfWildcard(InputMappingMetadata md){
        var map = md.target.getMap();
        var value = map.get(ALL);
        return value != null ? value : map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filterModelRoot(Map<String, Object> stateMachine) {
        // do a shallow copy and remove the alias model.root
        Map<String, Object> result = new HashMap<>(stateMachine);
        var original = result.get(MODEL);
        if (original instanceof Map) {
            Map<String, Object> filtered = new HashMap<>((Map<String, Object>) original);
            filtered.remove(ROOT);
            result.put(MODEL, filtered);
        }
        return result;
    }

    private long getDelayedVariable(InputMappingMetadata md, FlowInstance flowInstance, Task task) {
        Object d = md.source.getElement(task.getDelayVar());
        if (d != null) {
            long delay = Math.max(1, util.str2long(d.toString()));
            if (delay < flowInstance.getFlow().ttl) {
                return delay;
            } else {
                log.warn("Unable to schedule future task for {} because {} is invalid (TTL={}, delay={})",
                        task.service, task.getDelayVar(), flowInstance.getFlow().ttl, delay);
            }
        } else {
            log.warn("Unable to schedule future task for {} because {} does not exist",
                    task.service, task.getDelayVar());
        }
        return 0;
    }

    private void performInputDataMapping(InputMappingMetadata md, FlowInstance flowInstance, Task task,
                                         int dynamicListIndex, String dynamicListKey) {
        /*
         * Java virtual thread system is backed by multiple kernel threads.
         * Therefore, to ensure the state machine is updated in a thread safe manner,
         * this block applies a thread-safety lock per flow instance.
         */
        var ancestor = flowInstance.resolveAncestor();
        var useParentModel = task.hasInputParentRef() && !ancestor.id.equals(flowInstance.id);
        flowInstance.modelSafety.lock();
        if (useParentModel) {
            ancestor.modelSafety.lock();
        }
        try {
            List<String> mapping = task.input;
            for (String entry: mapping) {
                int sep = entry.lastIndexOf(MAP_TO);
                if (sep > 0) {
                    doInputDataMappingEntry(md, flowInstance, task, entry, sep, dynamicListIndex, dynamicListKey);
                }
            }
        } finally {
            if (useParentModel) {
                ancestor.modelSafety.unlock();
            }
            flowInstance.modelSafety.unlock();
        }
        // has input data mapping monitor?
        var monitor = task.getMonitorBeforeTask();
        if (monitor != null) {
            EventEmitter.getInstance().send(monitor,
                    Map.of(STATE_MACHINE, filterModelRoot(flowInstance.dataset),
                            HEADER, md.optionalHeaders,
                            INPUT_MAPPING, md.target.getMap()));
        }
    }

    private void doInputDataMappingEntry(InputMappingMetadata md, FlowInstance flowInstance, Task task,
                                         String entry, int sep, int dynamicListIndex, String dynamicListKey) {
        md.lhs = substituteDynamicIndex(entry.substring(0, sep).trim(), md.source, false);
        md.rhs = substituteDynamicIndex(entry.substring(sep+2).trim(), md.source, true);
        boolean inputLike = md.lhs.startsWith(INPUT_NAMESPACE) || md.lhs.equalsIgnoreCase(INPUT) ||
                md.lhs.equals(DATA_TYPE) ||
                md.lhs.startsWith(MODEL_NAMESPACE) || md.lhs.startsWith(ERROR_NAMESPACE) ||
                md.lhs.startsWith(SIMPLE_PLUGIN_PREFIX) ||
                md.lhs.startsWith(DOLLAR_TYPE);
        if (md.lhs.startsWith(INPUT_HEADER_NAMESPACE)) {
            md.lhs = md.lhs.toLowerCase();
        }
        final Object value = inputLike?
                        getInputDataMappingLhsValue(md, dynamicListIndex, dynamicListKey) : helper.getConstantValue(md.lhs);
        if (md.rhs.startsWith(EXT_NAMESPACE)) {
            callExternalStateMachine(flowInstance, task, md.rhs, value);
        } else if (md.rhs.startsWith(MODEL_NAMESPACE)) {
            setInputDataMappingModelVar(md, flowInstance, value, inputLike);
        } else if (inputLike) {
            if (value != null) {
                setInputDataMappingRhs(entry, md, value);
            }
        } else {
            setInputDataMappingRhsAsConstant(md);
        }
    }

    private void setInputDataMappingModelVar(InputMappingMetadata md, FlowInstance flowInstance,
                                             Object value, boolean inputLike) {
        Map<String, Object> modelOnly = new HashMap<>();
        modelOnly.put(MODEL, flowInstance.dataset.get(MODEL));
        MultiLevelMap model = new MultiLevelMap(modelOnly);
        if (inputLike) {
            if (value == null) {
                removeModelElement(md.rhs, model);
            } else {
                setRhsElement(value, md.rhs, model);
            }
        } else {
            setConstantValue(md.lhs, md.rhs, model);
        }
    }

    private void setInputDataMappingRhsAsConstant(InputMappingMetadata md) {
        // Assume left hand side is a constant
        if (md.rhs.startsWith(HEADER_NAMESPACE)) {
            String k = md.rhs.substring(HEADER_NAMESPACE.length());
            Object v = helper.getConstantValue(md.lhs);
            if (!k.isEmpty() && v != null) {
                md.optionalHeaders.put(k, v.toString());
            }
        } else {
            setConstantValue(md.lhs, md.rhs, md.target);
        }
    }

    private Object getInputDataMappingLhsValue(InputMappingMetadata md, int dynamicListIndex, String dynamicListKey) {
        Object value = helper.getLhsElement(md.lhs, md.source);
        // special cases for simple type matching for a non-exist model variable
        if (value == null && md.lhs.startsWith(MODEL_NAMESPACE)) {
            value = getValueFromNonExistModel(md.lhs);
        }
        // special case for a dynamic list in fork and join
        if (value == null && dynamicListKey != null) {
            if (md.lhs.equals(dynamicListKey + ITEM_SUFFIX)) {
                value = getDynamicListItem(dynamicListKey, dynamicListIndex, md.source);
            }
            if (md.lhs.equals(dynamicListKey + INDEX_SUFFIX)) {
                value = dynamicListIndex;
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private void setInputDataMappingRhs(String entry, InputMappingMetadata md, Object value) {
        boolean valid = true;
        if (ALL.equals(md.rhs)) {
            if (value instanceof Map) {
                md.target.reload((Map<String, Object>) value);
            } else {
                md.target.setElement(ALL, value);
            }
        } else if (md.rhs.equals(HEADER)) {
            if (value instanceof Map) {
                Map<String, Object> headers = (Map<String, Object>) value;
                headers.forEach((k,v) -> md.optionalHeaders.put(k, v.toString()));
            } else {
                valid = false;
            }
        } else if (md.rhs.startsWith(HEADER_NAMESPACE)) {
            String k = md.rhs.substring(HEADER_NAMESPACE.length());
            if (!k.isEmpty()) {
                md.optionalHeaders.put(k, value.toString());
            }
        } else {
            setRhsElement(value, md.rhs, md.target);
        }
        if (!valid) {
            log.error("Invalid input mapping '{}' - expect: Map, actual: {}", entry, value.getClass().getSimpleName());
        }
    }

    private Object getValueFromNonExistModel(String lhs) {
        int colon = lhs.lastIndexOf(':');
        if (colon > 0) {
            var qualifier = lhs.substring(colon+1).trim();
            if (UUID_SUFFIX.equals(qualifier)) {
                return util.getUuid4();
            } else {
                var v = getNullBooleanValue(qualifier);
                if (v == 1) {
                    return true;
                }
                if (v == 2) {
                    return false;
                }
            }
        }
        return null;
    }

    private int getNullBooleanValue(String qualifier) {
        var parts = util.split(qualifier, "(= )");
        if (parts.size() == 3 && BOOLEAN_SUFFIX.equals(parts.getFirst()) && NULL.equals(parts.get(1))) {
            if (TRUE.equals(parts.get(2))) {
                return 1;
            }
            if (FALSE.equals(parts.get(2))) {
                return 2;
            }
        }
        return 0;
    }

    private void callExternalStateMachine(FlowInstance flowInstance, Task task, String rhs, Object value) {
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
            Object value = helper.getValueByType(type, null, "?", model);
            if (value != null) {
                setRhsElement(value, key, model);
            } else {
                model.removeElement(key);
            }
        } else {
            model.removeElement(rhs);
        }
    }

    private Object getDynamicListItem(String dynamicListKey, int dynamicListIndex, MultiLevelMap source) {
        Object value = source.getElement(dynamicListKey);
        if (value instanceof List<?> list) {
            return list.get(dynamicListIndex);
        }
        return value;
    }

    private String getStringFromModelValue(Object obj) {
        return obj instanceof String || obj instanceof Number? String.valueOf(obj) : "null";
    }

    private int replaceWithRuntimeVar(VarSegment s, StringBuilder sb, int start, String text, MultiLevelMap source) {
        String heading = text.substring(start, s.start());
        if (!heading.isEmpty()) {
            sb.append(heading);
        }
        String middle = text.substring(s.start() + 1, s.end() - 1).trim();
        if (middle.startsWith(MODEL_NAMESPACE) && !middle.endsWith(".")) {
            sb.append(getStringFromModelValue(source.getElement(middle)));
        } else {
            sb.append(text, s.start(), s.end());
        }
        return s.end();
    }

    protected final String substituteRuntimeVarsIfAny(String text, MultiLevelMap source) {
        if (text.contains("{") && text.contains("}")) {
            List<VarSegment> segments = util.extractSegments(text, "{", "}");
            if (segments.isEmpty()) {
                return text;
            } else {
                int start = 0;
                StringBuilder sb = new StringBuilder();
                for (VarSegment s : segments) {
                    start = replaceWithRuntimeVar(s, sb, start, text, source);
                }
                String lastSegment = text.substring(start);
                if (!lastSegment.isEmpty()) {
                    sb.append(lastSegment);
                }
                return sb.toString();
            }
        } else {
            return text;
        }
    }

    private int scanDynamicIndex(StringBuilder sb, String text, MultiLevelMap source, boolean isRhs, int start) {
        int open = text.indexOf('[', start);
        int close = text.indexOf(']', start);
        if (open != -1 && close > open) {
            sb.append(text, start, open+1);
            var idx = text.substring(open+1, close).trim();
            if (idx.startsWith(MODEL_NAMESPACE) && !idx.endsWith(".")) {
                resolveModelIndex(sb, text, source, idx, isRhs);
            } else {
                validateNumericIndex(sb, text, idx, isRhs);
            }
            sb.append(']');
            return close + 1;
        } else {
            // scan completed
            sb.append(text, start, text.length());
            return text.length();
        }
    }

    private String substituteDynamicIndex(String statement, MultiLevelMap source, boolean isRhs) {
        String text = substituteRuntimeVarsIfAny(statement, source);
        if (text.contains("[") && text.contains("]")) {
            StringBuilder sb = new StringBuilder();
            int start = 0;
            while (start < text.length()) {
                start = scanDynamicIndex(sb, text, source, isRhs, start);
            }
            return sb.toString();
        } else {
            return text;
        }
    }

    private void validateNumericIndex(StringBuilder sb, String text, String idx, boolean isRhs) {
        if (isRhs && !idx.isEmpty()) {
            int ptr = util.str2int(idx);
            if (ptr < 0) {
                throw new IllegalArgumentException("Cannot set RHS to negative index - " + text);
            }
        }
        sb.append(idx);
    }

    private void resolveModelIndex(StringBuilder sb, String text, MultiLevelMap source,
                                   String modelIndex, boolean isRhs) {
        int ptr = util.str2int(getStringFromModelValue(source.getElement(modelIndex)));
        if (isRhs) {
            if (ptr > maxModelArraySize) {
                throw new IllegalArgumentException("Cannot set RHS to index > " + ptr
                        + " that exceeds max "+maxModelArraySize+" - "+text);
            }
            if (ptr < 0) {
                throw new IllegalArgumentException("Cannot set RHS to negative index - " + text);
            }
        }
        sb.append(ptr);
    }

    private int getModelTypeIndex(String text) {
        if (text.startsWith(MODEL_NAMESPACE)) {
            return text.indexOf(':');
        } else {
            return -1;
        }
    }

    @SuppressWarnings("unchecked")
    private void setRhsElement(Object value, String rhs, MultiLevelMap target) {
        int colon = getModelTypeIndex(rhs);
        String selector = colon == -1? rhs : rhs.substring(0, colon).trim();
        if (colon != -1) {
            String type = rhs.substring(colon+1).trim();
            Object matched = helper.getValueByType(type, value, "RHS '"+rhs+"'", target);
            target.setElement(selector, matched);
        } else {
            if (selector.startsWith(MODEL_NAMESPACE)) {
                if (value instanceof Map) {
                    target.setElement(selector, util.deepCopy((Map<String, Object>) value));
                    return;
                }
                if (value instanceof List) {
                    target.setElement(selector, util.deepCopy((List<Object>) value));
                    return;
                }
            }
            target.setElement(selector, value);
        }
    }

    private void setConstantValue(String lhs, String rhs, MultiLevelMap target) {
        Object value = helper.getConstantValue(lhs);
        if (value != null) {
            setRhsElement(value, rhs, target);
        } else {
            removeModelElement(rhs, target);
        }
    }

    private record TaskReference(String uuid, String flowInstanceId, String processId, String errorTask) { }

    private static class OutputMappingMetadata {
        MultiLevelMap consolidated;
        int sep;
        String lhs;
        String rhs;

        OutputMappingMetadata(Map<String, Object> combined) {
            this.consolidated = new MultiLevelMap(combined);
        }
    }

    private static class InputMappingMetadata {
        MultiLevelMap source;
        MultiLevelMap target = new MultiLevelMap();
        Map<String, String> optionalHeaders = new HashMap<>();
        String lhs;
        String rhs;

        InputMappingMetadata(Map<String, Object> combined) {
            this.source = new MultiLevelMap(combined);
        }
    }
}
