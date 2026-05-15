import NodeDialog from '../NodeDialog/NodeDialog';
import type { NodeDraft } from '../../graphActions/nodeAuthoringTypes';
import type { AuthoringState } from './useGraphAuthoring';

interface GraphAuthoringModalsProps {
  state: AuthoringState;
  validationErrors: Record<string, string>;
  onDraftChange: (draft: NodeDraft) => void;
  onSubmit: () => void;
  onClose: () => void;
}

export default function GraphAuthoringModals({
  state,
  validationErrors,
  onDraftChange,
  onSubmit,
  onClose,
}: GraphAuthoringModalsProps) {
  if (state.status === 'closed') return null;

  const lockReason =
    state.phase === 'sending'
      ? 'sending'
      : state.connectionLost
        ? 'disconnected'
        : null;

  return (
    <NodeDialog
      open
      mode={state.action === 'edit-node' ? 'edit' : 'create'}
      aliasReadOnly={state.action === 'edit-node'}
      draft={state.draft}
      phase={state.phase}
      lockReason={lockReason}
      serverMessage={state.serverMessage}
      validationErrors={validationErrors}
      onDraftChange={onDraftChange}
      onSubmit={onSubmit}
      onClose={onClose}
    />
  );
}
