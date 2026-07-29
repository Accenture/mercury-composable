Run a graph instance
--------------------
1. This command runs a graph instance from a root node. Using graph traversal, it will execute any node with skill
   configured.
2. Each new instance can only be executed once.
3. You must close the current instance and instantiate a new one for the next "run" command.
4. Before traversal begins, the graph is checked against the same whole-graph rules that the
   CompileGraph deployment gate enforces (the suspend/resume contract). Draft authoring allows
   partial models, but a runnable graph must honor these rules - a violation is reported as
   "Unable to run - <reason>" and the run is aborted.

Pre-requisite
-------------
A graph instance is created with the "instantiate" command

Syntax
------
```
run
```
