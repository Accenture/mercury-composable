Skill: Graph Extension
----------------------
When a node is configured with this skill of "graph extension", it will make an API call to another graph model
and collect result set into the "result" property of the node. In case of exception, the "status" and "result.error"
fields will be set to the node's properties and the graph execution will stop.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.extension"

Setup
-----
To enable this skill for a node, set "skill=graph.extension" as a property in a node.

The following parameters are required in the properties of the node:

1. extension - this should be a valid graph model name in the same memory space
2. mapping - this should include one or more data mapping as input parameters to invoke the API call

The system uses the same syntax of Event Script for data mapping.

Properties
----------
```
skill=graph.extension
extension=graph-id
mapping[]=source -> {parameter}
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
1. "model." namespace for holding intermediate key-values for simple data transformation
2. any key not using the "model." namespace will be mapped as an input parameter

Example
-------
```
create node performance-evaluator
with properties
skill=graph.extension
extension=evaluate-sales-performance
mapping[]=input.body.department_id -> id
```

The "[]" syntax is used to create and append a list of one or more data mapping entries
The "->" signature indicates the direction of mapping where the left-hand-side is source and right-hand-side is target
