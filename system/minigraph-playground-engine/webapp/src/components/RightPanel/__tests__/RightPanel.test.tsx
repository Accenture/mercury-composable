// @vitest-environment happy-dom

import { type CSSProperties, type ReactNode } from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import RightPanel from '../RightPanel';

const graphViewRender = vi.hoisted(() => vi.fn());

vi.mock('../../GraphView/GraphView', () => ({
  default: (props: Record<string, unknown>) => {
    graphViewRender(props);
    return <div data-testid="graph-view" />;
  },
}));

vi.mock('../../PayloadEditor/PayloadEditor', () => ({
  default: () => <div />,
}));

vi.mock('../../GraphDataView/GraphDataView', () => ({
  default: () => <div />,
}));

vi.mock('react-resizable-panels', () => ({
  Group: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  Panel: ({ children, style }: { children: ReactNode; style?: CSSProperties }) => (
    <div style={style}>{children}</div>
  ),
  Separator: () => <div />,
}));

function renderRightPanel() {
  const graphRunControls = {
    phase: 'idle' as const,
    canInstantiate: true,
    canRun: false,
    disabledReason: '',
    onInstantiate: vi.fn(),
    onRun: vi.fn(),
  };
  return render(
    <RightPanel
      tabs={['graph']}
      payload=""
      onChange={() => {}}
      validation={{ valid: true, error: null, type: null }}
      onFormat={() => {}}
      graphData={{ nodes: [], connections: [] }}
      activeTab="graph"
      onTabChange={() => {}}
      isConnected
      graphRunControls={graphRunControls}
      helpPanel={(onToggleMaximize, isMaximized) => (
        <button type="button" onClick={onToggleMaximize}>
          {isMaximized ? 'Restore help' : 'Maximize help'}
        </button>
      )}
    />
  );
}

describe('RightPanel minimap hint eligibility', () => {
  beforeEach(() => {
    graphViewRender.mockClear();
    sessionStorage.clear();
  });

  afterEach(cleanup);

  it('waits while maximized Help clips the graph, then enables the hint after restore', () => {
    sessionStorage.setItem('help-split-maximized', '1');
    renderRightPanel();

    expect(graphViewRender.mock.lastCall?.[0]).toMatchObject({
      isActive: true,
      minimapHintEligible: false,
    });

    fireEvent.click(screen.getByRole('button', { name: 'Restore help' }));

    expect(graphViewRender.mock.lastCall?.[0]).toMatchObject({
      isActive: true,
      minimapHintEligible: true,
    });
  });

  it('forwards graph-local run controls to GraphView', () => {
    renderRightPanel();

    expect(graphViewRender.mock.lastCall?.[0]).toMatchObject({
      graphRunControls: {
        phase: 'idle',
        canInstantiate: true,
        canRun: false,
      },
    });
  });
});
