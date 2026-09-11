# starter-flow — Layer 2 template

A minimal Event Script application: an HTTP endpoint launches a flow that sequences two
decoupled functions (`v1.validate.request` → `v1.make.greeting`) with declarative data
mapping. Copy this directory out of the Mercury repository to begin a new project — the
build files are standalone.

## Choose your build tool

The template ships both. Keep one, delete the other:

- **Maven** — keep `pom.xml`
- **Gradle** — keep `build.gradle` + `settings.gradle` (run `gradle wrapper` once in
  your copy to pin a Gradle version)

## Prerequisite

Mercury artifacts are not published to Maven Central. Build the
[mercury-composable](https://github.com/Accenture/mercury-composable) repository once with
`mvn clean install` (this installs them into your local Maven repository), or point the
build at your organization's artifact repository.

## Build, test, run

```bash
# Maven
mvn clean package
java -jar target/starter-flow-4.12.7.jar

# Gradle
gradle build
java -jar build/libs/starter-flow-4.12.7.jar
```

Then:

```bash
curl -s -X POST http://127.0.0.1:8302/api/greeting \
     -H "content-type: application/json" \
     -d '{"name": "Mercury"}'
# → {"greeting": "Hello, Mercury", ...}
```

## What to look at

| File | Role |
|:---|:---|
| `src/main/resources/flows/greeting-flow.yml` | The orchestration — task order and data mapping, no code |
| `src/main/resources/flows.yaml` | Registry of active flows |
| `src/main/resources/rest.yaml` | Binds `/api/greeting` to the flow (`service` + `flow`, both required) |
| `src/main/java/com/accenture/starter/tasks/` | The two functions — each knows nothing about the other |
| `src/test/java/com/accenture/starter/GreetingFlowTest.java` | End-to-end flow test over HTTP |

## Next steps

- Grow the flow: add tasks, a `decision` branch, or an exception handler — see the
  [Event Script syntax](https://accenture.github.io/mercury-composable/guides/event-script/syntax/).
- Modeling a whole service as a graph? Move up a layer — see the
  [starter-graph](../starter-graph) template and the
  [AI developer guide](https://accenture.github.io/mercury-composable/guides/ai-developer-guide/).
