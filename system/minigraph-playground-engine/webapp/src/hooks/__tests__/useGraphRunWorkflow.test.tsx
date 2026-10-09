// @vitest-environment happy-dom

import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { MinigraphGraphData } from '../../utils/graphTypes';
import { ProtocolBus } from '../../protocol/bus';
import { classifyMessage } from '../../protocol/classifier';
import { GRAPH_RUN_COMMANDS } from '../../graphRun/graphRunProtocol';
import { GRAPH_RUN_SETUP_TIMEOUT_MS, useGraphRunWorkflow } from '../useGraphRunWorkflow';

const graphWithoutInput: MinigraphGraphData = {
  nodes: [{ alias: 'root', types: ['Root'], properties: { name: 'hello' } }],
  connections: [],
};

const graphWithInput: MinigraphGraphData = {
  nodes: [{
    alias: 'mapper',
    types: ['Task'],
    properties: { mapping: ['input.body.user.id -> model.user_id'] },
  }],
  connections: [],
};

const INSTANCE_CREATED = 'Graph instance created. Loaded 0 mock entries, model.ttl = 30000 ms';

function emitRaw(bus: ProtocolBus, msgId: number, raw: string) {
  for (const event of classifyMessage(msgId, raw)) bus.emit(event);
}

function setup(graphData: MinigraphGraphData = graphWithoutInput) {
  const bus = new ProtocolBus();
  const sendRawText = vi.fn(() => true);
  const addToast = vi.fn();
  const initialProps = {
    connected: true,
    connectionEpoch: 1 as number | null,
    graphData: graphData as MinigraphGraphData | null,
    graphIdentity: '/api/graph/model/current/1' as string | null,
    isPrimary: true,
  };
  const rendered = renderHook(
    (props: typeof initialProps) => useGraphRunWorkflow({
      enabled: true,
      bus,
      sendRawText,
      addToast,
      ...props,
    }),
    { initialProps },
  );
  return { ...rendered, bus, sendRawText, addToast, initialProps };
}

afterEach(() => {
  vi.useRealTimers();
});

