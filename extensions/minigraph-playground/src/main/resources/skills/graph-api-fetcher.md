Skill: Graph API Fetcher
------------------------
When a node is configured with this skill of "graph API fetcher", it will make an API call to a backend service
and collect result set into the "result" property of the node. In case of exception, the "status" and "result.error"
fields will be set to the node's properties and the graph execution will stop.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.api.fetcher"

Setup
-----
To enable this skill for a node, set "skill=graph.api.fetcher" as a property in a node.
It will find out the data provider from a given data dictionary item to make an outgoing API call.

The following parameters are required in the properties of the node:

1. dictionary - this is a list of valid data dictionary node names configured in the same graph model
2. mapping - this should include one or more data mapping as input parameters to invoke the API call

The parameter name in each mapping statement must match that in the data dictionary item.
Otherwise, execution will fail.

The system uses the same syntax of Event Script for data mapping.

Properties
----------
```
skill=graph.api.fetcher
dictionary[]={data dictionary item}
input[]={mapping of key-value from input or another node to input parameter(s) of the data dictionary item(s)}
output[]={mapping of result set to key-values of another node}
```

Result set
----------
Upon successful execution, the result set will be stored in the "result" parameter in the properties of
the node. A subsequent data mapper can then map the key-values in the result set to one or more nodes.

Data mapping
------------
source.composite.key -> target.composite.key

For input data mapping, the source can use a key-value from the `input.` namespace or another node.
The target can be a key-value in the state machine (`model.` namespace) or an input parameter name of the
data dictionary.

For output data mapping, the source can be a key-value from the result set and the target can use
the `output.` or `model.` namespace.

Output data mapping is optional because you can use another data mapper to map result set of the fetcher
to another node.

Example
-------
```
create node my-api-fetcher
with properties
skill=graph.api.fetcher
dictionary[]=person_name
dictionary[]=person_address
input[]=input.body.person_id -> person_id
output[]=result.person_name -> output.body.name
output[]=result.person_address -> output.body.address
```

- The "[]" syntax is used to create and append a list of one or more data mapping entries
- The "->" signature indicates the direction of mapping where the left-hand-side is a source
  and right-hand-side is a target
