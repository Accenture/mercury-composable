# starter-function — Layer 1 template

A minimal composable application: one function (`v1.greeting`) exposed over HTTP by
REST automation. Copy this directory out of the Mercury repository to begin a new
project — the build files are standalone.

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
java -jar target/starter-function-4.12.6.jar

# Gradle
gradle build
java -jar build/libs/starter-function-4.12.6.jar
```

Then:

```bash
curl "http://127.0.0.1:8301/api/greeting?name=Mercury"
# → {"greeting": "Hello, Mercury", ...}
```

## What to look at

| File | Role |
|:---|:---|
| `src/main/java/com/accenture/starter/Greeting.java` | The composable function — addressed only by its route name |
| `src/main/resources/rest.yaml` | Maps `/api/greeting` to the function; add your endpoints here |
| `src/main/resources/application.properties` | App name, port, `rest.automation` |
| `src/test/java/com/accenture/starter/GreetingTest.java` | Unit test (function in isolation) + end-to-end HTTP test |

## Next steps

- Add functions (`@PreLoad` + `TypedLambdaFunction`) and map them in `rest.yaml`.
- Ready to orchestrate several functions? Move up a layer — see the
  [starter-flow](../starter-flow) template and the
  [AI developer guide](https://accenture.github.io/mercury-composable/guides/ai-developer-guide/).
