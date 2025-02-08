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

package org.platformlambda.core.system

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.platformlambda.core.exception.AppException
import org.platformlambda.core.models.AsyncHttpRequest
import org.platformlambda.core.models.EventEnvelope
import org.platformlambda.core.models.MappingExceptionHandler
import org.platformlambda.core.models.ProcessStatus
import org.platformlambda.core.serializers.SimpleMapper
import org.platformlambda.core.services.DistributedTrace
import org.platformlambda.core.services.TemporaryInbox
import org.platformlambda.core.util.Utility
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import kotlin.math.max

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
class WorkerQueue(def: ServiceDef, route: String, private val instance: Int) : WorkerQueues(def, route) {
    private val myOrigin: String
    private val useEnvelope: Boolean
    private var interceptor = false
    private var tracing = false

    init {
        val system = Platform.getInstance().eventSystem
        consumer = system.localConsumer(route, WorkerHandler())
        myOrigin = Platform.getInstance().origin
        useEnvelope = def.inputIsEnvelope()
        interceptor = def.isInterceptor
        tracing = def.isTrackable
        // tell manager that this worker is ready to process a new event
        system.send(def.route, READY + route)
        started()
    }

    private inner class WorkerHandler : Handler<Message<ByteArray?>> {
        @OptIn(DelicateCoroutinesApi::class)
        override fun handle(message: Message<ByteArray?>) {
            if (!stopped) {
                val event = EventEnvelope()
                try {
                    event.load(message.body())
                    event.headers.remove(MY_ROUTE)
                    event.headers.remove(MY_TRACE_ID)
                    event.headers.remove(MY_TRACE_PATH)
                } catch (e: IOException) {
                    log.error("Unable to decode event - {}", e.message)
                    return
                }
                if (def.isKotlin) {
                    // execute function as a coroutine
                    GlobalScope.launch(Platform.getInstance().vertx.dispatcher()) {
                        executeFunction(event)
                    }
                } else if (def.isVirtualThread) {
                    // execute function as a virtual thread
                    vThreadExecutor.submit {
                        val worker = WorkerHandler(def, route, instance, tracing, interceptor, useEnvelope)
                        worker.executeFunction(event)
                    }
                } else {
                    // execute function as a runnable using kernel thread
                    kernelExecutor.submit {
                        val worker = WorkerHandler(def, route, instance, tracing, interceptor, useEnvelope)
                        worker.executeFunction(event)
                    }
                }
            }
        }

        private suspend fun executeFunction(event: EventEnvelope) {
            if (TemporaryInbox.TEMPORARY_INBOX != def.route) {
                event.clearAnnotations()
            }
            val rpc = event.getTag(EventEmitter.RPC)
            val po = EventEmitter.getInstance()
            val ref = if (tracing) po.startTracing(parentRoute, event.traceId, event.tracePath, instance) else "?"
            val ps = processEvent(event, rpc)
            val trace = po.stopTracing(ref)
            if (tracing && trace != null && trace.id != null && trace.path != null) {
                try {
                    val journaled = po.isJournaled(def.route)
                    if (journaled || rpc == null || !ps.isDelivered) {
                        // Send tracing information to distributed trace logger
                        val dt = EventEnvelope().setTo(DistributedTrace.DISTRIBUTED_TRACING)
                        val payload: MutableMap<String, Any> = HashMap()
                        payload[ANNOTATIONS] = trace.annotations
                        // send input/output dataset to journal if configured in journal.yaml
                        if (journaled) {
                            payload[JOURNAL] = ps.inputOutput
                        }
                        val metrics: MutableMap<String, Any> = HashMap()
                        metrics[ORIGIN] = myOrigin
                        metrics[ID] = trace.id
                        metrics[PATH] = trace.path
                        metrics[SERVICE] = def.route
                        metrics[START] = trace.startTime
                        metrics[SUCCESS] = ps.isSuccess
                        metrics[FROM] = if (event.from == null) UNKNOWN else event.from
                        metrics[EXEC_TIME] = ps.executionTime
                        metrics[STATUS] = ps.status
                        if (!ps.isSuccess) {
                            metrics[EXCEPTION] = ps.exception
                        }
                        if (!ps.isDelivered) {
                            metrics[STATUS] = 500
                            metrics[SUCCESS] = false
                            metrics[EXCEPTION] = "Response not delivered - " + ps.deliveryError
                        }
                        payload[TRACE] = metrics
                        po.send(dt.setBody(payload))
                    }
                } catch (e: Exception) {
                    log.error("Unable to send to {}", DistributedTrace.DISTRIBUTED_TRACING, e)
                }
            } else {
                // print delivery warning if tracing is not enabled
                if (!ps.isDelivered) {
                    log.warn(
                        "Event not delivered - {}, from={}, to={}, type={}, exec_time={}",
                        ps.deliveryError,
                        if (event.from == null) "unknown" else event.from, event.to,
                        if (ps.isSuccess) "response" else "exception(" + ps.status + ", " + ps.exception + ")",
                        ps.executionTime
                    )
                }
            }

            /*
         * If this response is not a Mono reactive object, send a ready signal to inform the system this worker
         * is ready for next event. Otherwise, defer it until the Mono result is realized.
         *
         * This guarantee that this future task is executed orderly.
         */
            if (!ps.isReactive) {
                Platform.getInstance().eventSystem.send(def.route, READY + route)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private suspend fun processEvent(event: EventEnvelope, rpc: String?): ProcessStatus {
            val f: Any = def.suspendFunction
            val ps = ProcessStatus()
            val po = EventEmitter.getInstance()
            val inputOutput: MutableMap<String, Any> = HashMap()
            val input: MutableMap<String, Any> = HashMap()
            input[HEADERS] = event.headers
            if (event.rawBody != null) {
                input[BODY] = event.rawBody
            }
            inputOutput[INPUT] = input
            val customSerializer = def.customSerializer
            val begin = System.nanoTime()
            return try {
                /*
                 * If the service is an interceptor or the input argument is EventEnvelope,
                 * we will pass the original event envelope instead of the message body.
                 */
                val inputBody: Any?
                val cls = if (def.inputClass != null) def.inputClass else def.poJoClass
                if (useEnvelope || (interceptor && cls == null)) {
                    inputBody = event
                } else if (event.rawBody is List<*> && cls != null) {
                    // check if the input is a list of map
                    val inputList = event.rawBody as List<Any?>
                    // validate that the objects are PoJo or null
                    var n = 0
                    for (o in inputList) {
                        if (o == null || o is Map<*, *>) {
                            n++
                        }
                    }
                    if (n == inputList.size) {
                        val updatedList: MutableList<Any?> = ArrayList()
                        val mapper = SimpleMapper.getInstance().mapper
                        for (o in inputList) {
                            // convert Map to PoJo
                            val pojo: Any?
                            if (o == null) {
                                pojo = null
                            } else {
                                val serializer = def.customSerializer
                                pojo = if (customSerializer != null) {
                                    serializer.toPoJo(o, cls)
                                } else {
                                    mapper.readValue(o, cls)
                                }
                            }
                            updatedList.add(pojo)
                        }
                        inputBody = updatedList
                    } else {
                        inputBody = event.body
                    }
                } else {
                    inputBody = if (event.rawBody is Map<*, *> && cls != null) {
                        if (cls == AsyncHttpRequest::class.java) {
                            // handle special case
                            AsyncHttpRequest(event.rawBody)
                        } else {
                            // automatically convert Map to PoJo
                            if (customSerializer != null) {
                                customSerializer.toPoJo(event.rawBody, cls)
                            } else {
                                event.getBody(cls)
                            }
                        }
                    } else {
                        event.body
                    }
                }

                // Insert READ only metadata into function input headers
                val parameters: MutableMap<String, String> = HashMap(event.headers)
                parameters[MY_ROUTE] = parentRoute
                if (event.traceId != null) {
                    parameters[MY_TRACE_ID] = event.traceId
                }
                if (event.tracePath != null) {
                    parameters[MY_TRACE_PATH] = event.tracePath
                }
                val result: Any? = def.suspendFunction.handleEvent(parameters, inputBody, instance)
                val delta: Float = (System.nanoTime() - begin).toFloat() / EventEmitter.ONE_MILLISECOND
                // adjust precision to 3 decimal points
                val diff = String.format("%.3f", 0.0f.coerceAtLeast(delta)).toFloat()
                val output: MutableMap<String, Any> = HashMap()
                val replyTo = event.replyTo
                if (replyTo != null) {
                    val rpcTimeout = util.str2long(rpc)
                    // if it is a callback instead of an RPC call, use default timeout of 30 minutes
                    val expiry = if (rpcTimeout < 0) DEFAULT_TIMEOUT else rpcTimeout
                    val response = EventEnvelope()
                    response.to = replyTo
                    response.from = def.route
                    response.tags = event.tags
                    /*
                     * Preserve correlation ID and extra information
                     *
                     * "Extra" is usually used by event interceptors.
                     * For example, to save some metadata from the original sender.
                     */
                    if (event.correlationId != null) {
                        response.setCorrelationId(event.correlationId)
                    }
                    // propagate the trace to the next service if any
                    if (event.traceId != null) {
                        response.setTrace(event.traceId, event.tracePath)
                    }
                    var skipResponse = false
                    // if response is a Mono, subscribe to it for a future response
                    if (result is Mono<*>) {
                        skipResponse = true
                        // set reactive to defer service acknowledgement until Mono is complete
                        ps.setReactive()
                        val platform = Platform.getInstance()
                        val timer = AtomicLong(-1)
                        val completed = AtomicBoolean(false)
                        // For non-blocking operation, use a new virtual thread for the subscription
                        val disposable = result.doFinally(Consumer { _: Any? ->
                            val t1 = timer.get()
                            if (t1 > 0) {
                                platform.vertx.cancelTimer(t1)
                            }
                            // finally, send service acknowledgement
                            platform.eventSystem.send(def.route, READY + route)
                        }).subscribeOn(Schedulers.fromExecutor(platform.virtualThreadExecutor))
                            .subscribe({ data: Any? ->
                                completed.set(true)
                                sendMonoResponse(response, data!!, begin)
                            }, { e: Any? ->
                                if (e is Throwable) {
                                    completed.set(true)
                                    val errorResponse = prepareErrorResponse(event, e)
                                    try {
                                        po.send(encodeTraceAnnotations(errorResponse).setExecutionTime(getExecTime(begin)))
                                    } catch (e2: IOException) {
                                        log.error(
                                            "Unable to deliver exception from {} - {}",
                                            route,
                                            e2.message
                                        )
                                    }
                                }
                            }, {
                                // When the Mono emitter sends a null payload, Mono will not return any result.
                                // Therefore, the system must return a null body for this normal use case.
                                if (!completed.get()) {
                                    sendMonoResponse(response, null, begin)
                                }
                            })
                        // dispose a pending Mono if timeout
                        timer.set(platform.vertx.setTimer(expiry) { _: Long? ->
                            timer.set(-1)
                            if (!disposable.isDisposed) {
                                log.warn(
                                    "Async response timeout after {} for {}",
                                    util.elapsedTime(expiry),
                                    route
                                )
                                disposable.dispose()
                            }
                        })
                    }
                    var resultSet = result
                    /*
                 * if response is a Flux, subscribe to it for a future response and immediately
                 * return x-stream-id and x-ttl so the caller can use a FluxConsumer to read the stream.
                 *
                 * The response contract is two headers containing x-stream-id and x-ttl.
                 * The response body is an empty map.
                 */
                    if (result is Flux<*>) {
                        resultSet = Collections.EMPTY_MAP
                        val fluxRelay = FluxPublisher<Any?>(result as Flux<Any?>, expiry)
                        response.setHeader(X_TTL, expiry)
                        response.setHeader(X_STREAM_ID, fluxRelay.publish())
                    }
                    val simulatedStreamTimeout = !skipResponse && updateResponse(response, resultSet!!)
                    if (response.headers.isNotEmpty()) {
                        output[HEADERS] = response.headers
                    }
                    output[BODY] =
                        if (response.rawBody == null) "null" else response.rawBody
                    output[STATUS] = response.status
                    inputOutput[OUTPUT] = output
                    try {
                        if (!interceptor && !skipResponse && !simulatedStreamTimeout) {
                            po.send(encodeTraceAnnotations(response).setExecutionTime(diff))
                        }
                    } catch (e2: Exception) {
                        ps.setUnDelivery(e2.message)
                    }
                } else {
                    val response = EventEnvelope().setBody(result)
                    output[BODY] = if (response.rawBody == null) "null" else response.rawBody
                    output[STATUS] = response.status
                    output[ASYNC] = true
                    inputOutput[OUTPUT] = output
                }
                ps.inputOutput = inputOutput
                ps.executionTime = diff
                return ps
            } catch (e: Exception) {
                val delta = (System.nanoTime() - begin).toFloat() / EventEmitter.ONE_MILLISECOND
                val diff = String.format("%.3f", 0.0f.coerceAtLeast(delta)).toFloat()
                ps.executionTime = diff
                val replyTo = event.replyTo
                val status: Int = when (e) {
                    is AppException -> {
                        e.status
                    }
                    is TimeoutException -> {
                        408
                    }
                    is IllegalArgumentException -> {
                        400
                    } else -> {
                        500
                    }
                }
                val ex = Utility.getInstance().getRootCause(e)
                val error = simplifyCastError(ex)
                if (f is MappingExceptionHandler) {
                    try {
                        f.onError(parentRoute, AppException(status, error), event, instance)
                    } catch (e3: Exception) {
                        ps.setUnDelivery(e3.message)
                    }
                    val output: MutableMap<String, Any?> = HashMap()
                    output[STATUS] = status
                    output[EXCEPTION] = error
                    inputOutput[OUTPUT] = output
                    return ps.setException(status, error).setInputOutput(inputOutput)
                }
                val output: MutableMap<String, Any?> = HashMap()
                if (replyTo != null) {
                    val response = EventEnvelope()
                    response.setTo(replyTo).setStatus(status).body = error
                    response.exception = e
                    response.executionTime = diff
                    response.from = def.route
                    if (event.correlationId != null) {
                        response.correlationId = event.correlationId
                    }
                    response.tags = event.tags
                    // propagate the trace to the next service if any
                    if (event.traceId != null) {
                        response.setTrace(event.traceId, event.tracePath)
                    }
                    encodeTraceAnnotations(response)
                    try {
                        po.send(response)
                    } catch (e4: Exception) {
                        ps.setUnDelivery(e4.message)
                    }
                } else {
                    output[ASYNC] = true
                    if (status >= 500) {
                        log.error("Unhandled exception for $route", ex)
                    } else {
                        log.warn("Unhandled exception for {} - {}", route, error)
                    }
                }
                output[STATUS] = status
                output[EXCEPTION] = error
                inputOutput[OUTPUT] = output
                ps.setException(status, error).setInputOutput(inputOutput)
            }
        }

        private fun sendMonoResponse(response: EventEnvelope, data: Any?, begin: Long) {
            val po = EventEmitter.getInstance()
            updateResponse(response, data)
            try {
                po.send(encodeTraceAnnotations(response).setExecutionTime(getExecTime(begin)))
            } catch (e1: IOException) {
                log.error(
                    "Unable to deliver async response from {} - {}",
                    route,
                    e1.message
                )
            }
        }

        private fun prepareErrorResponse(event: EventEnvelope, e: Throwable): EventEnvelope {
            val response = EventEnvelope()
            response.setTo(event.replyTo).setFrom(def.route).setException(e)
            if (event.correlationId != null) {
                response.setCorrelationId(event.correlationId)
            }
            response.tags = event.tags
            // propagate the trace to the next service if any
            if (event.traceId != null) {
                response.setTrace(event.traceId, event.tracePath)
            }
            return response
        }

        private fun updateResponse(response: EventEnvelope, result: Any?): Boolean {
            val customSerializer = def.customSerializer
            if (result is EventEnvelope) {
                val headers = result.headers
                if (headers.isEmpty() && result.status == 408 && result.rawBody == null) {
                    // simulate a READ timeout for ObjectStreamService
                    return true
                } else {
                    /*
                     * When EventEnvelope is used as a return type, the system will transport
                     * 1. payload
                     * 2. key-values (as headers)
                     */
                    response.setBody(result.rawBody)
                    if (customSerializer == null) {
                        response.setType(result.type)
                    }
                    for ((k, value) in headers) {
                        if (MY_ROUTE != k && MY_TRACE_ID != k && MY_TRACE_PATH != k) {
                            response.setHeader(k, value)
                        }
                    }
                    response.setStatus(result.status)
                }
            } else {
                // when using custom serializer, the result will be converted to a Map
                if (customSerializer != null && util.isPoJo(result)) {
                    response.setBody(customSerializer.toMap(result))
                } else {
                    response.setBody(result)
                }
            }
            return false
        }

        private fun getExecTime(begin: Long): Float {
            val delta = (System.nanoTime() - begin).toFloat() / EventEmitter.ONE_MILLISECOND
            // adjust precision to 3 decimal points
            return String.format("%.3f", max(0.0, delta.toDouble())).toFloat()
        }

        private fun simplifyCastError(ex: Throwable): String {
            val error = ex.message
            if (error == null) {
                return "null"
            } else if (ex is ClassCastException) {
                val sep = error.lastIndexOf(" (")
                return if (sep > 0) error.substring(0, sep) else error
            } else {
                return error
            }
        }

        private fun encodeTraceAnnotations(response: EventEnvelope): EventEnvelope {
            val po = EventEmitter.getInstance()
            val trace = po.getTrace(parentRoute, instance)
            if (trace != null) {
                response.setAnnotations(trace.annotations)
            }
            return response
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WorkerQueue::class.java)
        private val util: Utility = Utility.getInstance()
        private const val ID = "id"
        private const val PATH = "path"
        private const val START = "start"
        private const val UNKNOWN = "unknown"
        private const val TRACE = "trace"
        private const val SUCCESS = "success"
        private const val FROM = "from"
        private const val EXEC_TIME = "exec_time"
        private const val ORIGIN = "origin"
        private const val SERVICE = "service"
        private const val INPUT = "input"
        private const val OUTPUT = "output"
        private const val HEADERS = "headers"
        private const val BODY = "body"
        private const val STATUS = "status"
        private const val EXCEPTION = "exception"
        private const val ASYNC = "async"
        private const val ANNOTATIONS = "annotations"
        private const val JOURNAL = "journal"
        private const val MY_ROUTE = "my_route"
        private const val MY_TRACE_ID = "my_trace_id"
        private const val MY_TRACE_PATH = "my_trace_path"
        private const val X_STREAM_ID: String = "x-stream-id"
        private const val X_TTL: String = "x-ttl"
        private const val DEFAULT_TIMEOUT: Long = 30 * 60 * 1000L // 30 minutes
    }
}