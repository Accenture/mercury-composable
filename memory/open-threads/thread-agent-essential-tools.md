- [ ] **Essential built-in tools for agent loops: files inside a workspace and the web inside an allowlist, as pack
  functions with published catalog rows (RFC-0013; design sprint 4, Eric's topic of 2026-11-16).** Round 1 drafted:
  `extensions/agent-tools/` in the packs (Python first, Node in lockstep) with `file.read`, `file.write` (`write: true`),
  `file.list` under `tools.file.root`, and `web.fetch`, `web.crawl` under `tools.web.allow` (private ranges refused,
  redirects checked, sizes, pages and depth capped, `robots.txt` honoured, HTML to text in-house); a tool whose sandbox is
  not configured does not register; rows published as files and by a `describe` command; the Playground example
  `site-notes` (a crawl and a supervised write). Spec `draft-design-specs/agent-essential-tools.md`; rulings R1-R7
  (section 9) are Eric's. Next: Eric's review round 1, then the build plan (Python, Node, the example and its drive).
  → proposal: RFC-0013
  → serves: vision-mercury-composable
  <!-- id: agent-essential-tools | created: 2026-11-16 | last_used: 2026-11-16 | uses: 1 | tier: working | origin: 2026-11-16-105602 -->
