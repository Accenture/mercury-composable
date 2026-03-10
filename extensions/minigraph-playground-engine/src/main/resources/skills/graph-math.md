Skill: Graph Math
-----------------
When a node is configured with this skill of "graph math", it will execute a set of simple math or boolean statements
to return result. For example, doing mathematical calculation or boolean operation for decision-making.

While your math and/or boolean statements use JavaScript syntax, this skill does not support full JavaScript language.
Its capability is limited to simple math and boolean operations.

Examples for math statement: 
- `COMPUTE: Math.sin(Math.PI / 2) + 1`
- `COMPUTE: value -> x ** 2 + 10 * {interest.rate}`

where "interest" is a node-name and "rate" is a property of the node.
The return value is a floating point number with double precision.

Example for boolean statement: 
- `IF: {member.age} >= 18`
The return value is true or false to execute the THEN or ELSE path.

For performance reason, you should use this skill instead of the "graph.js" skill.

Execution will start when the GraphExecutor reaches the node containing this skill.

Route name
----------
"graph.math"

Setup
-----
To enable this skill for a node, set "skill=graph.math" as a property in a node.
One or more statements can be added.

There are 4 types of statements:
1. "IF" statement for decision-making
2. "COMPUTE" statement to evaluate a mathematical formula
3. "MAPPING" statement to do data mapping from a source to a target variable
4. "EXECUTE" statement to execute another node with "graph.math" skill

You can configure one or more statements of these 3 types.

The system will reject execution if the node contains only "MAP" statements
because it is more efficient to use the "graph.data.mapper" skills for mapping
only operations.

Statements are executed orderly.

Properties
----------
```
skill=graph.math
statement[]=COMPUTE: variable -> mathematical statement
statement[]=IF: if-then-else statement
statement[]=MAPPING: source -> target
statement[]=EXECUTE: another-node
```

Execution
---------
Upon successful execution of a "COMPUTE" statement, the result set will be stored in the "result" namespace
of the node. A subsequent "MAPPING" statement can map the key-values in the result set to one or more nodes.

For an "IF" statement, the system will execute a boolean operation.
This process will override the natural graph traversal order and jump to a specific node.
If the function returns "next" after evaluation of all statements, the natural graph traversal order
will be preserved.

Syntax for COMPUTE statement
----------------------------
It will be a regular JavaScript statement with parameter substitution using the bracket syntax where
the enclosed parameter is a reference to a data attributes in the namespace of "input.", "model." or node name.

When you have more than one JavaScript statement, a subsequent statement can use the result of a prior statement
as its parameters.

Each parameter is wrapped by a set of curly brackets.

Limitation
----------
This skill is designed to execute a simple inline JavaScript statement that uses only standard JavaScript library.
For simplicity and speed of execution, it does not support complex declaration.

Example
-------
```
create node demo-math-runner
with properties
skill=graph.math
statement[]=COMPUTE: amount -> (1 - {input.body.discount}) * {book.price}
```

Syntax for IF statement
-----------------------
Each IF statement is a multiline command:
```
IF: Boolean-operation-statement
THEN: node-name | next
ELSE: node-name | next
```

The "next" keyword tells the system to execute the next statement.

The if-then-else is used to select two options after evaluation of the boolean operation statement.

Example
-------
```
statement[]='''
IF: (1 - {input.body.discount}) * {book.price} > 5000
THEN: high-price
ELSE: low-price
```

Syntax for MAPPING statement
----------------------------
MAPPING: source.composite.key -> target.composite.key

The source composite key can use the following namespaces:
1. "input." namespace to map key-values from the input header or body of an incoming request
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

The target composite key can use the following namespaces:
1. "output." namespace to map key-values to the result set to be returned as response to the calling party
2. Node name (aka 'alias') to map key-values of a node's properties
3. "model." namespace for holding intermediate key-values for simple data transformation

Example
-------
```
statment[]=MAPPING: input.body.hr_id -> employee.id
statement[]=MAPPING: input.body.join_date -> employee.join_date
```

Syntax for EXECUTE statement
----------------------------
EXECUTE: another-node

Example
-------
```
statment[]=EXECUTE: math-3
```

The "[]" syntax is used to create and append a list of one or more statements
