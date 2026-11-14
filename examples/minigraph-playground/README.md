# MiniGraph Playground

This is a sample MiniGraph Playground application for user to create and test Knowledge Graphs

## Build and run

To build this application from source, use `mvn clean package`.

To run this application, do `java -jar target/minigraph-playground-{version}.jar`

## Getting started

1. Visit http://127.0.0.1:8085 to get started
2. Select "MiniGraph Playground" from the top right selection box
3. Click the help button to learn more
4. Follow the tutorials as a step-by-step study guide

## Model answers

While the model answers for tutorials 1 to 14 are embedded, we recommend you follow the tutorials one by one.

When you want to compare your graph model with the model answer, you can use the 'import tutorial-n' where
n is a tutorial chapter number.

## AI demo graphs

Two deployed graphs reach an LLM through the LLM helper app of a language pack, with the engine holding no credential:
`support-triage` (one AI node, the graph routes on its verdict) and `order-assistant` (an agent loop whose one write
waits for a supervisor through suspend and resume). See `src/main/resources/graph/README.md` for the setup and the
protocol, and `OrderAssistantTest` for the token-free end-to-end test.

## Application template

You can use this subproject as a template to build your own MiniGraph application.
