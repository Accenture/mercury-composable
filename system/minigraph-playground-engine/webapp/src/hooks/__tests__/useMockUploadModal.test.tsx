// @vitest-environment happy-dom

import { act, renderHook } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ProtocolBus } from '../../protocol/bus';
import { useMockUploadModal } from '../useMockUploadModal';

describe('useMockUploadModal path ownership', () => {
  it('closes an invalidated workflow path without closing a different manual modal', () => {
    const { result } = renderHook(() => useMockUploadModal({
      bus: new ProtocolBus(),
      addToast: vi.fn(),
    }));

    act(() => result.current.handleOpenUploadModal('/api/mock/manual'));
    act(() => expect(result.current.handleCloseUploadPath('/api/mock/workflow')).toBe(false));
    expect(result.current.modalUploadPath).toBe('/api/mock/manual');

    act(() => expect(result.current.handleCloseUploadPath('/api/mock/manual')).toBe(true));
    expect(result.current.modalUploadPath).toBeNull();
  });

  it('can open a later invitation after an invalidated invitation is opened and closed in one event', () => {
    const bus = new ProtocolBus();
    const { result } = renderHook(() => useMockUploadModal({
      bus,
      addToast: vi.fn(),
    }));
    const offInvalidate = bus.on('upload.invitation', event => {
      result.current.handleCloseUploadPath(event.uploadPath);
    });

    act(() => bus.emit({
      kind: 'upload.invitation',
      msgId: 1,
      raw: 'You may upload JSON payload -> POST /api/mock/stale',
      uploadPath: '/api/mock/stale',
    }));
    expect(result.current.modalUploadPath).toBeNull();

    offInvalidate();
    act(() => bus.emit({
      kind: 'upload.invitation',
      msgId: 2,
      raw: 'You may upload JSON payload -> POST /api/mock/current',
      uploadPath: '/api/mock/current',
    }));
    expect(result.current.modalUploadPath).toBe('/api/mock/current');
  });
});
