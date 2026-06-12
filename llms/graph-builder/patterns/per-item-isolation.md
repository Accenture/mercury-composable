# Pattern — Per-Item Failure Isolation

Status: **verified** (session `ws-969255-6`, 2026-06-10). Domain-neutral (`item`/`record`); swap the provider URL for your source.

## Intent

Process a list of items one at a time; when **one item's call fails, record that failure as data and keep going** — the batch is not aborted by a bad item. Output is one record per item, including the failed ones.

## Why this isn't `for_each`

`for_each` (tutorial-6) iterates, but **cannot isolate a failing iteration**: all iterations share one node-scoped `.status`/`.error`, and an un-handled failure aborts the whole graph (see [minigraph-syntax.md](../minigraph-syntax.md) `for_each` + `exception`, and the customer-360 evidence). To get isolation you instead **process one item per pass of a bounded `RESET` loop**, each item's call carrying an `exception=` handler that writes a `status:"failed"` record and rejoins the loop. Cost: **sequential** (no concurrency) — that is the isolation↔throughput trade.

## Shape

```
root ─→ loop-check ──(idx>=n? THEN: done)──────────────→ done ─→ end
            │ ELSE: next
            ▼
          pick → pickmap → process ──(ok)──→ record-ok ───┐
                              │ exception                 ├→ collect → collectmap → loop-back
                              └────────→ process-fail ────┘                            │
                                                                       NEXT: loop-check ┘  (RESET head+body)
```

Catalog island holds the source cluster: `root --contains--> catalog --data--> dict-item --provider--> prov-item`.

## Test source (self-contained — run on :8099)

The pattern needs a source where **one item's call fails per-call**. Any stub works; this minimal one returns `200` for every id except `d-fail` → `500` (the `id`/path are placeholders — swap them for your domain):

```js
// stub.mjs — node stub.mjs   (verifies this card; no dependencies)
import { createServer } from 'node:http';
createServer((req, res) => {
  const id = (req.url.match(/^\/content\/([^/?]+)/) || [])[1];
  if (!id) { res.writeHead(404); return res.end('{}'); }
  if (id === 'd-fail') { res.writeHead(500, {'Content-Type':'application/json'}); return res.end('{"error":"upstream"}'); }
  res.writeHead(200, {'Content-Type':'application/json'});
  res.end(JSON.stringify({ id, text: 'body of ' + id }));
}).listen(8099, () => console.error('stub on http://localhost:8099'));
```

## Minimal build (exact commands)

Source cluster (island + one fetcher whose call can fail per-item):

```
create node prov-item
with type Provider
with properties
purpose=item source (one id returns 500)
url=http://127.0.0.1:8099/content/{id}
method=GET
input[]=text(application/json) -> header.accept
input[]=id -> path_parameter.id

create node dict-item
with type Dictionary
with properties
purpose=item value
provider=prov-item
input[]=id
output[]=response.text -> result.value

create node catalog
with type Island
with properties
skill=graph.island
purpose=catalog
```

Loop head — **one** natural edge out (`ELSE: next` → `pick`); the exit is a **named jump** (`THEN: done`), no edge:

```
create node loop-check
with type Decision
with properties
skill=graph.math
statement[]='''
IF: {model.idx} >= {model.n}
THEN: done
ELSE: next
'''
```

Per-item body — pick the current item, call the source, branch ok/fail into a record:

```
create node pick
with type Decision
with properties
skill=graph.js
statement[]=COMPUTE: cur -> ({model.items}||[])[{model.idx}]

create node pickmap
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=pick.result.cur -> model.item

create node process
with type Fetcher
with properties
skill=graph.api.fetcher
dictionary[]=dict-item
exception=process-fail
input[]=model.item -> id
output[]=result.value -> model.itemval

create node record-ok
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=model.item -> model.rec.id
mapping[]=text(ok) -> model.rec.status
mapping[]=text(none) -> model.rec.reason

create node process-fail
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=model.item -> model.rec.id
mapping[]=text(failed) -> model.rec.status
mapping[]=text(fetch_failed) -> model.rec.reason
```

Accumulate + advance + loop — the head `loop-check` is in its **own** RESET list:

```
create node collect
with type Decision
with properties
skill=graph.js
statement[]=COMPUTE: newrecs -> ({model.records}||[]).concat([{model.rec}])
statement[]=COMPUTE: nidx -> ({model.idx}||0)+1

create node collectmap
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=collect.result.newrecs -> model.records
mapping[]=collect.result.nidx -> model.idx

create node loop-back
with type Decision
with properties
skill=graph.math
statement[]=RESET: loop-check, pick, pickmap, process, record-ok, process-fail, collect, collectmap, loop-back
statement[]=NEXT: loop-check

create node done
with type Mapper
with properties
skill=graph.data.mapper
mapping[]=model.records -> output.body.records
```

Plus `root` (Root) and `end` (End). Edges (the only natural fan point is the catalog; everything else is linear or a named jump):

```
connect root to loop-check with next
connect root to catalog with contains
connect catalog to dict-item with data
connect dict-item to prov-item with provider
connect loop-check to pick with next
connect pick to pickmap with next
connect pickmap to process with next
connect process to record-ok with complete
connect record-ok to collect with next
connect process-fail to collect with next
connect collect to collectmap with next
connect collectmap to loop-back with next
connect done to end with next
```

Run (one item id resolves to a 500 from the stub):

```
instantiate graph
text(d-1) -> model.items[]
text(d-fail) -> model.items[]
text(d-pii) -> model.items[]
int(0) -> model.idx
int(3) -> model.n

run --expect output.body.records
```

**Verified output** — `d-fail` 500'd, was recorded, and the batch continued:

```json
[ {"id":"d-1","status":"ok","reason":"none"},
  {"id":"d-fail","status":"failed","reason":"fetch_failed"},
  {"id":"d-pii","status":"ok","reason":"none"} ]
```

## The invariant

Every path through the body converges on `collect` → `collectmap` → `loop-back`, and the failing call's `exception` handler writes a **record**, never an error status. So no item can abort the run; the loop advances `idx` and re-enters until `idx >= n`.

## Failure symptoms if you break it (each observed)

- **Loop runs once then stops** → the re-entry head (`loop-check`) is missing from `loop-back`'s `RESET` list. A non-join node runs once unless reset.
- **Whole graph aborts on the first bad item** → the fetcher has no `exception=` handler, so the failure propagates instead of becoming data. (This is exactly why `for_each` can't isolate.)
- **Graph completes early with too few records** → `loop-check` has a second natural outgoing edge (to `done`); `ELSE: next` fans out to it. Keep one natural edge; route the exit via `THEN: done`.
- **A record carries a stale `reason` from a prior failed item** → `record-ok` doesn't set every field. Write all record fields on both the ok and fail paths (here `reason: none` on ok).

## Constraints

- **Sequential** — no `concurrency`; isolation is bought with throughput.
- Needs a source whose failure is **per-call** (one item's call returns ≥ 400) so the `exception` fires per item.
- Bare literals in the loop `IF` (`>= {model.n}`, not `int(...)`); the tight-loop guard (`graph.node.high.frequency`) is a backstop, not the exit — the `idx >= n` check is.
