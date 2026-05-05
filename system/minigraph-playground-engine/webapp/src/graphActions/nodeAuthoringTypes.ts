export type NodeDraftSource = 'empty-graph' | 'pane-context-menu';

export interface PropertyRow {
  id: string;
  key: string;
  value: string;
}

export interface NodeDraft {
  alias: string;
  nodeType: string;
  properties: PropertyRow[];
  source: NodeDraftSource;
}

export type NodeDraftValidationErrors = Record<string, string>;