describe('useGraphRunWorkflow', () => {
  it('rejects Run and Upload until the instance is acknowledged Ready', () => {
    const { result, bus, sendRawText } = setup();

    expect(result.current.canUpload).toBe(false);
    act(() => expect(result.current.runGraph()).toBe(false));
    expect(sendRawText).not.toHaveBeenCalled();

    act(() => expect(result.current.instantiateGraph()).toBe(true));
    expect(sendRawText).toHaveBeenNthCalledWith(1, GRAPH_RUN_COMMANDS.instantiate);
    expect(result.current.phase).toBe('instantiating');
    expect(result.current.canInstantiate).toBe(false);
    expect(result.current.canUpload).toBe(false);
    expect(result.current.canRun).toBe(false);

    act(() => emitRaw(bus, 1, INSTANCE_CREATED));
    expect(result.current.phase).toBe('ready');
    expect(result.current.canUpload).toBe(true);
    expect(result.current.canRun).toBe(true);

    act(() => expect(result.current.runGraph()).toBe(true));
    expect(sendRawText).toHaveBeenNthCalledWith(2, GRAPH_RUN_COMMANDS.run);
    expect(result.current.phase).toBe('running');
    expect(result.current.canUpload).toBe(false);

    act(() => emitRaw(bus, 2, 'Graph traversal completed in 12 ms'));
    expect(result.current.phase).toBe('idle');
  });

  it('makes a graph that reads input.body Ready right after Instantiate: uploading is optional', () => {
    const { result, bus, sendRawText, addToast } = setup(graphWithInput);

    act(() => result.current.instantiateGraph());
    act(() => emitRaw(bus, 1, INSTANCE_CREATED));

    expect(result.current.phase).toBe('ready');
    expect(result.current.inputBodyPaths).toEqual(['input.body.user.id']);
    expect(result.current.inputHeaderNames).toEqual([]);
    expect(result.current.canUpload).toBe(true);
    expect(result.current.canRun).toBe(true);
    expect(sendRawText).toHaveBeenCalledTimes(1);
    expect(sendRawText).not.toHaveBeenCalledWith('upload mock data');
    expect(addToast).toHaveBeenCalledWith(
      'Graph instantiated. Upload mock input if the run needs it, then run.',
      'success',
    );

    act(() => expect(result.current.runGraph()).toBe(true));
    expect(sendRawText).toHaveBeenNthCalledWith(2, GRAPH_RUN_COMMANDS.run);
  });

  it('toasts the plain ready message for a graph without input.body', () => {
    const { result, bus, addToast } = setup();

    act(() => result.current.instantiateGraph());
    act(() => emitRaw(bus, 1, INSTANCE_CREATED));
    expect(addToast).toHaveBeenCalledWith('Graph instantiated and ready to run.', 'success');
  });

  it('allows Instantiate again while Ready, starting a fresh instance', () => {
    const { result, bus, sendRawText } = setup();

    act(() => result.current.instantiateGraph());
    act(() => emitRaw(bus, 1, INSTANCE_CREATED));
    expect(result.current.canInstantiate).toBe(true);

    act(() => expect(result.current.instantiateGraph()).toBe(true));
    expect(sendRawText).toHaveBeenCalledTimes(2);
    expect(result.current.phase).toBe('instantiating');
    act(() => emitRaw(bus, 2, INSTANCE_CREATED));
    expect(result.current.phase).toBe('ready');
  });

  it('mirrors typed or replayed console instantiate/run events without a toast', () => {
    const { result, bus, sendRawText, addToast } = setup();

    act(() => emitRaw(bus, 1, '> instantiate graph'));
    expect(result.current.phase).toBe('instantiating');
    act(() => emitRaw(bus, 2, 'Graph instance created. Loaded 1 mock entry, model.ttl = 30000 ms'));
    expect(result.current.phase).toBe('ready');
    expect(addToast).not.toHaveBeenCalled();

    act(() => emitRaw(bus, 3, '> run'));
    expect(result.current.phase).toBe('running');
    expect(sendRawText).not.toHaveBeenCalled();
    act(() => emitRaw(bus, 4, 'Graph traversal completed in 5 ms'));
    expect(result.current.phase).toBe('idle');
  });

  it('ignores a replayed upload invitation: the phase and the controls do not change', () => {
    const { result, bus } = setup(graphWithInput);

    act(() => emitRaw(bus, 1, '> instantiate graph'));
    act(() => emitRaw(bus, 2, INSTANCE_CREATED));
    act(() => emitRaw(bus, 3, '> upload mock data'));
    act(() => emitRaw(bus, 4, 'You may upload JSON payload -> POST /api/mock/ws-123-1'));
    act(() => emitRaw(bus, 5, "Mock data loaded into 'input.body' namespace"));

    expect(result.current.phase).toBe('ready');
    expect(result.current.canRun).toBe(true);
    expect(result.current.canUpload).toBe(true);
  });

  it('invalidates stale Ready on graph/session lifecycle changes', () => {
    const { result, bus, rerender, initialProps } = setup();

    act(() => emitRaw(bus, 1, '> instantiate graph'));
    act(() => emitRaw(bus, 2, INSTANCE_CREATED));
    expect(result.current.ready).toBe(true);
    act(() => emitRaw(bus, 3, 'node root updated'));
    expect(result.current.phase).toBe('idle');

    act(() => emitRaw(bus, 4, '> instantiate graph'));
    act(() => emitRaw(bus, 5, INSTANCE_CREATED));
    act(() => rerender({ ...initialProps, graphIdentity: '/api/graph/model/other/2' }));
    expect(result.current.phase).toBe('idle');

    act(() => emitRaw(bus, 6, '> instantiate graph'));
    act(() => emitRaw(bus, 7, INSTANCE_CREATED));
    act(() => rerender({ ...initialProps, connected: false }));
    expect(result.current.phase).toBe('idle');
  });

  it('quarantines a late instantiate acknowledgement after mutation instead of exposing stale Ready', () => {
    const { result, bus, sendRawText } = setup();

    act(() => emitRaw(bus, 1, '> instantiate graph'));
    expect(result.current.phase).toBe('instantiating');
    act(() => emitRaw(bus, 2, 'node root updated'));
    expect(result.current.phase).toBe('outcome-uncertain');

    act(() => emitRaw(bus, 3, INSTANCE_CREATED));
    expect(result.current.phase).toBe('idle');
    expect(sendRawText).not.toHaveBeenCalledWith(GRAPH_RUN_COMMANDS.run);

    act(() => emitRaw(bus, 4, '> instantiate graph'));
    act(() => emitRaw(bus, 5, INSTANCE_CREATED));
    expect(result.current.phase).toBe('ready');
  });

  it('uses an acknowledged session reset to clear an uncertain backend outcome', () => {
    vi.useFakeTimers();
    const { result, bus } = setup();

    act(() => result.current.instantiateGraph());
    act(() => vi.advanceTimersByTime(GRAPH_RUN_SETUP_TIMEOUT_MS));
    expect(result.current.phase).toBe('outcome-uncertain');

    act(() => bus.emit({
      kind: 'session.reset',
      msgId: 2,
      raw: 'Session restarted',
    }));
    expect(result.current.phase).toBe('idle');
  });

  it('invalidates Ready when Save Graph confirms an export', () => {
    const { result, bus } = setup();

    act(() => emitRaw(bus, 1, '> instantiate graph'));
    act(() => emitRaw(bus, 2, INSTANCE_CREATED));
    expect(result.current.ready).toBe(true);

    act(() => bus.emit({
      kind: 'graph.exported',
      msgId: 3,
      raw: 'Graph exported to /tmp/example\nDescribed in /api/graph/model/example/1',
      graphName: 'example',
      apiPath: '/api/graph/model/example/1',
    }));
    expect(result.current.phase).toBe('idle');
  });

  it('reports an aborted run, a setup error, and times out a missing acknowledgement', () => {
    vi.useFakeTimers();
    const { result, bus, addToast } = setup();

    act(() => result.current.instantiateGraph());
    act(() => emitRaw(bus, 1, INSTANCE_CREATED));
    act(() => result.current.runGraph());
    act(() => emitRaw(bus, 2, 'Graph traversal aborted: Profile 100 not found (node fetcher)'));
    expect(result.current.phase).toBe('idle');
    expect(addToast).toHaveBeenCalledWith('Graph run aborted. See the console for details.', 'error');

    act(() => result.current.instantiateGraph());
    act(() => emitRaw(bus, 3, 'ERROR: Root node does not exist'));
    expect(result.current.phase).toBe('idle');
    expect(addToast).toHaveBeenCalledWith('Could not instantiate graph: Root node does not exist', 'error');

    act(() => result.current.instantiateGraph());
    act(() => vi.advanceTimersByTime(GRAPH_RUN_SETUP_TIMEOUT_MS));
    expect(result.current.phase).toBe('outcome-uncertain');
    expect(result.current.busy).toBe(true);
    act(() => expect(result.current.instantiateGraph()).toBe(false));
    expect(addToast).toHaveBeenCalledWith(
      'Graph setup is taking longer than expected. Waiting for the backend outcome…',
      'info',
    );

    act(() => emitRaw(bus, 4, INSTANCE_CREATED));
    expect(result.current.phase).toBe('ready');
  });

  it('lets a subscribed session instantiate and run as an equal partner', () => {
    const { result, rerender, initialProps, bus, sendRawText } = setup(graphWithInput);

    act(() => rerender({ ...initialProps, isPrimary: false }));
    expect(result.current.canInteract).toBe(true);
    expect(result.current.canInstantiate).toBe(true);
    expect(result.current.disabledReason).toBe('');

    act(() => expect(result.current.instantiateGraph()).toBe(true));
    expect(sendRawText).toHaveBeenNthCalledWith(1, GRAPH_RUN_COMMANDS.instantiate);
    act(() => emitRaw(bus, 1, '> instantiate graph'));
    act(() => emitRaw(bus, 2, INSTANCE_CREATED));
    expect(result.current.phase).toBe('ready');
    expect(result.current.canUpload).toBe(true);

    // A graph refetch while subscribed keeps the run (only a topology change resets it).
    act(() => rerender({ ...initialProps, isPrimary: false, graphData: { ...graphWithInput } }));
    expect(result.current.phase).toBe('ready');

    act(() => expect(result.current.runGraph()).toBe(true));
    expect(sendRawText).toHaveBeenNthCalledWith(2, GRAPH_RUN_COMMANDS.run);
    expect(result.current.phase).toBe('running');
  });

  it('resets the run when the session subscribes or unsubscribes', () => {
    const { result, rerender, initialProps, bus } = setup();

    act(() => result.current.instantiateGraph());
    act(() => emitRaw(bus, 1, INSTANCE_CREATED));
    expect(result.current.phase).toBe('ready');

    act(() => rerender({ ...initialProps, isPrimary: false }));
    expect(result.current.phase).toBe('idle');

    act(() => result.current.instantiateGraph());
    act(() => emitRaw(bus, 2, INSTANCE_CREATED));
    expect(result.current.phase).toBe('ready');

    act(() => rerender({ ...initialProps, isPrimary: true }));
    expect(result.current.phase).toBe('idle');
  });
});
