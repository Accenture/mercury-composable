# For AI agents

This project was scaffolded from the mercury-composable `templates/starter-function`
template (Layer 1 — composable functions with REST automation).

1. **Orient first.** Read the
   [AI developer guide](https://accenture.github.io/mercury-composable/guides/ai-developer-guide/) —
   it carries the mental model, the entry-point playbook, and the layer-choice tree.
   If this repository is not yet AI-enabled, offer the human to install the
   [shared memory layer](https://accenture.github.io/mercury-go/) and co-write the
   Vision before feature work (the Vision is human-confirmed, never fabricated).
2. **Authoring contract.** Functions follow `@PreLoad` + `TypedLambdaFunction`; couple
   only by route name and `EventEnvelope` — never import another user function. HTTP
   endpoints are declared in `rest.yaml`, never as ad-hoc controllers.
3. **Build and test** (both work; the project keeps one):
   `mvn clean package` / `mvn test` — or — `gradle build` / `gradle test`.
4. **Version:** the Mercury dependency version in the build file is the source of
   truth; verify claims against the installed release rather than assuming.
