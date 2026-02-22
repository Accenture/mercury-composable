Instantiate from a Graph Model
------------------------------
1. This command creates a graph instance from the current graph model for development and functional tests
2. You must do this before using "execute", "show", "run" and "close" commands
3. The name does not require the ".json" extension
4. You can tell the system to emulate some input variables in the data mapping section
5. The input namespace contains 'body' and 'header'
6. The model namespace is a state machine. It is optional unless you want to emulate some model variables.

Syntax
------
```
instantiate graph
with data mapping
{constant} -> input.body.some_key
{constant} -> input.header.some_parameter
{constant} -> model.some_var
```
