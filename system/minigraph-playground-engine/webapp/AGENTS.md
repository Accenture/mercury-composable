# MiniGraph Webapp Agent Instructions

This React/Vite webapp has a scoped memory bank in `memory/`.

Before work that primarily touches this directory, read:

1. `memory/instructions.md`
2. `memory/continuity.md`
3. the latest 2-3 files in `memory/sessions/`

Keep frontend/UI implementation facts, tactical bug fixes, and webapp architecture review notes here.
Use the repository root `memory/` only for framework, Java engine, or cross-boundary contracts that affect
Mercury Composable as a whole.

Identify yourself with your agent name in webapp session logs.

## Project Context

This is a typescript project using raw-http.

The UI has 25 components. See .codesight/components.md for the full list with props.

High-impact files (most imported, changes here affect many other files):
- src/protocol/bus.ts (imported by 12 files)
- src/utils/graphTypes.ts (imported by 11 files)
- src/hooks/useToast.ts (imported by 11 files)
- src/protocol/events.ts (imported by 8 files)
- src/config/playgrounds.ts (imported by 7 files)
- src/clipboard/db.ts (imported by 7 files)
- src/contexts/WebSocketContext.tsx (imported by 6 files)
- src/hooks/useLocalStorage.ts (imported by 5 files)

Required environment variables (no defaults):
- DEV (src/utils/urls.ts)

Read .codesight/wiki/index.md for orientation (WHERE things live). Then read actual source files before implementing. Wiki articles are navigation aids, not implementation guides.
Read .codesight/CODESIGHT.md for the complete AI context map including all routes, schema, components, libraries, config, middleware, and dependency graph.
