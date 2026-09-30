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

There are 7 types of statements:
1. "IF" statement for decision-making
2. "COMPUTE" statement to evaluate a mathematical formula
3. "DECIMAL" statement to evaluate a mathematical formula with exact decimal arithmetic (the high-precision COMPUTE)
4. "CONDITION" statement to evaluate a boolean expression into a declared boolean result
5. "MAPPING" statement to do data mapping from a source to a target variable
6. "EXECUTE" statement to execute another node with "graph.math" skill
7. "RESET" statement to reset the state machine for one or more nodes

You can configure one or more statements of these types.

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

Node cannot be executed more than once
--------------------------------------
To avoid unintended looping, the system guarantees that a node, that has been "seen", is not executed again.

The `reset` command clears the "seen" status and erases its result from the state machine. This is reserved
for advanced use cases that execute a node more than once. *This optional feature must be used with care*.

The following statement resets the node named "previous-node" so that the graph executor can run this node
again when conditional traversal points to the node.

```
statement[]=RESET: previous-node
```

Optional properties
-------------------
```
for_each[]={map an array parameter for iterative statement execution}
statement[]=BEGIN
statement[]=END
statement[]=NEXT: {next-node-name}
statement[]=DELAY: {milliseconds}
```

Execution
---------
Upon successful execution of a "COMPUTE" statement, the result set will be stored in the "result" namespace
of the node. A subsequent "MAPPING" statement can map the key-values in the result set to one or more nodes.

For an "IF" statement, the system will execute a boolean operation.
This process will override the natural graph traversal order and jump to a specific node.
If the function returns "next" after evaluation of all statements, the natural graph traversal order
will be preserved.

Iterative Execution and Begin-End
---------------------------------
Using the optional `for_each` statement, you can tell the skill module to execute the statements iteratively.

A "for_each" statement extracts the next array element from another array variable into a model variable.
You can then put the model variable in the "left-hand-side" of an input statement. The module will then
execute the statement block using an iterative stream of the model variable.

You can also use the `BEGIN` and `END` control statements to select a section of the statements for the
iterative execution based on the "for_each" criteria.

Syntax for COMPUTE statement
----------------------------
It will be a regular JavaScript statement with parameter substitution using the bracket syntax where
the enclosed parameter is a reference to a data attributes in the namespace of "input.", "model." or node name.

When you have more than one JavaScript statement, a subsequent statement can use the result of a prior statement
as its parameters.

Each parameter is wrapped by a set of curly brackets.

Override Graph Traversal
------------------------
Normally the next node is the one or more nodes that this node is connected to.
If you want to tell system to jump to a specific "next-node", you can use the "NEXT:" syntax and put the name
of the node to jump to.

Deferred completion
-------------------
You can add an artificial delay to defer completion of the execution of this node. This is useful to simulate
a slow service for performance test and to pause between retries.

Next and Delay statements
-------------------------
It is a good practice to place the next or delay statement, if any, as last one in the statement block.
However, the placement does not change the behavior because they will only be processed at the end.

Dynamic variables in statement commands
---------------------------------------
Every statement command resolves {dynamic variables}, not only expressions. A NEXT: or THEN:/ELSE:
jump target, a RESET: list entry and a DELAY: value may each be a {namespace.key} reference
resolved at execution time. This is what makes a GENERIC error handler possible - it retries
whichever node routed to it without naming any node:

```
statement[]=RESET: {error.source}, error-handler
statement[]=NEXT: {error.source}
statement[]=DELAY: {model.backoff}
```

An unresolved variable renders "null": a RESET: entry is then a safe no-op, a DELAY: is skipped,
and a jump target fails the run loudly ("Next node 'null' does not exist") - correct for a jump,
so seed the variable before relying on it. A COMPUTE: or IF: expression over an unresolved variable
fails before evaluation and names it ("Unknown identifier: model.backoff (unresolved variable in
'{model.backoff} * 2')") - every unresolved variable when there are several ("Unknown identifier:
model.threshold or model.factor") - so the node that failed to set it can be found. See tutorial 12
for the full generic retry handler.

Limitation
----------
This skill is designed to execute simple inline mathematics or boolean operations that use JavaScript syntax.
For simplicity and speed of execution, the dialect is a closed set: the operators, built-in functions and
constants listed under "Operators and functions" below, and nothing else. There is no assignment, no
user-defined variable or function and no bitwise operator; {variable} substitution is the only variable.
An unlisted function fails by name ("Unknown function: hypot"), never silently.

Operators and functions
-----------------------
The expression dialect accepts exactly the following - an operator, function or constant not listed
here is rejected by name:

