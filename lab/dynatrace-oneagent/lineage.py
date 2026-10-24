#!/usr/bin/env python3
"""The span check of the OneAgent lab: link every Telemetry record of the most recent orders.

Reads lab-edge.log and lab-worker.log (docker logs, JSON lines) from the folder given as the argument, and for each
of the three most recent orders rebuilds Mercury's own span tree by span_id and parent_span_id. An order is CONNECTED
when it has exactly one root (the REST edge, http.request), every span is reachable from it, no span id repeats, and
every record without a span id is an RPC round-trip record with a valid parent. It also lists the Dynatrace trace ids
that the functions' log lines carry (dt.trace_id). Run it with ./lab.sh lineage. Exit code 1 when an order is broken.
"""
import collections
import json
import sys
from datetime import datetime

folder = sys.argv[1]
spans = collections.defaultdict(list)        # mercury trace id -> span records
dt_ids = collections.defaultdict(lambda: collections.defaultdict(set))  # trace -> dt.trace_id -> services
for app in ("lab-edge", "lab-worker"):
    for line in open(f"{folder}/{app}.log"):
        line = line.strip()
        if not line.startswith("{"):
            continue
        try:
            d = json.loads(line)
        except ValueError:
            continue
        msg = d.get("message")
        if isinstance(msg, dict) and "trace" in msg:
            t = dict(msg["trace"]); t["app"] = app
            spans[t.get("id")].append(t)
        ctx = d.get("context") or {}
        if ctx.get("trace_id") and ctx.get("dt.trace_id"):
            dt_ids[ctx["trace_id"]][ctx["dt.trace_id"]].add(f"{app.split('-')[1]}:{ctx.get('service')}")

def ts(s):
    return datetime.strptime(s, "%Y-%m-%dT%H:%M:%S.%fZ")

orders = [tid for tid, rows in spans.items() if any(r.get("service") == "http.request" for r in rows)]
orders.sort(key=lambda tid: min(ts(r["start"]) for r in spans[tid]))
all_ok = True
for tid in orders[-3:]:
    rows = sorted(spans[tid], key=lambda r: ts(r["start"]))
    by_id = {r["span_id"]: r for r in rows if r.get("span_id")}
    no_id = [r for r in rows if not r.get("span_id")]
    roots = [r for r in rows if r.get("span_id") and r.get("parent_span_id") not in by_id]
    children = collections.defaultdict(list)
    for r in rows:
        if r.get("span_id") and r.get("parent_span_id") in by_id:
            children[r["parent_span_id"]].append(r)
    round_trips = collections.defaultdict(list)
    for r in no_id:
        round_trips[r.get("parent_span_id")].append(r)
    orphan_round_trips = [r for r in no_id if r.get("parent_span_id") not in by_id or "round_trip" not in r]
    t0 = ts(rows[0]["start"])
    reached = set()
    print(f"\n=== Mercury trace {tid}  ({len(rows)} span records, {rows[0]['start']})")
    def walk(r, depth):
        reached.add(r["span_id"])
        offset = (ts(r["start"]) - t0).total_seconds() * 1000
        parent = r.get("parent_span_id")
        note = ""
        if r in roots:
            note = f"   <- root; parent {parent} is outside Mercury (OneAgent's server node)" if parent else "   <- root, no parent"
        cross = ""
        if parent in by_id and by_id[parent]["app"] != r["app"]:
            cross = f"   <- crosses Kafka from {by_id[parent]['app']}"
        print(f"  {'   ' * depth}{r['service']:<26} [{r['app']}]  span {r['span_id']}  +{offset:6.1f} ms  {r.get('exec_time', 0):6.2f} ms"
              f"{'' if r.get('success', True) else '  FAILED'}{note}{cross}")
        for c in sorted(children[r["span_id"]], key=lambda x: ts(x["start"])):
            walk(c, depth + 1)
        for rt in round_trips.get(r["span_id"], []):
            print(f"  {'   ' * (depth + 1)}{rt['service']:<26} [{rt['app']}]  (RPC round-trip record from {rt.get('from')}, "
                  f"{rt.get('round_trip')} ms - no span of its own, by design)")
    for root in roots:
        walk(root, 0)
    unreached = [r for r in by_id.values() if r["span_id"] not in reached]
    dup = len(by_id) != len([r for r in rows if r.get("span_id")])
    cross_links = sum(1 for r in by_id.values() if r.get("parent_span_id") in by_id and by_id[r["parent_span_id"]]["app"] != r["app"])
    apps = collections.Counter(r["app"] for r in rows)
    ok = len(roots) == 1 and roots[0]["service"] == "http.request" and not unreached and not dup and not orphan_round_trips
    all_ok &= ok
    print(f"  check: roots={len(roots)} (expect 1, the REST edge), unreached={len(unreached)}, duplicate span ids={dup}, "
          f"round-trip records={len(no_id)} (orphaned {len(orphan_round_trips)}), spans per app={dict(apps)}, links across Kafka={cross_links}  -> {'CONNECTED' if ok else 'BROKEN'}")
    for dt, services in dt_ids[tid].items():
        same = "= Mercury trace id" if dt == tid else "the negative control's fragment" if services == {"edge:lab.audit"} else "OTHER"
        print(f"  Dynatrace trace {dt}  ({same}): {', '.join(sorted(services))}")
print("\nALL CONNECTED" if all_ok else "\nBROKEN SPANS FOUND")
sys.exit(0 if all_ok else 1)
