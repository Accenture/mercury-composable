Skill: Graph JS
---------------
When a node is configured with this skill of "graph js", it will execute a set of simple JavaScript statements
to return result. For example, doing mathematical calculation or boolean operation for decision-making.

Execution will start when the GraphExecutor reaches the node containing this skill.

Function route name
-------------------
"graph.js"

Setup
-----
To enable this skill for a node, set "skill=graph.js" as a property in the node.
One or more JavaScript statements can be added to the property "js".

Properties
----------
```
skill=graph.data.mapper
js[]=composite.key -> JavaScript statement
```

Execution
---------
Upon successful execution, the result set will be stored in the "result" parameter in the properties of
the node. A subsequent data mapper can then map the key-values in the result set to one or more nodes.

Syntax for JavaScript statement
-------------------------------
It will be a regular JavaScript statement with parameter substitution using the bracket syntax where
the enclosed parameter is a reference to a data attributes in the namespace of "input.", "model." or node name.

When you have more than one JavaScript statement, a subsequent statement can use the result of a prior statement
as its parameters.

Each parameters is wrapped by a set of curly brackets.

Limitation
----------
This skill is designed to execute a simple inline JavaScript statement that uses only standard JavaScript library.
For simplicity and speed of execution, it does not support function declaration.

Example
-------
```
Update node my-js-runner
with properties
skill=graph.js
js[]=amount -> (1 - {input.body.discount}) * {book.price}
```

The "[]" syntax is used to create and append a list of one or more data mapping entries
The "->" signature indicates the direction of mapping where the left-hand-side is source and right-hand-side is target
