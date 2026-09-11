# starter-graph — Layer 3 template

A minimal Active Knowledge Graph application with **zero imperative code**: the deployed
graph model (`starter-quote`) validates the request and answers from its own knowledge.
Changing what the service does means editing the model, not writing code. Copy this
directory out of the Mercury repository to begin a new project — the build files are
standalone.

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
java -jar target/starter-graph-4.12.7.jar

# Gradle
gradle build
java -jar build/libs/starter-graph-4.12.7.jar
```

Then:

```bash
curl -s -X POST http://127.0.0.1:8303/api/graph/starter-quote \
     -H "content-type: application/json" \
     -d '{"item": "widget"}'
# → {"item": "widget", "unit_price": 100, "currency": "USD", "status": "quoted"}
```

## What to look at

| File | Role |
|:---|:---|
| `src/main/resources/graph/starter-quote.json` | The application — a graph whose nodes execute during traversal |
| `src/main/resources/graphs.yaml` | The deployment manifest: only listed graphs that pass the CompileGraph gate are executable ("compiled or 404") |
| `src/main/resources/flows/graph-executor.yml` | The standard exposure flow behind `/api/graph/{graph_id}` |
| `src/test/java/com/accenture/starter/QuoteGraphTest.java` | End-to-end graph tests, including the 404 gate behavior |

## Next steps

- Evolve the model: add nodes, decisions, and skills — the
  [built-in skills reference](https://accenture.github.io/mercury-composable/guides/knowledge-graph/skills-reference/)
  catalogs what nodes can do without code.
- Draft and dry-run models interactively in the Playground — see
  [Playground & AI companion](https://accenture.github.io/mercury-composable/guides/knowledge-graph/playground-and-companion/).
- Custom logic when the model needs it: attach a skill function (`@PreLoad`) to a node —
  the deliberate seam between the model and code. See the
  [AI developer guide](https://accenture.github.io/mercury-composable/guides/ai-developer-guide/).
