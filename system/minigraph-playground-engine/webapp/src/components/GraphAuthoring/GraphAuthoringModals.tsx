import NodeDialog from '../NodeDialog/NodeDialog';
import ConnectionDialog from '../ConnectionDialog/ConnectionDialog';
import type { NodeFormState } from '../../graphActions/nodeAuthoringTypes';
import type { ConnectionFormState } from '../../graphActions/connectionAuthoringTypes';
import type { AuthoringState } from './useGraphAuthoring';

interface GraphAuthoringModalsProps {
  state: AuthoringState;
  validationErrors: Record<string, string>;
  onFormStateChange: (formState: NodeFormState | ConnectionFormState) => void;
  onSubmit: () => void;
  onClose: () => void;
}

export default function GraphAuthoringModals({
  state,
  validationErrors,
  onFormStateChange,
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

  if (state.action === 'create-connection') {
    return (
      <ConnectionDialog
        open
        formState={state.formState}
        phase={state.phase}
        lockReason={lockReason}
        serverMessage={state.serverMessage}
        validationErrors={validationErrors}
        onFormStateChange={(formState) => onFormStateChange(formState)}
        onSubmit={onSubmit}
        onClose={onClose}
      />
    );
  }

  // Edit-node sessions render as the in-place NodeEditPanel in the left panel
  // slot (hosted by Playground), not as a modal.
  if (state.action === 'edit-node') return null;

  return (
    <NodeDialog
      open
      mode="create"
      aliasReadOnly={false}
      formState={state.formState}
      phase={state.phase}
      lockReason={lockReason}
      serverMessage={state.serverMessage}
      validationErrors={validationErrors}
      onFormStateChange={(formState) => onFormStateChange(formState)}
      onSubmit={onSubmit}
      onClose={onClose}
    />
  );
}
