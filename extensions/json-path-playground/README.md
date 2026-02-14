# JSON-Path Playground

This is a simple playground application for user to test if their JSON-Path commands are correct.

## Build and run

To build this application from source, use `mvn clean package`.

To run this application, do `java -jar target/json-path-playground-{version}.jar`

## User guide

1. Visit http://127.0.0.1:8085 to get started
2. Click "Start" to connect to playground
3. Paste JSON (or XML) text into text area
4. Type "help" in command input box for more instruction
5. Enter "load" to load the JSON/XML text and render it into a JSON object
6. The JSON object will be populated in a `response` node. 
7. Type `response` to try out simple retrieval command 
8Enter `$.response` to try out the first JSON-Path command

## React UI for the playground

For your convenience, we have pre-built the playground UI app in the resources/public folder.

If you want to build the UI from source, please refer to [React UI](USER_INTERFACE.md) for details

## Simple retrieval command

You can use dot-bracket format to retrieve subset of data from the JSON object.

Given `{ "hello": "world" }`,
the response object is `{ response: { "hello": "world" } }`,

Submitting `response.hello` will return "world".

## JSON-Path command

There are multiple sources of documentation for JSON-Path syntax so it is not listed here.
Generally, JSON-Path command starts with "$".

For example, https://support.smartbear.com/alertsite/docs/monitors/api/endpoint/jsonpath.html

Given `{ "hello": "world" }`,
the response object is `{ response: { "hello": "world" } }`,

Entering `$.response.hello` will return "world".
