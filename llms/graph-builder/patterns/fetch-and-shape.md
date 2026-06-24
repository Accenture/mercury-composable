# Pattern — Fetch and Shape (verified, runnable)

> **Verified end-to-end against live HTTP** (session `ws-177533-2`, engine source `f9d77584`, 2026-06-16, against `evidence/stub-server.mjs` on `:8099`). Backed by ledger claim [C-63](../evidence/dogfood-customer-360.md) (and the data-dictionary claims C-15–C-19). For reading one external provider and mapping the response to the output contract.

**Intent:** fetch a profile by `input.body.id` from a provider; return `output.body.profile_name`.

```text
root → fetch-profile → end
            │ (dictionary[]=dict-profile)
dict-profile ──(provider)──> profile-api
```

## The exact build (verified — minimal wiring, C-19)

```
create node profile-api
with type Provider
with properties
purpose=profile service
url=http://localhost:8099/profile/{id}
method=GET
input[]=id -> path_parameter.id
```
*(Provider URL is templated; `{id}` is filled from `path_parameter.id`.)*
```
create node dict-profile
with type Dictionary
with properties
purpose=profile name lookup
provider=profile-api
input[]=id
output[]=response.name -> result.profile_name
```
*(Dictionary binds to the provider and extracts `response.name` (the raw API field) into `result.profile_name`.)*
```
create node fetch-profile
with type Fetcher
with properties
skill=graph.api.fetcher
dictionary[]=dict-profile
input[]=input.body.id -> id
output[]=result.profile_name -> output.body.profile_name
```
```
create node root
with type Root
with properties
purpose=entry
```
```
create node end
with type End
```
```
connect root to fetch-profile with ask
connect dict-profile to profile-api with provider
connect fetch-profile to end with complete
```
```
instantiate graph
text(P-10001) -> input.body.id
```
```
run
```

## Verified result

`output.body` = `{ "profile_name": "Ada Example" }`; `fetch-profile` = `{ result:{profile_name:"Ada Example"}, target:"dict-profile", status:200 }`. The engine made a real `GET /profile/P-10001`.

## The load-bearing points

- **Minimal wiring is enough (C-19):** Provider + Dictionary + Fetcher, the fetcher's `dictionary[]` property, and **one** `connect {dictionary} to {provider} with provider` edge (+ `root→fetcher`, `fetcher→end`). No island/container node, no `contains`/`data` edges.
- **The fetcher can write straight to `output.body` (C-63):** `output[]=result.profile_name -> output.body.profile_name` — no separate post-fetch shape mapper required for a simple pass-through. (Use a shaping mapper only when you need to combine/transform fetched values.)
- **Input chain:** `input.body.id` →(fetcher `input[]`)→ `id` →(dictionary `input[]`)→ `id` →(provider)→ `path_parameter.id` → the URL.
- **Failure handling:** add `exception={handler-alias}` to the fetcher to route a non-2xx to a fallback node (C-20); without it, a fetch failure stops forward traversal and copies the upstream error body into `output.body` (C-22). Provider must be reachable from the engine host.
