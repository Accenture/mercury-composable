Tutorial 1
----------
Welcome to the MiniGraph Playground, the self-service user interface for creating amazing applications
using Active Knowledge Graph.

Let's get started.

In this session, you will create the simplest application that returns a "hello world" message.

Exercise
--------

If you can see this page, this means you have successfully started the MiniGraph Playground from a browser
and connected to a designer workbench session.

If your session is disconnected, select the "Tools" dropdown in the top-right corner, click MiniGraph's start
and select "MiniGraph".

Create a starting point of a graph
----------------------------------
**Create a root node** that is the starting point for a graph model.
Select multiline and enter the following command in the bottom-right inbox box.

```terminaloutput
create node root
with type Root
with properties
purpose=Tutorial one to return a 'hello world' message
```

The console displays:

```terminaloutput
> create node root...
Graph with 1 node described in /api/graph/model/ws-875677-2/165-1
```

A drawing will be shown on the right hand side under the "Graph" tab.

This means a graph with a single node called "root" has been created.

`ws-875677-2` is the session ID of the workbench.
`165-1` is a random number for the session that you can ignore.

Create an end node
------------------
An end node is the exit point of a graph model.

Enter the following to create an end node.

```terminaloutput
create node end
with type End
with properties
skill=graph.data.mapper
mapping[]=text(hello world) -> output.body
```

The console displays:

```terminaloutput
> create node end...
Graph with 2 nodes described in /api/graph/model/ws-875677-2/061-2
```

The "skill=graph.data.mapper" assigns the data mapper function to the end node.
In a data mapper, you can do data mapping. 

The mapping statement `mapping[]=text(hello world) -> output.body` tells the
system to map the constant "hello world" to the `output.body` that is the response
payload when the graph is executed. The "[]" syntax means append the data mapping statement
to a list of statements.

First attempt to run a graph
----------------------------
To run a graph model, you can use the `instantiate graph` command.

The console displays:

```terminaloutput
> instantiate graph
Graph instance created. Loaded 0 mock entries, model.ttl = 30000 ms
```

When you enter "instantiate graph", you ask the system to create an "instance"
from a graph model.

You can now try to run the graph by entering the "run" command.

The console displays:

```terminaloutput
> run
Walk to root
```

The system will start running the graph from the starting point. i.e. the root node.
However, nothing happens after that.

What is missing?
----------------
Active Knowledge Graph is a "property graph" that contains one or more "active" nodes.
An active node is associated with a "skill" that is backed by a composable function.

The system will perform graph traversal from the root node. There is nothing happened
because there are no further nodes to reach after the root node.

Connecting nodes
----------------
Please connect the root node to the end node.

Please enter the following command:

```terminaloutput
connect root to end with done
```

The console displays:

```terminaloutput
> connect root to end with done
node root connected to end
Graph with 2 nodes described in /api/graph/model/ws-875677-2/551-3
```

The graph model drawing is updated on the right panel.

Running the graph
-----------------
Now you have a graph that has a start and an ending point where one node contains a skill to do something.
i.e. the end node with a data mapping statement.

You can now instantiate the graph again and run it by entering the following commands.

```terminaloutput
instantiate graph
run
```

The console displays:

```terminaloutput
> instantiate graph
Graph instance created. Loaded 0 mock entries, model.ttl = 30000 ms
> run
Walk to root
Walk to end
Executed end with skill graph.data.mapper in 1.736 ms
{
  "output": {
    "body": "hello world"
  }
}
Graph traversal completed in 9 ms
```

Congratulations. You have create your first MiniGraph that works.
It returns "hello world" when it runs.

Export the graph
----------------
You may now export the graph so that you can deploy it to production.

Enter the export command below:

```terminaloutput
export graph as helloworld
```

This will export the graph model in JSON format with the name `helloworld`
in "/tmp/graph/helloworld.json"

The console displays:

```terminaloutput
> export graph as helloworld
Added name=helloworld to Root node
Graph exported to /tmp/graph/helloworld.json
Described in /api/graph/model/helloworld/436-4
```

Note that the system will add the graph name (i.e. unique "id") to the root node.
This avoids the user from accidentally overwriting an existing graph model.

Help pages
----------
To display more information about each command that you use in this tutorial,
enter the following:

```terminaloutput
help create
help connect
help instantiate
help run
help export
```

Well done. Let's move on to "Tutorial 2".

Solution
--------
The solution for this tutorial is available in the "tutorials" folder of this repository.
