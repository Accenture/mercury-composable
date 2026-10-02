// @vitest-environment happy-dom

import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ReactNode } from 'react';
import Playground from '../Playground';
import { PLAYGROUND_CONFIGS } from '../../config/playgrounds';

const testDoubles = vi.hoisted(() => {
  const noop = vi.fn();
  return {
    noop,
    autoHelpNavigate: vi.fn(),
    rightPanelRender: vi.fn(),
    context: {
      peekPendingPayload: vi.fn(() => null),
      takePendingPayload: vi.fn(() => null),
      setPendingPayload: noop,
      getSlot: vi.fn(() => ({ phase: 'idle', connectionEpoch: null, messages: [] })),
      connect: noop,
      disconnect: noop,
      send: vi.fn(() => false),
      appendMessage: noop,
      clearMessages: noop,
    },
    websocket: {
      connected: false,
      connecting: false,
      connectionEpoch: null,
      messages: [],
      command: '',
      setCommand: noop,
      connect: noop,
      disconnect: noop,
      sendCommand: noop,
      handleKeyDown: noop,
      consoleRef: { current: null },
      copyMessages: noop,
      clearMessages: noop,
      uploadPayload: noop,
      sendRawText: vi.fn(() => false),
      appendMessage: noop,
      history: [],
    },
  };
});

vi.mock('react-router', () => ({ useNavigate: () => testDoubles.noop }));

// The panel doubles expose their size props as data attributes, so the split
// tests can read the latest render from the DOM.
vi.mock('react-resizable-panels', () => ({
  Group: ({ children, defaultLayout }: { children: ReactNode; defaultLayout?: unknown }) => (
    <div
      data-testid="panel-group"
      data-default-layout={defaultLayout === undefined ? undefined : JSON.stringify(defaultLayout)}
    >
      {children}
    </div>
  ),
  Panel: ({ children, defaultSize }: { children: ReactNode; defaultSize?: string }) => (
    <div data-testid="resizable-panel" data-default-size={defaultSize}>{children}</div>
  ),
  Separator: () => <div />,
}));

// The production module uses Vite raw-file globs outside the webapp root.
// This component test exercises profile wiring, so a small content boundary is enough.
vi.mock('../../data/helpContent', () => ({
  getHelpContent: (topic: string, profile = 'minigraph') => (
    profile === 'json-path' && topic === '' ? '# JSON-Path Overview' : null
  ),
}));

