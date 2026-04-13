import { describe, it, expect } from 'vitest';
import { classifyMessage } from '../classifier';
import graphMutations from './fixtures/graph-mutations.json';
import graphLinks from './fixtures/graph-links.json';
import exportGraph from './fixtures/export-graph.json';
import largePayloads from './fixtures/large-payloads.json';
import mockUploads from './fixtures/mock-uploads.json';
import uploadContentPaths from './fixtures/upload-content-paths.json';
import helpDescribeCommands from './fixtures/help-describe-commands.json';
import importGraphCommands from './fixtures/import-graph-commands.json';
import lifecycleEvents from './fixtures/lifecycle-events.json';
import jsonResponses from './fixtures/json-responses.json';
import markdownCandidates from './fixtures/markdown-candidates.json';
import negativeCases from './fixtures/negative-cases.json';
import multiEvent from './fixtures/multi-event.json';

interface TestVector {
  name:              string;
  raw:               string;
  expectedKinds:     string[];
  expectedProps?:    Record<string, unknown>;
  notExpectedKinds?: string[];
  expectedLength?:   number;
}

function runFixture(vectors: TestVector[]) {
  for (const v of vectors) {
    it(v.name, () => {
      const events = classifyMessage(1, v.raw);
      const kinds = events.map(e => e.kind);

      // All expected kinds are present
      expect(kinds).toEqual(expect.arrayContaining(v.expectedKinds));

      // Verify length if specified
      if (v.expectedLength !== undefined) {
        expect(kinds).toHaveLength(v.expectedLength);
      }

      // No unexpected kinds
      if (v.notExpectedKinds) {
        for (const nk of v.notExpectedKinds) {
          expect(kinds).not.toContain(nk);
        }
      }

      // At least one emitted event must contain all expected props
      if (v.expectedProps) {
        const match = events.find(e =>
          Object.entries(v.expectedProps!).every(([k, val]) =>
            (e as unknown as Record<string, unknown>)[k] === val
          )
        );
        expect(match).toBeDefined();
      }
    });
  }
}

describe('classifier — graph mutations', () => runFixture(graphMutations));
describe('classifier — graph links', () => runFixture(graphLinks));
describe('classifier — export graph', () => runFixture(exportGraph));
describe('classifier — large payloads', () => runFixture(largePayloads));
describe('classifier — mock uploads', () => runFixture(mockUploads));
describe('classifier — upload content paths', () => runFixture(uploadContentPaths));
describe('classifier — help/describe commands', () => runFixture(helpDescribeCommands));
describe('classifier — import graph commands', () => runFixture(importGraphCommands));
describe('classifier — lifecycle events', () => runFixture(lifecycleEvents));
describe('classifier — json responses', () => runFixture(jsonResponses));
describe('classifier — markdown candidates', () => runFixture(markdownCandidates));
describe('classifier — negative cases', () => runFixture(negativeCases));
describe('classifier — multi-event', () => runFixture(multiEvent));

describe('classifier — invariants', () => {
  it('always returns at least one event', () => {
    const events = classifyMessage(1, '');
    expect(events.length).toBeGreaterThanOrEqual(1);
  });

  it('every event carries the original msgId and raw string', () => {
    const events = classifyMessage(42, 'Node foo created');
    for (const e of events) {
      expect(e.msgId).toBe(42);
      expect(e.raw).toBe('Node foo created');
    }
  });

  it('is pure — same input gives same output', () => {
    const a = classifyMessage(1, '> help');
    const b = classifyMessage(1, '> help');
    expect(a).toEqual(b);
  });

  it('lifecycle JSON does not produce json.response', () => {
    const events = classifyMessage(1, '{"type":"info","message":"connected"}');
    const kinds = events.map(e => e.kind);
    expect(kinds).toContain('lifecycle');
    expect(kinds).not.toContain('json.response');
  });

  it('non-lifecycle JSON produces json.response, not lifecycle', () => {
    const events = classifyMessage(1, '{"name":"test"}');
    const kinds = events.map(e => e.kind);
    expect(kinds).toContain('json.response');
    expect(kinds).not.toContain('lifecycle');
  });
});
