package com.accenture.demo.tasks

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.models.TypedLambdaFunction
import org.platformlambda.core.util.Utility
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@PreLoad(route = "v1.hello.exception", instances = 10)
class HelloExceptionHandler : TypedLambdaFunction<Map<String?, Any?>, Map<String?, Any?>> {
    override fun handleEvent(
        headers: Map<String?, String?>,
        input: Map<String?, Any?>,
        instance: Int
    ): MutableMap<String?, Any?> {
        if (input.containsKey(STACK)) {
            val map = Utility.getInstance().stackTraceToMap(input[STACK].toString())
            log.info("{}", map)
        }
        return if (input.containsKey(STATUS) && input.containsKey(MESSAGE)) {
            log.info("User defined exception handler - status={} error={}", input[STATUS], input[MESSAGE])
            mutableMapOf<String?, Any?>(TYPE to ERROR, STATUS to input[STATUS], MESSAGE to input[MESSAGE])
        } else {
            mutableMapOf<String?, Any?>()
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HelloExceptionHandler::class.java)

        private const val TYPE = "type"
        private const val ERROR = "error"
        private const val STATUS = "status"
        private const val MESSAGE = "message"
        private const val STACK = "stack"
    }
}
