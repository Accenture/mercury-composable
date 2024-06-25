# Event Script example application

This is a demo application written using Event Script

# Hypothetical profile management system

The demo is a hypothetical user profile management system.

## REST endpoints

The REST endpoints for the demo app are configured in the rest.yaml file like this:

```yaml
rest:
  - service: "http.flow.adapter"
    methods: ['GET']
    url: "/api/profile/{profile_id}"
    flow: 'get-profile'
    timeout: 10s
    cors: cors_1
    headers: header_1
    tracing: true

  - service: "http.flow.adapter"
    methods: ['POST']
    url: "/api/profile"
    flow: 'create-profile'
    timeout: 10s
    cors: cors_1
    headers: header_1
    tracing: true
```

To tell the flow-engine to select the correct flow, you can set the flow ID in the "flow" tag in the REST endpoint
configuration.

## Event Scripting

The event script is defined in a flow YAML file.

For this demo, the flow config files (get-profile.yml, delete-profile.yml and create-profile.yml) are stored
in the "resources/flows" folder. The active flow YAML files are configured in the "flows.yml" under the
"resources" folder.
