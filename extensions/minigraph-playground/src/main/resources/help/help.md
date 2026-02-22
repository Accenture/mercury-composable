MiniGraph
---------
A mini-graph is a property graph that is designed to run entirely in memory.
You should limit the number of nodes to less than 500.

Graph Model is used to describe a business use case using graph methodology.
Optionally, you may configure a nodes to have a special skill to react to incoming events.

Instance Model is an instance of a graph model that is used to process a specific business use case
or transaction. It is created when an incoming event arrives. It will map data attributes from input
of a request to properties of one or more nodes.

Execution of an instance model will start from the root node of a graph until it reaches the end node.
Result of the end node will be returned to the calling party.

For a model to be meaningful, you must configure at least one node to have a skill to process the data
attributes of some nodes (aka "data entities"). A skill is a property with the label "skill" and the
value is a composable function route name.

For more information about each feature, try the following help topics.

For graph model
---------------
help create (node)
help delete (node or connection)
help update (node)
help connect (node-A to node-B)
help export
help import
help describe (graph, node, connection or skill)

For instance model
------------------
help instantiate (create an instance from a graph model)
help execute (skill of a specific node. Graph traversal is paused to enable functional test in isolation.)
help inspect (state-machine for properties of nodes, input, output and model namespaces)
help run (execute a graph instance from a root node to the end node, if any, using graph traversal.)
help close (close a graph instance)
