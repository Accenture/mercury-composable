Skill: Graph JS
---------------
When a node is configured with this skill of "graph js", it will execute a set of simple JavaScript statements
to return result. For example, doing mathematical calculation or boolean operation for decision-making.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.js"

Setup
-----
To enable this skill for a node, set "skill=graph.js" as a property in a node.
One or more JavaScript statements can be added to the property "js" and
one or more if-then-else statements to the property "decision".

JavaScript statements in the "js" section will execute before the
boolean JavaScript statements in the "decision" section.

You can configure js or decision or both of them in a single node.

Properties
----------
```
skill=graph.js
js[]=composite.key -> JavaScript statement
decision[]=if-then-else
```

Execution
---------
Upon successful execution of the "js" section, the result set will be stored in the "result" parameter in the
properties of the node. A subsequent data mapper can then map the key-values in the result set to one or more nodes.

For the "decision" section, the system will execute the boolean JavaScript statement one by one.
This process will override the natural graph traversal order and jump to a specific node.
If the function returns "next" after evaluation of all statements, the natural graph traversal order
will be preserved.

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
update node demo-js-runner
with properties
skill=graph.js
js[]=amount -> (1 - {input.body.discount}) * {book.price}
```

Syntax for decision statement
-----------------------------
Each decision statement is a multiline command:
```
IF: JavaScript-statement
THEN: node-name | next
ELSE: node-name | next
```

The "next" keyword tells the system to execute the next statement.

The if-then-else is used to select two options after evaluation of the JavaScript statement.
If the JavaScript statement does not return a boolean value, the following resolution would apply:
1. numeric value - true is positive value and false is negative value
2. text value - "true", "yes", "T", "Y" are positive and all other values are false
3. other value will be converted to a text string first

Example
-------
```
Update node demo-decision-maker
with properties
skill=graph.js
decision[]='''
IF: (1 - {input.body.discount}) * {book.price} > 5000
THEN: high-price
ELSE: low-price
```

The "[]" syntax is used to create and append a list of one or more data mapping entries
