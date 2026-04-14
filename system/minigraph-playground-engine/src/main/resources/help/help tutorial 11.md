Tutorial 11
-----------
In this session, you will create a graph model to use an "event flow" as an extension.

Pre-requisite
-------------
You would need some working knowledge with event script. For more details, please refer to
[Event Script Syntax](https://accenture.github.io/mercury-composable/guides/CHAPTER-4).

Assume you already know how to create an event flow (configuration and composable functions as tasks),
it is easy to use event flow as an extension.

What is a flow extension?
-------------------------
A flow extension is an event flow that is built to serve some logic that can be reused by a graph model.

Create a node to use an extension
---------------------------------
Enter the following to create an extension node. The skill is 'extension' and the extension is 'flow://my-flow'.

The input mapping sets the input parameter(s) to an extension which is also a graph model.
The output mapping sets the result from the extension to the output payload.

```
create node extension
with type Extension
with properties
skill=graph.extension
extension=flow://my-flow
input[]=input.body.person_id -> person_id
output[]=result -> output.body
```

*Note*: the flow://my-flow and the input parameters are placeholder examples.

Using event flow as an extension is similar to using a graph model as an extension.
The only difference is the use of the protocol prefix `flow://` followed by a flow-id.

Since this is an advanced topic, model answer is not provided. You will try this as an exercise.

Summary
-------
In this session, we have discussed the use of an event flow as an extension to a graph model and
the use of the flow protocol prefix "flow://".

Why extending a graph model with event flow?
--------------------------------------------
While graph extension discussed in tutorial 10 can create sophisticated and powerful graph models,
extending a graph with event flow allows us to do things beyond simple API fetching, data mapping, computation
and decision-making.

With event flow, you can model very complex transaction processing with "pro-code". The combined graph modeling
and event script programming provides the best of both worlds in no-code and pro-code to tackle the most
demanding use cases.
