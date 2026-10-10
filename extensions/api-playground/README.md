# API playground

A standalone Mercury application that renders OpenAPI 3.0 documents with Swagger UI, wrapped in a
Mercury-branded page. It is a convenient tool for API design and tests, and the natural companion
of a MiniGraph application: a graph that declares its contract answers an OpenAPI document on
demand, and this page loads it in one dialog.

The application has no Spring Boot dependency: the Reactive HTTP server of `platform-core` serves
the page as static content and the two REST endpoints below through REST automation, on one port.

## Before you start

Swagger UI is third-party code that goes stale quickly, so it is not committed here: the
`src/main/resources/public/swagger-ui` folder is in `.gitignore`, and a script fetches the latest
release into it. Run one of the two (they do the same thing; Python 3.8+ or Node.js 18+, no
dependencies):

```shell
python3 scripts/fetch-swagger-ui.py
# or
node scripts/fetch-swagger-ui.js
```

Then build the executable JAR:

```shell
mvn clean package
```

The script resolves the latest release of https://github.com/swagger-api/swagger-ui, downloads the
tag's source tarball and extracts the assets of its `dist` folder into `src/main/resources/public/swagger-ui`
- all but upstream's own page (`index.html`, `index.css`, `swagger-initializer.js`), which this
application replaces with its own - and records what it fetched in `swagger-ui-version.txt`.
`--version v5.33.1` pins a release and `--target DIR` names another folder. Run it again to
upgrade: the script empties the target folder before it writes, so a fetch never leaves files of
two releases side by side (a non-empty folder that holds no previous fetch is refused rather than
emptied, in case `--target` was mistyped). The build fails at the start with the same instruction
when the folder has not been fetched. A release tarball carries the `dist` files and not the
`.map` source maps (the upstream project marks them `export-ignore`); the page does not need them.

## Running this application

```shell
java -jar target/api-playground-x.y.z.jar
```

Then visit http://127.0.0.1:8200 - the page opens on the bundled example `demo.yaml`.

## Loading a document

The header offers four sources; the status line under it says what is loaded.

1. **URL** - enter the URL of an OpenAPI document and press Load. `http://127.0.0.1:8200/?url=...`
   opens the page on that document.
2. **MiniGraph...** - a dialog for a MiniGraph application running in dev mode: host (default
   `127.0.0.1`), port (default `8085`) and either a graph ID (a deployed graph,
   `GET /api/openapi/{graph-id}`) or a session ID (a Playground session's draft,
   `GET /api/openapi/session/{session-id}`). The dialog shows the URL it will load and remembers
   the last values in this browser. The engine's dev routes carry the wildcard CORS entry, so the
   browser may read the document, and its `servers` entry points at the engine, so **Try it out**
   posts to the graph.
3. **Bundled...** - the documents this application serves itself: the example `demo.yaml` and the
   files of the optional folder below.
4. **Open file**, or **drag and drop** a `.yaml`, `.yml` or `.json` file anywhere on the page. A
   file is a point-in-time copy; a URL is always current.

## Your own documents without a rebuild

Copy OpenAPI files (yaml, yml or json) into the folder named by `api.playground.apps` in
`application.properties` (default `/tmp/api-playground`). They appear in the **Bundled...** list and
at `GET /api/specs/{filename}`. A file name must be a plain name - letters, digits, dot, dash and
underscore.

## Sample REST endpoints

The example `demo.yaml` documents the application's own two endpoints, declared in `resources/rest.yaml`
and served by the functions in `com.accenture.examples.services`:

```text
GET http://127.0.0.1:8200/api/specs

# the documents this application offers: the example and the folder's files

GET http://127.0.0.1:8200/api/specs/demo.yaml

# one document, as application/yaml or application/json
```

Because the page and the endpoints share one origin, **Try it out** on the example works as is.

## Acknowledgements

This application uses the following open source software:
1. Mercury Composable under the Apache 2.0 license - https://github.com/Accenture/mercury-composable/blob/main/LICENSE
2. Swagger UI under the Apache 2.0 license, fetched at build time and not redistributed here - https://swagger.io/license/
3. The "atom" icon of the page header, from Material Design Icons under the Apache 2.0 license - https://pictogrammers.com/library/mdi/
