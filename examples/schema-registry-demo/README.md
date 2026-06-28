# Schema Registry Demo

This project demonstrates how to interact with the minimalist Schema Registry standalone mock provided in `helpers/schema-registry-standalone`.

## Pre-requisites

You must first start the schema registry mock server.
In a separate terminal, run:
```bash
cd ../../
mvn clean install -f helpers/schema-registry-standalone/pom.xml
java -jar helpers/schema-registry-standalone/target/schema-registry-standalone-4.5.0.jar
```
The server will start on port `8081` by default.

## Option 1: Interact via cURL (Fastest)

A convenient shell script is provided to demonstrate registering and retrieving both AVRO and JSON schemas using standard `curl` commands.

```bash
./run-demo.sh
```

### Manual cURL Commands

**1. Register an AVRO Schema:**
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}"}' \
  http://127.0.0.1:8081/subjects/user-value/versions
```

**2. Register a JSON Schema:**
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"schema": "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}", "schemaType": "JSON"}' \
  http://127.0.0.1:8081/subjects/person-value/versions
```

**3. Retrieve a Schema by ID:**
```bash
curl http://127.0.0.1:8081/schemas/ids/1
```

## Option 2: Run the Java Demo Application

You can also run this Maven module to test programmatic interaction. The `DemoRunner` class demonstrates how an application could use `AsyncHttpRequest` to dynamically hit the registry.

```bash
mvn clean install
java -jar target/schema-registry-demo-4.5.0.jar
```

Once started, the application exposes a simple composable function route at `demo.runner`. You can trigger it via the event API or by adapting the `MainApp` to fire the event on startup.
