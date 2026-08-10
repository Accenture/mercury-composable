Inspect state machine
---------------------
This command inspects the state machine containing properties of nodes, input, output and model namespaces.

Pre-requisite
-------------
A graph instance is created with the "instantiate" command

Syntax
------
```
inspect {variable_name}
```
`{variable_name}` is a placeholder — substitute your key and do **not** type the
braces (see the examples). A whole namespace (`input` | `output` | `model` | `error`) is
also valid, e.g. `inspect output`.

After a failed node routes to its exception handler, `inspect error` shows the staged
exception context — `error.source` (the failing node), `error.code`, `error.message` and
`error.stack` when available. The `error` namespace is a first-class state-machine
citizen like `model`, which is why `error` is a reserved node alias.

Examples
--------
```
inspect input.body.user_id
inspect book.price
inspect model.some_variable
inspect output.body.some_key
inspect error
inspect error.source
```