vi.mock('../../hooks/useToast', () => ({
  useToast: () => ({ toasts: [], addToast: testDoubles.noop, removeToast: testDoubles.noop }),
}));
vi.mock('../../hooks/useWebSocket', () => ({ useWebSocket: () => testDoubles.websocket }));
vi.mock('../../hooks/useMediaQuery', () => ({ useMediaQuery: () => false }));
vi.mock('../../hooks/useGraphData', () => ({
  useGraphData: () => ({
    graphData: null,
    setGraphData: testDoubles.noop,
    rightTab: 'payload',
    setRightTab: testDoubles.noop,
    isRefreshing: false,
    refetchGraph: testDoubles.noop,
  }),
}));
vi.mock('../../hooks/useAutoGraphRefresh', () => ({ useAutoGraphRefresh: testDoubles.noop }));
vi.mock('../../hooks/useAutoHelpNavigate', () => ({
  useAutoHelpNavigate: testDoubles.autoHelpNavigate,
}));
vi.mock('../../hooks/useSendToJsonPath', () => ({
  useSendToJsonPath: () => ({ handleSendToJsonPath: testDoubles.noop }),
}));
vi.mock('../../hooks/useMockUploadPanel', () => ({
  useMockUploadPanel: () => ({
    uploadPanelPath: null,
    successfulUploadPaths: new Set(),
    handleOpenUploadPanel: testDoubles.noop,
    handleCloseUploadPanel: testDoubles.noop,
    handleCloseUploadPath: testDoubles.noop,
    handleUploadSuccess: testDoubles.noop,
    handleUploadError: testDoubles.noop,
    resetSuccessfulPaths: testDoubles.noop,
  }),
}));
vi.mock('../../hooks/useLargePayloadDownload', () => ({ useLargePayloadDownload: testDoubles.noop }));
vi.mock('../../hooks/useSavedGraphs', () => ({
  useSavedGraphs: () => ({
    savedGraphs: [],
    saveGraph: testDoubles.noop,
    deleteGraph: testDoubles.noop,
    hasGraph: vi.fn(() => false),
  }),
}));
vi.mock('../../hooks/useGraphSaveName', () => ({
  useGraphSaveName: () => ({ defaultName: 'untitled-1', savedName: null, resetName: testDoubles.noop }),
}));
vi.mock('../../hooks/useSavedGraphWorkflow', () => ({
  useSavedGraphWorkflow: () => ({ handleSaveGraph: testDoubles.noop, handleLoadGraph: testDoubles.noop }),
}));
vi.mock('../../contexts/WebSocketContext', () => ({
  useWebSocketContext: () => testDoubles.context,
}));
vi.mock('../../contexts/ClipboardContext', () => ({
  useClipboardContext: () => ({
    items: [],
    clipNode: testDoubles.noop,
    confirmReplace: testDoubles.noop,
  }),
}));
vi.mock('../../protocol/useProtocolKernel', () => ({
  useProtocolKernel: () => ({ classificationMap: new Map() }),
}));
vi.mock('../GraphAuthoring/useGraphAuthoring', () => ({
  useGraphAuthoring: () => ({
    state: { status: 'closed', pendingSubmit: null, serverMessage: null },
    validationErrors: {},
    updateFormState: testDoubles.noop,
    submit: testDoubles.noop,
    close: testDoubles.noop,
    openCreateNode: testDoubles.noop,
    openCreateConnection: testDoubles.noop,
    openEditNode: testDoubles.noop,
    deleteNode: testDoubles.noop,
    deleteNodes: testDoubles.noop,
  }),
}));
vi.mock('../../session/useSessionCollaboration', () => ({
  useSessionCollaboration: () => ({
    state: { sessionId: 'ws-123456-1' },
    isPrimary: true,
  }),
}));

vi.mock('../Toast', () => ({ ToastContainer: () => null }));
vi.mock('../Navigation', () => ({ default: () => null }));
vi.mock('../GraphSaveButton/GraphSaveButton', () => ({
  default: () => <div data-testid="save-graph-control" />,
}));
vi.mock('../SavedGraphsMenu/SavedGraphsMenu', () => ({ default: () => null }));
vi.mock('../LeftPanel/LeftPanel', () => ({ default: () => null }));
vi.mock('../MockUploadPanel/MockUploadPanel', () => ({ default: () => null }));
vi.mock('../ClipboardSidebar/ClipboardSidebar', () => ({ default: () => null }));
vi.mock('../ClipboardSidebar/ClipboardDuplicateDialog', () => ({ ClipboardDuplicateDialog: () => null }));
vi.mock('../HelpBrowser/HelpBrowser', () => ({
  default: ({ contentProfile }: { contentProfile?: string }) => (
    <div data-testid="help-browser" data-profile={contentProfile} />
  ),
}));
vi.mock('../RightPanel/RightPanel', () => ({
  default: (props: {
    helpPanel?: ReactNode | ((onToggleMaximize: () => void, isMaximized: boolean) => ReactNode);
    graphRunControls?: unknown;
  }) => {
    testDoubles.rightPanelRender(props);
    const { helpPanel } = props;
    return (
      <div data-testid="right-panel">
        {typeof helpPanel === 'function' ? helpPanel(testDoubles.noop, false) : helpPanel}
      </div>
    );
  },
}));

const jsonPathConfig = PLAYGROUND_CONFIGS.find(config => config.path === '/json-path')!;
const minigraphConfig = PLAYGROUND_CONFIGS.find(config => config.path === '/')!;

