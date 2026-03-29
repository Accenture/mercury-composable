// Data model skeleton borrowed from https://github.com/spring-guides/gs-rest-service

package com.accenture.rest;

import java.util.Map;

@SuppressWarnings("rawtypes")
public record Greeting(long id, Map content) { }
