Skill: Graph Decision
---------------------
When a node is configured with this skill of "graph decision", it will execute a set of simple JavaScript statements
to return a boolean result. Based on the result, it will jump to a specific node, thus changing the natural order
of graph traversal.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.decision"

Setup
-----
To enable this skill for a node, set "skill=graph.decision" as a property in a node.
One or more decision statements can be added to the property "decision". Each decision statement
is a multiline command.

Properties
----------
```
skill=graph.decision
decision[]=if-then-else and execute-statement if any
```

Execution
---------
Upon successful execution, this skill will override the natural graph traversal order and jump to a specific node.

Syntax for decision statement
-----------------------------
Each decision statement is a multiline command:

```
IF JavaScript statement
THEN node name | next
ELSE node name | next

EXECUTE data-mapper, JS node or API fetcher
```

The "next" keyword tells the system to execute the next statement.

The if-then-else is used to select two options after evaluation of the JavaScript statement.
If the JavaScript statement does not return a boolean value, the following resolution would apply:
1. numeric value - true is positive value and false is negative value
2. text value - "true", "yes", "T", "Y" are positive and all other values are false
3. other value will be converted to a text string first

The execute statement is used to run a node that has skill of graph.data.mapper, graph.js or graph.api.fetcher
that would update some data attributes. Usually the last statement in a multiline command should be an if-then-else.
Otherwise, the next node in the natural graph traversal will be executed.

Syntax for JavaScript statement
-------------------------------
It will be a regular JavaScript statement with parameter substitution using the bracket syntax where
the enclosed parameter is a reference to a data attributes in the namespace of "input.", "model." or node name.

Example
-------
```
Update node my-js-runner
with properties
skill=graph.js
decision[]='''
IF (1 - {input.body.discount}) * {book.price} > 5000
THEN high-price
ELSE low-price
```

The "[]" syntax is used to create and append a list of one or more data mapping entries
