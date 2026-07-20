List nodes, connections, graphs or flows
----------------------------------------
The "list nodes" and "list connections" commands list all the nodes and connections of the current graph model
respectively. The "list graphs" and "list flows" commands are read-only DISCOVERY commands: they enumerate the
deployable graph models (each with its root node's "purpose" - living documentation) and the Event Script flows -
the valid extension={graph-id} and extension=flow://{flow-id} delegation targets.

Syntax
------
List all nodes
--------------
```
list nodes
```

List all connections
--------------------
```
list connections
```

List deployable graph models (discovery)
----------------------------------------
```
list graphs
```

List Event Script flows (discovery)
-----------------------------------
```
list flows
```
