# Review: `docs/ai-grammar-methodology.md` — the agent-memory relationship

**Status:** FEEDBACK — external review of the published white paper's description of its
relationship with agent-memory, with drafted edits for the team to accept, adapt, or decline.
**Reviewer:** Claude Code (agent-memory maintainer session), 2026-09-05
**Scope:** accuracy and attribution only — no position on the paper's thesis or structure,
which read well.

---

## Summary verdict

Nothing in the paper is factually wrong about agent-memory, and the two papers are mutually
consistent (this paper's "Memory explains the project. Grammar explains the platform" is the
exact complement of agent-memory's own "continuity half / legibility half" positioning; the
Rust-engine proof point matches agent-memory's evidence section). The issue is that the
relationship is **systematically understated**: every memory mechanism the paper leans on —
Vision, Blueprint, continuity records, session logs, the orientation loop, the
"mechanize the arithmetic" principle — is agent-memory's, described accurately, but presented
as methodology practice rather than as a named, reusable component, while Mercury's own
mechanisms are named and cited in detail. The tool is named once, in the closing italics,
with no locator and no link from the body's "shared memory" to that name.

## Findings and drafted edits

### F1 — §6 "What Mercury Ships" lists "Project Memory" as a Mercury mechanism

"Project Memory" is the only externally-provided item in a section otherwise about Mercury's
own mechanisms. Mercury ships its memory *content*; the memory *system* (the files, the
protocol, decay/review, the thread layout) is installed and versioned by agent-memory (this
repository is stamped v4.39.2, Mode A, in `.agent/version.md`). As written, a reader
concludes Mercury implements its own memory layer.

**Drafted edit** (replace the first sentence of §6 · Project Memory):

> The project's memory is distinct from the platform's grammar — and unlike the mechanisms
> above, Mercury does not implement it. The repository is AI-enabled with **agent-memory**,
> a vendor-neutral shared-memory and cognitive-loop tool that installs and versions the
> memory layer whose content Mercury's contributors maintain. Vision, Blueprint, continuity,
> session records, and architecture decisions orient each AI session on purpose and state
> rather than only API shape.

### F2 — §7 Step 1 describes agent-memory's enable flow without naming it

"Establish Intent and Memory" is precisely what the tool's "AI enable this repo" flow does
(human-confirmed Vision, derived Blueprint, per-session orientation), yet Step 1 names no
implementation while Steps 2–3 name Mercury's mechanisms concretely. The methodology's first
step is not actionable for a reader outside this repository.

**Drafted edit** (append after the "Memory preserves continuity…" paragraph in Step 1):

> This step has a concrete, reusable implementation: Mercury's repositories are AI-enabled
> with **agent-memory**, which installs the shared memory layer, gates a human-confirmed
> Vision (bootstrapped as a draft, never fabricated), and orients every subsequent session —
> the same tool that maintains this repository's own `memory/`.

### F3 — closing note: "framework", and a dangling license mention

Two small things in the final italics paragraph:

- agent-memory self-describes as a *tool/system* and its non-goals explicitly disclaim
  framework/process weight — "framework" clashes with the thing being described.
- "published under Apache-2.0" with no locator leaves readers unable to find it. Either add
  the locator or drop the license clause; a license without a pointer reads as an oversight.

**Drafted edit, option A (with locator — see Decision below):**

> agent-memory is a lightweight, vendor-neutral shared-memory and cognitive-loop tool for
> human-AI collaboration, published under Apache-2.0
> (github.com/acn-ericlaw/agent-memory); it installs and maintains this repository's shared
> `memory/` layer.

**Drafted edit, option B (no locator):**

> agent-memory is a lightweight, vendor-neutral shared-memory and cognitive-loop tool for
> human-AI collaboration; it installs and maintains this repository's shared `memory/` layer.

### F4 — References: no entry for agent-memory or the companion article (optional)

The References list 19 entries; agent-memory's whitepaper cites mercury-composable by name in
its evidence section, but the reverse citation is absent, and the companion IDD article
appears only in the closing note. If F1/F2 land, a reference entry makes the naming citable:

**Drafted entry:**

> 20. E. Law, *agent-memory: A Lightweight, Vendor-Neutral Memory + Cognitive-Loop System
>     for Predictable AI–Human Delivery*, whitepaper v1.6, September 2026.
>     <https://acn-ericlaw.github.io/agent-memory/>

### F5 — the presentation deck mirrors F1 (minor)

`docs/presentations/ai-grammar-story.html`, slide "The grammar Mercury carries", lists
"Memory layer — the project's grammar — Vision, Blueprint, continuity" with the same
Mercury-ships framing. Slides compress, so this may be acceptable as-is; if F1 lands, the
row's description could read "the project's grammar, via agent-memory — Vision, Blueprint,
continuity".

## Decision for the maintainer

The one genuine judgment call is **attribution depth**: this is an official Accenture
publication, and agent-memory is independent personal research on a personal GitHub
(explicitly "not an official Accenture product" in its own docs). Naming and describing the
tool (F1/F2, F3 option B) is an accuracy fix with no cross-org linking; adding locators
(F3 option A, F4) is a visibility decision only the maintainer can make. The current
minimal footprint may be deliberate — if so, F1's provenance clause and F3's
framework→tool + dangling-license fix are still worth taking, with option B.

---

*Feedback only — no files in `docs/` were changed. Drafted wording is free to use, adapt,
or discard.*

---

## Resolution (2026-09-05, Eric's ruling)

**Accepted and applied now, both engines:** F1 (§6 Project Memory provenance), F2 (Step 1 names
agent-memory), F3 **option B** (framework → tool; license clause dropped in favor of the
installs-and-maintains sentence), F5 (deck row: "the project's grammar, via agent-memory").

**Deferred by design:** F3 option A's locator and F4's reference entry — direct URLs to
agent-memory stay out of this official Accenture publication until agent-memory is promoted to
an official Accenture open-source asset; they land then, at the official URL. Tracked in
`memory/open-threads/thread-ot-agent-memory-attribution-locators.md`.
