import { useId, useState, type ReactNode } from 'react';
import type { GraphRunPhase } from '../../hooks/useGraphRunWorkflow';
import { InstantiateIcon, RunIcon, UploadIcon } from '../../icons/GraphToolbarIcons';
import styles from './GraphToolbar.module.css';

export interface GraphRunControlsProps {
  phase: GraphRunPhase;
  canInstantiate: boolean;
  canUpload: boolean;
  canRun: boolean;
  disabledReason: string;
  /** The `input.body.*` paths the graph references — a hint on the Upload action only. */
  inputBodyPaths?: string[];
  /** The input header names the graph references — a hint on the Upload action only. */
  inputHeaderNames?: string[];
  onInstantiate: () => void;
  onUpload: () => void;
  onRun: () => void;
}

const SETUP_BUSY_PHASES = new Set<GraphRunPhase>(['instantiating', 'outcome-uncertain']);

function instantiateLabel(phase: GraphRunPhase): string {
  switch (phase) {
    case 'instantiating': return 'Instantiating…';
    case 'outcome-uncertain': return 'Waiting…';
    default: return 'Instantiate';
  }
}

function instantiateAriaLabel(phase: GraphRunPhase): string {
  switch (phase) {
    case 'instantiating': return 'Graph is being instantiated';
    case 'outcome-uncertain': return 'Waiting for the backend graph outcome';
    default: return 'Instantiate graph';
  }
}

function busyTitle(phase: GraphRunPhase): string {
  switch (phase) {
    case 'instantiating': return 'Graph is being instantiated';
    case 'running': return 'Graph is running';
    case 'outcome-uncertain': return 'Waiting for the backend graph outcome';
    default: return '';
  }
}

function appendStatus(purpose: string, status: string): string {
  if (!status) return purpose;
  const statusSentence = /[.!?]$/.test(status) ? status : `${status}.`;
  return `${purpose} ${statusSentence}`;
}

function inputHint(inputBodyPaths: string[], inputHeaderNames: string[]): string {
  const parts: string[] = [];
  if (inputBodyPaths.length > 0) {
    parts.push(`${inputBodyPaths.length} input.body ${inputBodyPaths.length === 1 ? 'path' : 'paths'}`);
  }
  if (inputHeaderNames.length > 0) {
    parts.push(`${inputHeaderNames.length} input ${inputHeaderNames.length === 1 ? 'header' : 'headers'}`);
  }
  if (parts.length === 0) return 'This graph reads neither input.body nor input.header, so uploading is optional';
  return `This graph reads ${parts.join(' and ')}`;
}

interface ActionTooltipProps {
  id: string;
  text: string;
  keyboardFallback: boolean;
  children: ReactNode;
}

function ActionTooltip({ id, text, keyboardFallback, children }: ActionTooltipProps) {
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);

  return (
    <span
      className={styles.tooltipAnchor}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocusCapture={() => setFocused(true)}
      onBlurCapture={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false);
      }}
      tabIndex={keyboardFallback ? 0 : undefined}
      aria-describedby={keyboardFallback ? id : undefined}
    >
      {children}
      <span
        id={id}
        className={styles.actionTooltip}
        role="tooltip"
        data-state={hovered || focused ? 'open' : 'closed'}
      >
        {text}
      </span>
    </span>
  );
}

/**
 * The three-step run lifecycle: Instantiate creates the instance (every
 * member of a collaborative session gets one), Upload opens this session's
 * mock-input form (optional — only the member who clicks it sees the form;
 * the engine loads the payload into every member's instance), Run runs it.
 */
export default function GraphRunControls({
  phase,
  canInstantiate,
  canUpload,
  canRun,
  disabledReason,
  inputBodyPaths = [],
  inputHeaderNames = [],
  onInstantiate,
  onUpload,
  onRun,
}: GraphRunControlsProps) {
  const tooltipId = useId();
  const instantiateTooltipId = `${tooltipId}-instantiate`;
  const uploadTooltipId = `${tooltipId}-upload`;
  const runTooltipId = `${tooltipId}-run`;
  const setupBusy = SETUP_BUSY_PHASES.has(phase);
  const phaseTitle = busyTitle(phase);
  const instantiateStatus = disabledReason || phaseTitle || (
    phase === 'ready' ? 'Instantiating again starts from a fresh instance' : ''
  );
  const uploadStatus = disabledReason || phaseTitle || (
    !canUpload ? 'Instantiate the graph first' : inputHint(inputBodyPaths, inputHeaderNames)
  );
  const runStatus = disabledReason || phaseTitle || (!canRun ? 'Instantiate the graph first' : '');
  const instantiateTooltip = appendStatus(
    'Create a runnable instance of the current graph.',
    instantiateStatus,
  );
  const uploadTooltip = appendStatus(
    'Upload a JSON payload as the mock input.body of the instance, and optional mock headers as its input.header. Only you see the form.',
    uploadStatus,
  );
  const runTooltip = appendStatus(
    'Run the instantiated graph, with the uploaded mock input if any.',
    runStatus,
  );

  return (
    <div className={styles.runControls} role="group" aria-label="Graph run controls">
      <ActionTooltip
        id={instantiateTooltipId}
        text={instantiateTooltip}
        keyboardFallback={!canInstantiate}
      >
        <button
          type="button"
          className={styles.toolbarButton}
          onClick={onInstantiate}
          disabled={!canInstantiate}
          aria-label={instantiateAriaLabel(phase)}
          aria-describedby={instantiateTooltipId}
          aria-busy={setupBusy}
        >
          <InstantiateIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
          <span>{instantiateLabel(phase)}</span>
        </button>
      </ActionTooltip>
      <ActionTooltip id={uploadTooltipId} text={uploadTooltip} keyboardFallback={!canUpload}>
        <button
          type="button"
          className={styles.toolbarButton}
          onClick={onUpload}
          disabled={!canUpload}
          aria-label="Upload mock input"
          aria-describedby={uploadTooltipId}
        >
          <UploadIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
          <span>Upload</span>
        </button>
      </ActionTooltip>
      <ActionTooltip id={runTooltipId} text={runTooltip} keyboardFallback={!canRun}>
        <button
          type="button"
          className={styles.toolbarButton}
          onClick={onRun}
          disabled={!canRun}
          aria-label={phase === 'ready'
            ? 'Run instantiated graph'
            : phase === 'running'
              ? 'Graph is running'
              : 'Run graph'}
          aria-describedby={runTooltipId}
          aria-busy={phase === 'running'}
        >
          <RunIcon className={styles.toolbarIcon} aria-hidden="true" focusable="false" />
          <span>{phase === 'running' ? 'Running…' : 'Run'}</span>
        </button>
      </ActionTooltip>
    </div>
  );
}
