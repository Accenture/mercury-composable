# This folder contains optional extensions

## Reactive PostGreSQL (R2DBC) library

This optional reactive database library is a reactive version of the PostGreSQL database client
using Spring R2DBC.

It supports four ways to access PostGreSQL:

1. Reactive Repository pattern
2. Database client API
3. PostOffice RPC calls to the PgService using route "postgres.service"
4. PgRequest query and update methods that are wrappers of method-3 above

To use this library, please build it from source using `mvn clean install` or publish it
to your organization artifactory.

You may then use the pg-example as a template to use the reactive PostGreSQL library.

## API playground

The API playground is an example application to deploy multiple OpenAPI 3.0 yaml files to test
various REST endpoints.
