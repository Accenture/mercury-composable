// @vitest-environment happy-dom

import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { useMockUploadPanel } from '../useMockUploadPanel';

describe('useMockUploadPanel', () => {
  it('opens only on an explicit request and closes by exact path', () => {
    const { result } = renderHook(() => useMockUploadPanel({ addToast: vi.fn() }));

    expect(result.current.uploadPanelPath).toBeNull();
    act(() => result.current.handleOpenUploadPanel('/api/mock/ws-1-1'));
    expect(result.current.uploadPanelPath).toBe('/api/mock/ws-1-1');

    act(() => expect(result.current.handleCloseUploadPath('/api/mock/other')).toBe(false));
    expect(result.current.uploadPanelPath).toBe('/api/mock/ws-1-1');

    act(() => expect(result.current.handleCloseUploadPath('/api/mock/ws-1-1')).toBe(true));
    expect(result.current.uploadPanelPath).toBeNull();
  });

  it('queues a second request behind the open panel and advances on close', () => {
    const { result } = renderHook(() => useMockUploadPanel({ addToast: vi.fn() }));

    act(() => result.current.handleOpenUploadPanel('/api/mock/first'));
    act(() => result.current.handleOpenUploadPanel('/api/mock/second'));
    expect(result.current.uploadPanelPath).toBe('/api/mock/first');

    act(() => result.current.handleCloseUploadPanel());
    expect(result.current.uploadPanelPath).toBe('/api/mock/second');
    act(() => result.current.handleCloseUploadPanel());
    expect(result.current.uploadPanelPath).toBeNull();
  });

  it('can drop a queued request without closing the current panel', () => {
    const { result } = renderHook(() => useMockUploadPanel({ addToast: vi.fn() }));

    act(() => result.current.handleOpenUploadPanel('/api/mock/first'));
    act(() => result.current.handleOpenUploadPanel('/api/mock/second'));
    act(() => expect(result.current.handleCloseUploadPath('/api/mock/second')).toBe(true));

    expect(result.current.uploadPanelPath).toBe('/api/mock/first');
    act(() => result.current.handleCloseUploadPanel());
    expect(result.current.uploadPanelPath).toBeNull();
  });

  it('records a successful upload for the open path, closes the panel and toasts', () => {
    const addToast = vi.fn();
    const { result } = renderHook(() => useMockUploadPanel({ addToast }));

    act(() => result.current.handleOpenUploadPanel('/api/mock/ws-1-1'));
    act(() => result.current.handleUploadSuccess('{"message":"Content uploaded"}'));

    expect(result.current.uploadPanelPath).toBeNull();
    expect(result.current.successfulUploadPaths.has('/api/mock/ws-1-1')).toBe(true);
    expect(addToast).toHaveBeenCalledWith('Mock data uploaded successfully ✓', 'success');

    act(() => result.current.resetSuccessfulPaths());
    expect(result.current.successfulUploadPaths.size).toBe(0);
  });
});
