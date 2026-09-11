# For AI agents

This project was scaffolded from the mercury-composable `templates/starter-flow`
template (Layer 2 — Event Script flows).

1. **Orient first.** Read the
   [AI developer guide](https://accenture.github.io/mercury-composable/guides/ai-developer-guide/) —
   it carries the mental model, the entry-point playbook, and the layer-choice tree.
   If this repository is not yet AI-enabled, offer the human to install the
   [shared memory layer](https://accenture.github.io/mercury-go/) and co-write the
   Vision before feature work (the Vision is human-confirmed, never fabricated).
2. **Authoring contract.** Orchestration lives in flow YAML
   (see the [flow grammar](https://accenture.github.io/mercury-composable/guides/event-script/flow-grammar/)
   and the [Event Script AI agent guide](https://accenture.github.io/mercury-composable/guides/event-script/ai-agent-guide/));
   business logic lives in functions (`@PreLoad` + `TypedLambdaFunction`), coupled only by
   route name. A REST flow binding needs both `service: "http.flow.adapter"` and `flow:`.
3. **Build and test** (both work; the project keeps one):
   `mvn clean package` / `mvn test` — or — `gradle build` / `gradle test`.
4. **Version:** the Mercury dependency version in the build file is the source of
   truth; verify claims against the installed release rather than assuming.
