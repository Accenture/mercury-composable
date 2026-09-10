- [ ] **First-class starter templates for the three layers.** Eric's ruling (2026-09-10,
  the fresh-agent greenfield discussion): "A first class template for the 3 layers is the
  right move." A fresh AI agent driving the entry-point playbook (AI developer guide,
  "Starting a collaboration") should scaffold from purpose-built starters rather than
  copying reference examples. Open design decisions for a proposal to Eric: WHERE they
  live (starter modules under `examples/`, separate template repositories in the Mercury
  family, or a Maven archetype with a `cargo generate` twin); WHAT each contains (Layer 1:
  functions + rest.yaml; Layer 2: flows + flows.yaml; Layer 3: knowledge-graph app with a
  CompileGraph manifest and Playground dry-run wiring — and whether each ships AI-enabled
  with agent-memory out of the box); and the Rust lock-step shape. Interim state: the
  playbook scaffolds from `lambda-example` / `composable-example` / `minigraph-playground`.
  Next step: design proposal.
  <!-- id: ot-three-layer-starter-templates | created: 2026-09-10 | last_used: 2026-09-10 | uses: 1 | tier: working | origin: 2026-09-10-234940 -->
