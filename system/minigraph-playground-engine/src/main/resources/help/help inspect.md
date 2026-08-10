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
`error.stack` when available. When the failing node is later retried successfully, the
context resolves: code becomes 200, the source stays, and the failure details are removed
— so an empty context means nothing failed, `{source, code: 200}` means recovered, and a
full context means an outstanding failure. The `error` namespace is a first-class
state-machine citizen like `model`, which is why `error` is a reserved node alias.

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
