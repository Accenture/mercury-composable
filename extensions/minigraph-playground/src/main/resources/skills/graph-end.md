Skill: Graph END
----------------
When a node is configured with this skill of "graph end", it denotes the end of graph execution.

The graph instance will come to an end when the GraphExecutor reaches the node containing this skill.
The output dataset will be returned to the calling application.

Route name
----------
"graph.end"

Setup
-----
To enable this skill for a node, set "skill=graph.end" as a property in a node.
There are no other properties required.

Properties
----------
```
skill=graph.end
```

Execution
---------
Upon successful execution, key-values of the "output." namespace will be returned to the calling application.

Example
-------
```
update node end-of-processing
with properties
skill=graph.end
```
