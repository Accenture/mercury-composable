// @vitest-environment happy-dom

import {
  useState,
  type ButtonHTMLAttributes,
  type CSSProperties,
  type ReactNode,
} from 'react';
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import GraphMinimap from '../GraphMinimap';
import { useMinimapHint } from '../useMinimapHint';

const miniMapRender = vi.hoisted(() => vi.fn());

vi.mock('@xyflow/react', () => ({
  MiniMap: (props: Record<string, unknown>) => {
    miniMapRender(props);
    return <div data-testid="graph-minimap" />;
  },
  Panel: ({ children, position, style }: {
    children: ReactNode;
    position?: string;
    style?: CSSProperties;
  }) => (
    <div
      data-testid="minimap-toggle-panel"
      data-position={position}
      data-bottom={style?.bottom}
    >
      {children}
    </div>
  ),
  Controls: ({
    children,
    className,
    position = 'bottom-left',
    showInteractive = true,
  }: {
    children?: ReactNode;
    className?: string;
    position?: string;
    showInteractive?: boolean;
  }) => (
    <div
      className={`react-flow__controls ${className ?? ''}`}
      data-testid="rf-controls"
      data-position={position}
      data-show-interactive={showInteractive}
    >
      <button type="button" className="react-flow__controls-button" aria-label="Zoom in" />
      <button type="button" className="react-flow__controls-button" aria-label="Zoom out" />
      <button type="button" className="react-flow__controls-button" aria-label="Fit view" />
      {showInteractive && (
        <button
          type="button"
          className="react-flow__controls-button"
          aria-label="Toggle interactivity"
        />
      )}
      {children}
    </div>
  ),
  ControlButton: ({ children, className, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button
      type="button"
      className={`react-flow__controls-button ${className ?? ''}`}
      {...props}
    >
      {children}
    </button>
  ),
}));

function GraphMinimapHarness({
  initialOpen = false,
  initialHintVisible = false,
  hintFading = false,
  hotkeyEnabled = true,
}: {
  initialOpen?: boolean;
  initialHintVisible?: boolean;
  hintFading?: boolean;
  hotkeyEnabled?: boolean;
}) {
  const [open, setOpen] = useState(initialOpen);
  const [hintVisible, setHintVisible] = useState(initialHintVisible);
  return (
    <GraphMinimap
      open={open}
      onOpenChange={setOpen}
      hotkeyEnabled={hotkeyEnabled}
      hintVisible={hintVisible}
      hintFading={hintFading}
      onDismissHint={() => setHintVisible(false)}
      onHintFocusChange={() => {}}
    />
  );
}

function EditableTargetHarness() {
  return (
    <>
      <input aria-label="Graph name" />
      <GraphMinimapHarness />
    </>
  );
}

function KeyedBoundaryHarness({ boundaryKey }: { boundaryKey: string }) {
  const [open, setOpen] = useState(true);
  return (
    <div key={boundaryKey}>
      <GraphMinimap
        open={open}
        onOpenChange={setOpen}
        hotkeyEnabled
        hintVisible={false}
        hintFading={false}
        onDismissHint={() => {}}
        onHintFocusChange={() => {}}
      />
    </div>
  );
}

function KeyedHintBoundaryHarness({ boundaryKey }: { boundaryKey: string }) {
  const [open, setOpen] = useState(false);
  const [hintVisible, setHintVisible] = useState(true);
  return (
    <div key={boundaryKey}>
      <GraphMinimap
        open={open}
        onOpenChange={setOpen}
        hotkeyEnabled
        hintVisible={hintVisible}
        hintFading={false}
        onDismissHint={() => setHintVisible(false)}
        onHintFocusChange={() => {}}
      />
    </div>
  );
}

function MinimapHintLifecycleHarness({
  boundaryKey = 'root',
  eligible,
}: {
  boundaryKey?: string;
  eligible: boolean;
}) {
  const [open, setOpen] = useState(false);
  const {
    hintVisible,
    hintFading,
    dismissHint,
    dismissHintImmediately,
    setHintFocused,
  } = useMinimapHint(eligible);

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (nextOpen) dismissHintImmediately();
  };

  return (
    <div key={boundaryKey}>
      <GraphMinimap
        open={open}
        onOpenChange={handleOpenChange}
        hotkeyEnabled
        hintVisible={hintVisible}
        hintFading={hintFading}
        onDismissHint={dismissHint}
        onHintFocusChange={setHintFocused}
      />
    </div>
  );
}

