Skill: Graph API Fetcher
------------------------
When a node is configured with this skill of "graph API fetcher", it will make an API call to a backend service
and collect result set into the "result" property of the node.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.api.fetcher"

Setup
-----
To enable this skill for a node, set "skill=graph.api.fetcher" as a property in a node.
It uses the data dictionary query plugin to make outgoing API calls so that complexity of API protocol
is encapsulated in the configuration of the data dictionary query plugin.

The following parameters are required in the properties of the node:

1. question - this should be a valid question ID configured in the data dictionary query plugin
2. mapping - this should include one or more data mapping as input parameters to invoke the API call

The system uses the same syntax of Event Script for data mapping.

Properties
----------
```
skill=graph.api.fetcher
question=question-id
mapping[]=source -> target
```

Result set
----------
Upon successful execution, the result set will be stored in the "result" parameter in the properties of
the node. A subsequent data mapper can then map the key-values in the result set to one or more nodes.

Syntax for mapping
------------------
source.composite.key -> target.composite.key

The source composite key can use the following namespaces:
1. "input." namespace to map key-values from the input header or body of an incoming request
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

The target composite key can use the following namespaces:
1. "parameter." namespace to map key-values to the input parameters of the outgoing API call
2. "model." namespace for holding intermediate key-values for simple data transformation

Example
-------
```
update node my-api-fetcher
with properties
skill=graph.api.fetcher
question=get-hr-record
mapping[]=input.body.hr_id -> parameter.id
```

The "[]" syntax is used to create and append a list of one or more data mapping entries
The "->" signature indicates the direction of mapping where the left-hand-side is source and right-hand-side is target
