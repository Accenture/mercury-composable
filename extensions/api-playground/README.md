# API playground

This is a standalone application that uses swagger UI for the rendering of OpenAPI 3.0 YAML files.

This application is designed as a convenient tool for API design and tests.

## Before you start

Swagger UI is third-party code that goes stale quickly, so it is not committed here: the
`src/main/resources/public` folder is in `.gitignore`, and a script fetches the latest release into
it. Run one of the two (they do the same thing; Python 3.8+ or Node.js 18+, no dependencies):

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
tag's source tarball, extracts the files of its `dist` folder into `src/main/resources/public`,
points `swagger-initializer.js` at the sample `demo.yaml` below (upstream points at the Petstore
demo), and records what it fetched in `swagger-ui-version.txt`. `--version v5.33.1` pins a release
and `--target DIR` names another folder. Run it again to upgrade: the script empties the target
folder before it writes, so a fetch never leaves files of two releases side by side (a non-empty
folder that holds no previous fetch is refused rather than emptied, in case `--target` was
mistyped). The build fails at the start with the same instruction when the folder has not been
fetched. A release tarball carries the 17 `dist`
files and not the six `.map` source maps (the upstream project marks them `export-ignore`); the
application does not need them.

## OpenAPI specs file folder

The default directory is `resources/sample/yaml`

In this folder, you will find the demo.yaml swagger config file.
This file is a sample for illustration purpose only.

## Sample REST endpoints

To support the demo.yaml config file, two REST endpoints are created in the resources/rest.yaml file.
They are served by REST automation on port 8222 (Swagger UI itself is served by Spring on port 8200):

```text
GET http://127.0.0.1:8222/api/specs

# this endpoint returns a list of swagger files
# under the resources/sample/yaml folder.

GET http://127.0.0.1:8222/api/specs/demo.yaml

# this endpoint returns content of the demo.yaml file.
```

The two endpoints are served by their corresponding functions in the package under com.accenture.examples.services.

## Running this application

To run this application:
```
java -jar target/api-playground-x.y.z.jar
```

You will see it starting a Reactive HTTP server at port 8222.
Then it will run as a Spring Boot app using port-8200.

The port 8222 illustrates that you can use swagger-ui to connect to another host.

Please visit http://127.0.0.1:8200 to see the swagger-ui home page.

The page opens on the sample demo.yaml. To load another file, enter its URL in the explore bar,
for example "http://127.0.0.1:8200/yaml/demo.yaml", or a MiniGraph engine's
"http://127.0.0.1:8085/api/openapi/{graph-id}" (see the graph contract guide).

## Loading your own swagger files

You can copy them into the resources/sample/yaml folder and rebuild this app.

Alternatively, you can externalize the swagger folder in the local file system.

You would need to update application.properties and replace "classpath:/sample" with
"file:/your/local/folder":

```properties
spring.web.resources.static-locations=classpath:/public/,classpath:/sample/
```

Note that you must keep the "classpath:/public/" in the static-locations parameter above.

This allows the app to serve the swagger-ui from classpath:/public/ and
your own swagger files from file:/your/local/folder.

## Acknowledgements

This application uses the following open source software:
1. Mercury Composable under the Apache 2.0 license - https://github.com/Accenture/mercury-composable/blob/main/LICENSE
2. Swagger UI under the Apache 2.0 license, fetched at build time and not redistributed here - https://swagger.io/license/