describe('Playground JSON-Path Help wiring', () => {
  beforeEach(() => {
    localStorage.clear();
    testDoubles.autoHelpNavigate.mockClear();
    testDoubles.rightPanelRender.mockClear();
  });

  afterEach(cleanup);

  it('shows the disconnected Help entry point and opens the JSON-Path profile by click or shortcut', () => {
    render(<Playground config={jsonPathConfig} />);

    expect(screen.getByRole('button', { name: 'Open help panel' })).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('Ctrl + `');
    expect(screen.queryByTestId('help-browser')).toBeNull();
    expect(testDoubles.autoHelpNavigate).toHaveBeenCalledWith(expect.objectContaining({
      enabled: true,
      contentProfile: 'json-path',
    }));

    fireEvent.click(screen.getByRole('button', { name: 'Open help panel' }));

    expect(screen.getByTestId('help-browser').getAttribute('data-profile')).toBe('json-path');
    expect(localStorage.getItem('help-panel-open')).toBe('true');

    fireEvent.click(screen.getByRole('button', { name: 'Close help panel' }));
    fireEvent.keyDown(window, { ctrlKey: true, key: '`' });

    expect(screen.getByTestId('help-browser').getAttribute('data-profile')).toBe('json-path');
  });

  it('restores the persisted open state without replaying the first-load hint', () => {
    localStorage.setItem('help-panel-open', 'true');

    render(<Playground config={jsonPathConfig} />);

    expect(screen.getByRole('button', { name: 'Close help panel' })).toBeTruthy();
    expect(screen.getByTestId('help-browser').getAttribute('data-profile')).toBe('json-path');
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('moves graph-run controls out of the header and wires them only to MiniGraph', () => {
    const { unmount } = render(<Playground config={jsonPathConfig} />);
    expect(screen.queryByRole('group', { name: 'Graph run controls' })).toBeNull();
    expect(testDoubles.rightPanelRender.mock.lastCall?.[0].graphRunControls).toBeUndefined();
    unmount();

    render(<Playground config={minigraphConfig} />);
    expect(screen.getByTestId('save-graph-control')).toBeTruthy();
    expect(screen.queryByRole('group', { name: 'Graph run controls' })).toBeNull();
    expect(testDoubles.rightPanelRender.mock.lastCall?.[0].graphRunControls).toMatchObject({
      phase: 'idle',
      canRun: false,
    });
  });
});

describe('Playground panel split', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(cleanup);

  /** Default widths of the rendered panels, left to right, as percentages. */
  function panelPercents(): number[] {
    return screen.getAllByTestId('resizable-panel')
      .map(panel => parseFloat(panel.getAttribute('data-default-size') ?? 'NaN'));
  }

  it('opens the console at one third and the graph and help at two thirds', () => {
    render(<Playground config={minigraphConfig} />);

    const percents = panelPercents();
    expect(percents).toHaveLength(2);
    expect(percents[0]).toBeCloseTo(100 / 3, 3);
    expect(percents[1]).toBeCloseTo(200 / 3, 3);
  });

  it('ignores a split saved by an earlier version, so a dragged split never replaces the default', () => {
    localStorage.setItem('react-resizable-panels:/-panel-split-v2', JSON.stringify({ _r_1_: 71, _r_3_: 29 }));

    render(<Playground config={minigraphConfig} />);

    expect(screen.getByTestId('panel-group').hasAttribute('data-default-layout')).toBe(false);
    const percents = panelPercents();
    expect(percents[0]).toBeCloseTo(100 / 3, 3);
    expect(percents[1]).toBeCloseTo(200 / 3, 3);
  });

  it('keeps the defaults adding up to the full width when the workspace sidebar is open', () => {
    localStorage.setItem('clipboard-sidebar-open', 'true');

    render(<Playground config={minigraphConfig} />);

    const percents = panelPercents();
    expect(percents).toHaveLength(3);
    expect(percents[0]).toBeCloseTo(100 / 3, 3);
    expect(percents[2]).toBe(20);
    expect(percents.reduce((sum, percent) => sum + percent, 0)).toBeCloseTo(100, 6);
  });

  it('gives the right panel the whole width while the console is hidden', () => {
    localStorage.setItem('console-panel-open', 'false');

    render(<Playground config={minigraphConfig} />);

    expect(panelPercents()).toEqual([100]);
  });
});