describe('GraphMinimap', () => {
  beforeEach(() => miniMapRender.mockClear());
  afterEach(() => {
    vi.useRealTimers();
    cleanup();
  });

  it('starts collapsed with the minimap as the bottom button in one native left control group', () => {
    render(<GraphMinimapHarness />);

    const showButton = screen.getByRole('button', { name: 'Show minimap' });
    const controls = screen.getByTestId('rf-controls');
    expect(controls.getAttribute('data-position')).toBe('bottom-left');
    expect(controls.getAttribute('data-show-interactive')).toBe('false');
    expect(within(controls).getAllByRole('button').map((button) => button.getAttribute('aria-label')))
      .toEqual(['Zoom in', 'Zoom out', 'Fit view', 'Show minimap']);
    expect(Array.from(controls.children).filter((child) => (
      child.matches('button.react-flow__controls-button')
    ))).toHaveLength(4);
    expect(controls.lastElementChild).toBe(showButton);
    expect(showButton.className).toContain('react-flow__controls-button');
    expect(showButton.className).toContain('nodrag');
    expect(showButton.className).toContain('nopan');
    expect(showButton.getAttribute('aria-pressed')).toBe('false');
    expect(showButton.getAttribute('aria-keyshortcuts')).toBe('Control+M');
    expect(showButton.getAttribute('title')).toBe('Show minimap (Ctrl + M)');
    expect(showButton.querySelector('svg')?.getAttribute('viewBox')).toBe('1.5 2 17 16');
    expect(screen.queryByTestId('graph-minimap')).toBeNull();

    fireEvent.click(showButton);

    const hideButton = screen.getByRole('button', { name: 'Hide minimap' });
    expect(hideButton.getAttribute('aria-pressed')).toBe('true');
    expect(screen.getByTestId('graph-minimap')).toBeTruthy();

    fireEvent.click(hideButton);

    expect(screen.getByRole('button', { name: 'Show minimap' })).toBeTruthy();
    expect(screen.queryByTestId('graph-minimap')).toBeNull();
  });

  it('toggles the minimap with the Ctrl + M hotkey', () => {
    render(<GraphMinimapHarness />);

    fireEvent.keyDown(window, { key: 'm', code: 'KeyM', ctrlKey: true });

    expect(screen.getByRole('button', { name: 'Hide minimap' })).toBeTruthy();
    expect(screen.getByTestId('graph-minimap')).toBeTruthy();

    fireEvent.keyDown(window, { key: 'M', code: 'KeyM', ctrlKey: true });

    expect(screen.getByRole('button', { name: 'Show minimap' })).toBeTruthy();
    expect(screen.queryByTestId('graph-minimap')).toBeNull();
  });

  it('shows and dismisses a canvas-safe Ctrl + M onboarding hint on the logo', () => {
    render(<GraphMinimapHarness initialHintVisible />);

    const toggle = screen.getByRole('button', { name: 'Show minimap' });
    const hint = screen.getByRole('status');
    const dismissButton = screen.getByRole('button', { name: 'Dismiss minimap shortcut hint' });
    const shortcut = screen.getByText('Ctrl + M', { selector: 'kbd' });

    expect(hint.textContent).toContain('Ctrl + M to toggle minimap');
    expect(screen.getByTestId('rf-controls').contains(hint)).toBe(true);
    expect(screen.getByTestId('rf-controls').lastElementChild).toBe(toggle);
    expect(hint.className).toContain('nodrag');
    expect(hint.className).toContain('nopan');
    expect(toggle.className).toContain('toggleButtonPulsing');
    expect(toggle.getAttribute('aria-describedby')).toBe(hint.id);
    expect(shortcut).toBeTruthy();

    fireEvent.click(dismissButton);

    expect(screen.queryByRole('status')).toBeNull();
    expect(toggle.getAttribute('aria-describedby')).toBeNull();
    expect(toggle.className).not.toContain('toggleButtonPulsing');
  });

  it('dismisses the onboarding hint when the minimap opens', () => {
    render(<GraphMinimapHarness initialHintVisible />);

    expect(screen.getByRole('status')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Show minimap' }));

    expect(screen.getByRole('button', { name: 'Hide minimap' })).toBeTruthy();
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('does not intercept typing, other shortcuts, repeats, or prevented events', () => {
    render(<EditableTargetHarness />);

    fireEvent.keyDown(screen.getByRole('textbox', { name: 'Graph name' }), {
      key: 'm',
      code: 'KeyM',
      ctrlKey: true,
    });
    fireEvent.keyDown(window, { key: 'm', code: 'KeyM' });
    fireEvent.keyDown(window, { key: 'm', code: 'KeyM', altKey: true });
    fireEvent.keyDown(window, { key: 'm', code: 'KeyM', ctrlKey: true, metaKey: true });
    fireEvent.keyDown(window, { key: 'm', code: 'KeyM', ctrlKey: true, altKey: true });
    fireEvent.keyDown(window, { key: 'm', code: 'KeyM', ctrlKey: true, shiftKey: true });
    fireEvent.keyDown(window, { key: 'm', code: 'KeyM', ctrlKey: true, repeat: true });

    const preventedEvent = new KeyboardEvent('keydown', {
      key: 'm',
      code: 'KeyM',
      ctrlKey: true,
      bubbles: true,
      cancelable: true,
    });
    preventedEvent.preventDefault();
    fireEvent(window, preventedEvent);

    expect(screen.getByRole('button', { name: 'Show minimap' })).toBeTruthy();
    expect(screen.queryByTestId('graph-minimap')).toBeNull();
  });

  it('does not toggle while the graph tab is inactive', () => {
    render(<GraphMinimapHarness hotkeyEnabled={false} />);

    fireEvent.keyDown(window, { key: 'm', code: 'KeyM', ctrlKey: true });

    expect(screen.getByRole('button', { name: 'Show minimap' })).toBeTruthy();
    expect(screen.queryByTestId('graph-minimap')).toBeNull();
  });

  it('places the legacy minimap beside the left controls and enables drag navigation without wheel zoom', () => {
    render(<GraphMinimapHarness />);
    fireEvent.click(screen.getByRole('button', { name: 'Show minimap' }));

    const lastRender = miniMapRender.mock.calls[miniMapRender.mock.calls.length - 1];
    const props = lastRender[0] as {
      maskColor?: string;
      nodeColor?: (node: { type?: string }) => string;
      pannable?: boolean;
      style?: Record<string, unknown>;
      position?: string;
      zoomable?: boolean;
    };

    expect(props.maskColor).toBe('rgba(0,0,0,0.3)');
    expect(props.style).toEqual({ background: '#fff', left: '35px' });
    expect(props.position).toBe('bottom-left');
    expect(props.pannable).toBe(true);
    expect(props.zoomable).toBeUndefined();
    expect({
      Root: props.nodeColor?.({ type: 'Root' }),
      End: props.nodeColor?.({ type: 'End' }),
      Fetcher: props.nodeColor?.({ type: 'Fetcher' }),
      mapper: props.nodeColor?.({ type: 'mapper' }),
      Math: props.nodeColor?.({ type: 'Math' }),
      JavaScript: props.nodeColor?.({ type: 'JavaScript' }),
      Provider: props.nodeColor?.({ type: 'Provider' }),
      Dictionary: props.nodeColor?.({ type: 'Dictionary' }),
      Join: props.nodeColor?.({ type: 'Join' }),
      Extension: props.nodeColor?.({ type: 'Extension' }),
      Island: props.nodeColor?.({ type: 'Island' }),
      Decision: props.nodeColor?.({ type: 'Decision' }),
      fallback: props.nodeColor?.({ type: 'Unknown' }),
    }).toEqual({
      Root: '#15803d',
      End: '#dc2626',
      Fetcher: '#2563eb',
      mapper: '#ea580c',
      Math: '#a16207',
      JavaScript: '#7e22ce',
      Provider: '#be185d',
      Dictionary: '#0e7490',
      Join: '#65a30d',
      Extension: '#4338ca',
      Island: '#475569',
      Decision: '#b45309',
      fallback: '#6c7086',
    });
  });

  it('keeps an open minimap visible when its keyed canvas boundary remounts', () => {
    const { rerender } = render(<KeyedBoundaryHarness boundaryKey="root" />);

    expect(screen.getByRole('button', { name: 'Hide minimap' })).toBeTruthy();
    expect(screen.getByTestId('graph-minimap')).toBeTruthy();

    rerender(<KeyedBoundaryHarness boundaryKey="root,end" />);

    expect(screen.getByRole('button', { name: 'Hide minimap' })).toBeTruthy();
    expect(screen.getByTestId('graph-minimap')).toBeTruthy();
  });

  it('does not resurrect a dismissed hint when the keyed canvas boundary remounts', () => {
    const { rerender } = render(<KeyedHintBoundaryHarness boundaryKey="root" />);

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss minimap shortcut hint' }));
    expect(screen.queryByRole('status')).toBeNull();

    rerender(<KeyedHintBoundaryHarness boundaryKey="root,end" />);

    expect(screen.queryByRole('status')).toBeNull();
  });

  it('counts three seconds of eligible display time across hidden and restored graph states', () => {
    vi.useFakeTimers();
    const { rerender } = render(<MinimapHintLifecycleHarness eligible />);

    expect(screen.getByRole('status')).toBeTruthy();
    act(() => vi.advanceTimersByTime(1000));

    rerender(<MinimapHintLifecycleHarness eligible={false} />);
    act(() => vi.advanceTimersByTime(5000));
    expect(screen.getByRole('status')).toBeTruthy();

    rerender(<MinimapHintLifecycleHarness eligible />);
    act(() => vi.advanceTimersByTime(1999));
    expect(screen.getByRole('status').className).not.toContain('hintFading');

    act(() => vi.advanceTimersByTime(1));
    expect(screen.getByRole('status').className).toContain('hintFading');

    act(() => vi.advanceTimersByTime(399));
    expect(screen.getByRole('status')).toBeTruthy();
    act(() => vi.advanceTimersByTime(1));
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('removes the hint immediately when opening and never shows it again after a keyed remount', () => {
    vi.useFakeTimers();
    const { rerender } = render(
      <MinimapHintLifecycleHarness boundaryKey="root" eligible />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Show minimap' }));

    expect(screen.getByRole('button', { name: 'Hide minimap' })).toBeTruthy();
    expect(screen.queryByRole('status')).toBeNull();

    rerender(<MinimapHintLifecycleHarness boundaryKey="root,end" eligible />);
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('does not advertise the shortcut after the minimap was opened before hint eligibility', () => {
    vi.useFakeTimers();
    const { rerender } = render(<MinimapHintLifecycleHarness eligible={false} />);

    fireEvent.click(screen.getByRole('button', { name: 'Show minimap' }));
    fireEvent.click(screen.getByRole('button', { name: 'Hide minimap' }));
    rerender(<MinimapHintLifecycleHarness eligible />);

    expect(screen.queryByRole('status')).toBeNull();
  });

  it('pauses expiry while the dismiss action has focus and resumes after focus leaves', () => {
    vi.useFakeTimers();
    render(<MinimapHintLifecycleHarness eligible />);

    const dismissButton = screen.getByRole('button', { name: 'Dismiss minimap shortcut hint' });
    act(() => dismissButton.focus());
    act(() => vi.advanceTimersByTime(5000));

    expect(screen.getByRole('status').className).not.toContain('hintFading');

    act(() => screen.getByRole('button', { name: 'Show minimap' }).focus());
    act(() => vi.advanceTimersByTime(2999));
    expect(screen.getByRole('status').className).not.toContain('hintFading');
    act(() => vi.advanceTimersByTime(1));
    expect(screen.getByRole('status').className).toContain('hintFading');
  });

  it('returns focus to the minimap logo when the focused hint is dismissed', () => {
    render(<GraphMinimapHarness initialHintVisible />);

    const toggle = screen.getByRole('button', { name: 'Show minimap' });
    const dismissButton = screen.getByRole('button', { name: 'Dismiss minimap shortcut hint' });
    dismissButton.focus();
    fireEvent.click(dismissButton);

    expect(document.activeElement).toBe(toggle);
  });
});
