export type NodeDraftSource = 'empty-graph' | 'pane-context-menu' | 'edit-node';
export type NodeAction = 'create-node' | 'edit-node' | 'delete-node';

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

export interface NodeDraftConversionResult {
  valid: boolean;
  draft: NodeDraft | null;
  message: string | null;
}
