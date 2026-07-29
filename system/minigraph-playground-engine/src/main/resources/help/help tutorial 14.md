Tutorial 14
-----------
In this session, you will build an approval workflow that suspends at a human checkpoint and
resumes later - a long-running business process expressed as a sequence of short graph runs.

Pre-requisite
-------------
Workflow suspension persists state to an external store through two composable functions. This
tutorial uses the Redis store from the "minigraph-state-redis" extension - the playground
application already includes it, so "v1.redis.persist.model" and "v1.redis.retrieve.model" are
registered automatically. Start a Redis before you run the graph (the "redis-standalone" helper
application works out of the box).

What is workflow suspension?
----------------------------
An approval may take minutes or days. Instead of parking a live graph instance, the graph
persists its workflow state - the "model" namespace - under the business correlation ID and the
run completes normally. A later request with the same correlation ID restores that state and
continues past the checkpoint without re-executing it. Three vocabulary pieces make this work:

1. the "suspend" node - a reserved node name (like root and end) with the "graph.suspend" skill;
   traversal jumps to it by name
2. a suspensible node - any skilled node with the "suspend=true" property; it routes to the
   suspend node after its skill completes
3. the resume node - the "graph.resume" skill placed right after root; it restores a persisted
   record or lets a fresh transaction flow through (the optional "missing" property names a
   node to handle the no-record case instead)

Create the graph model
----------------------
Create the root node:

```
create node root
with properties
purpose=Approval workflow with suspend and resume
name=tutorial-14
```

Create the resume node. A fresh transaction (no suspended record) jumps to the "check-fresh"
validation gate:

```
create node resume
with type Resume
with properties
purpose=Restore workflow state if this transaction was suspended earlier
skill=graph.resume
task=v1.redis.retrieve.model
missing=check-fresh
```

Create the input validation gate. The variable substitution inside the text() constant is
null-safe: when the request has no "decision" field, the probe value is "=null" - so a fresh
submission proceeds and a decision without a prior submission is rejected:

```
create node check-fresh
with type Decision
with properties
purpose=A decision without a suspended transaction is rejected
skill=graph.math
statement[]=MAPPING: text(={input.body.decision}) -> model.decision_probe
statement[]=IF: {model.decision_probe} == '=null'
THEN: record-request
ELSE: reject
```

Create the suspensible node that captures the submission. Anything a later step needs must be
mapped into the "model" namespace before suspension - the model is the workflow's durable memory:

```
create node record-request
with type Suspensible
with properties
purpose=Capture the submitted request, then suspend for approval
skill=graph.data.mapper
suspend=true
mapping[]=input.body -> model.request
mapping[]=text(pending-approval) -> model.stage
```

Create the suspend node. The name "suspend" is reserved and the "ttl" (duration syntax such as
20s, 5m, 2h, 2d) is mandatory - it becomes the store record's expiry:

```
create node suspend
with type Suspend
with properties
purpose=Persist workflow state to Redis and wait for the approver
skill=graph.suspend
task=v1.redis.persist.model
ttl=1h
```

Create the completion and rejection nodes and the end node:

```
create node approve
with type mapper
with properties
purpose=Complete the workflow with the approver decision and the original request
skill=graph.data.mapper
mapping[]=model.request -> output.body.request
mapping[]=input.body.decision -> output.body.decision
mapping[]=text(completed) -> output.body.stage
```

```
create node reject
with type mapper
with properties
purpose=Reject a decision that has no suspended transaction
skill=graph.data.mapper
mapping[]=int(404) -> output.status
mapping[]=text(rejected) -> output.body.type
mapping[]=text(Transaction not found. Submit the request before sending a decision) -> output.body.message
```

```
create node end
```

Connect the nodes. Draw both edges from the suspensible node - the checkpoint edge to "suspend"
and the continuation edge - so the diagram tells the whole story:

```
connect root to resume with then
connect resume to check-fresh with fresh
connect check-fresh to record-request with submission
connect check-fresh to reject with no-transaction
connect record-request to suspend with checkpoint
connect record-request to approve with approved
connect suspend to end with then
connect approve to end with then
connect reject to end with then
```

For your convenience, this graph model is preloaded as "tutorial-14".

Test the workflow
-----------------
Submit a request - the workflow captures it and suspends:

```
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: order-1001" \
  -d '{"item": "laptop", "amount": 2000}'
```

The caller receives {"type": "suspended", "cid": "order-1001"} and the run is over - nothing
stays in memory. When the approver decides, send the same correlation ID:

```
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: order-1001" \
  -d '{"decision": "approved"}'
```

The response carries the approver's decision AND the original request - the workflow state
crossed the suspension. Now try sending a decision with a correlation ID that never submitted:

```
curl -X POST http://127.0.0.1:8085/api/graph/tutorial-14 \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: order-9999" \
  -d '{"decision": "approved"}'
```

The workflow rejects it with HTTP-404 - the submission must come first. The record is consumed
on resume, so repeating the approval request behaves like a fresh transaction instead of
executing the approval twice.

Summary
-------
In this session, we used the "graph.suspend" and "graph.resume" skills with the "suspend=true"
node property to express a human-in-the-loop approval workflow as two short graph runs keyed by
the business correlation ID, with a Redis state store, a mandatory record expiry, and input
validation that enforces the submission-before-decision order.

Why suspend and resume?
-----------------------
Real business processes wait on people. Suspension turns that waiting into a durable record
instead of a parked runtime: any application instance sharing the state store can resume the
workflow, restarts lose nothing, and each run stays short and observable. The state store is
pluggable - Redis is the packaged implementation, and any composable function honoring the
documented store contract can replace it.
