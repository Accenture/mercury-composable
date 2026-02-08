# JSON-Path Playground

This is a simple playground application for user to test if their JSON-Path commands are correct.

## Build and run

To build this application from source, use `mvn clean package`.

To run this application, do `java -jar target/json-path-playground-{version}.jar`

## User guide

1. Visit http://127.0.0.1:8085 to get started
2. Select "Playground" from home page
3. Click "Start" to connect to playground
4. Paste JSON (or XML) text into text area
5. Type "help" in command input box for more instruction
6. Enter "load" to load the JSON/XML text and render it into a JSON object
7. The JSON object will be populated in a `response` node. 
8. Type `response` to try out simple retrieval command 
9. Enter `$.response` to try out the first JSON-Path command


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