```
Literals   : numbers (42, 3.14, .5, 1e-5), strings ('text' or "text"), booleans (true, false)
Variables  : {namespace.key} substitution only - e.g. {input.body.qty}, {model.total}, {book.price};
             an unresolved selector fails by name before evaluation
Operators  : **  exponent, right-associative; a unary operand needs parentheses: -(2 ** 2), never -2 ** 2
             unary + - !          * / % (remainder)          + - (+ concatenates when either side is a string)
             < <= > >= (two numbers, or two strings compared lexically)
             == != (same type on both sides)          && || (short-circuit)          test ? a : b          ( )
Functions  : sin, cos, tan, asin, acos, atan, sqrt, abs, floor, ceil, round, log, log10, exp   (one argument)
             pow(x, y)          min(a, b, ...)          max(a, b, ...)          random()
             every function is also available as Math.name, e.g. Math.pow(2, 3)
Constants  : PI, E (also Math.PI, Math.E)
Not in the dialect: bitwise and shift operators (& | ^ ~ <<), assignment (=), user-defined variables
             and functions, arrays, objects, string methods - use a graph.task function instead
```

Precedence, tightest first: ** > unary > * / % > + - > relational > equality > && > || > ?:

Example
-------
```
create node demo-math-runner
with properties
skill=graph.math
statement[]=COMPUTE: amount -> (1 - {input.body.discount}) * {book.price}
```

The syntax `{variable_name}` is used to resolve the value from the variable into the COMPUTE statement.

Syntax for DECIMAL statement
----------------------------
DECIMAL: variable -> mathematical statement

DECIMAL is the high-precision COMPUTE: the expression is evaluated with exact decimal arithmetic and the
result is stored in the node's "result" namespace as a canonical decimal string (plain notation, the
computed scale kept, a zero of any scale written "0"). COMPUTE is untouched, so a graph that never says
DECIMAL behaves exactly as before.

```
statement[]=DECIMAL: fee -> {input.body.amount} * {input.body.rate}
statement[]=DECIMAL: rounded -> round({price.result.fee}, 2, HALF_UP)
```

Decimals travel as strings: send "100.25", not 100.25. A JSON number arrives as a double, and a DECIMAL
statement rejects a double by name ("Inexact number: input.body.rate (0.0375) ..."), which also catches
the result of a COMPUTE. Whole numbers and strings that spell a number are exact. The result is a string
on purpose: graph.suspend saves the state machine and graph.resume restores it, and a string is the same
after as before.

Arithmetic: + - * are exact; / never truncates (the exact quotient when it terminates, otherwise 34
significant digits, half-even); % is the remainder; ** and pow(x, n) take a whole exponent from -999 to 999;
abs, floor, ceil, min and max are exact. Rounding is always explicit: round(x, scale, mode) with mode
HALF_UP, HALF_EVEN, HALF_DOWN, UP, DOWN, CEILING or FLOOR. What cannot be exact is refused by name: sqrt,
log, log10, exp, trigonometry, random(), PI and E - keep that step in a COMPUTE or a graph.task function.
A DECIMAL statement computes a number; a comparison may appear only inside a ternary test. A COMPUTE on
a decimal string computes in binary floating point, so use DECIMAL for money.

Syntax for CONDITION statement
------------------------------
CONDITION: variable -> boolean expression

The expression is evaluated as a boolean whatever operators it carries - a comparison, a boolean
operation, or a bare boolean variable - and the result is stored as a boolean in the node's
"result" namespace. It is the declared form of a decision value; a COMPUTE stores a boolean only
when its expression happens to carry a comparison or boolean operator.

```
statement[]=CONDITION: eligible -> {member.age} >= 18 && {member.active}
statement[]=CONDITION: same -> {model.flag}
statement[]=MAPPING: check.result.eligible -> output.body.eligible
```

An IF statement may test the stored boolean directly: `IF: {check.result.eligible}`.

Numbers and booleans
--------------------
A boolean is not a number. A boolean where arithmetic, a < or > comparison or a function argument
needs a number fails naming the selector, e.g. "Boolean operand: model.flag (true) in
'{model.flag} + 1' - a boolean is not a number; store a boolean with CONDITION or assert the type
with f:validate". So a JSON true in a numeric slot never computes as 1. Equality (==, !=)
type-checks its two sides; a string that is a canonical number counts as a number, so '200' == 200,
200 == '200' and '200' == '200' are the same comparison and '9.5' < '10.25' compares 9.5 with 10.25.

A misspelled or unsupported function fails by name ("Unknown function: mn").

Arithmetic is IEEE double precision. An overflow to infinity, a division by zero and a NaN each
fail naming the operator ("Arithmetic overflow in '*' (result Infinity)", "Division by zero or
arithmetic overflow in '/'"); integers beyond 2^53 lose precision; round() follows Java's
Math.round (half up toward positive infinity). Money that needs exact decimal arithmetic or a stated
rounding mode belongs in a DECIMAL statement, the high-precision COMPUTE; COMPUTE stays floating point.

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

The syntax `{variable_name}` is used to resolve the value from the variable into the IF statement.

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

Note that the MAPPING statement operates exactly in the same way as a data-mapper so there is
no need to use curly braces to wrap around variables.

Syntax for EXECUTE statement
----------------------------
EXECUTE: another-node

Example
-------
```
statment[]=EXECUTE: math-3
```

The "[]" syntax is used to create and append a list of one or more statements
