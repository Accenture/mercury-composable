package com.accenture.demo.services

import org.platformlambda.core.annotations.PreLoad
import org.platformlambda.core.models.LambdaFunction

@PreLoad(route = "demo.health", instances = 5)
class DemoHealth : LambdaFunction {

    override fun handleEvent(headers: Map<String, String>, input: Any, instance: Int): Any {
        return when (headers[TYPE]) {
            INFO -> {
                mapOf(
                    "service" to "demo.service",
                    "href" to "http://127.0.0.1"
                )
            }
            HEALTH -> {
                mapOf("demo" to "I am running fine")
            }
            else -> throw IllegalArgumentException("type must be info or health")
        }
    }

    companion object {
        private const val TYPE = "type"
        private const val INFO = "info"
        private const val HEALTH = "health"
    }
}