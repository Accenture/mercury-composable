import type { ConnectionRelation } from './connectionRelations';

export interface ConnectionFormState {
  sourceAlias: string;
  targetAlias: string;
  relation: ConnectionRelation | '';
}

export type ConnectionFormValidationErrors = Record<string, string>;
