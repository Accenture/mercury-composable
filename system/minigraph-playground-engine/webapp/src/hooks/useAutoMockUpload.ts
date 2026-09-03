import { useEffect } from 'react';
import { type ProtocolBus } from '../protocol/bus';

export interface UseAutoMockUploadOptions {
  bus:         ProtocolBus;
  onOpenModal: (uploadPath: string) => void;
  /** Synchronous ownership check so batched open→close events do not leave a stale guard. */
  isModalOpen: () => boolean;
}

/**
 * Subscribes to `upload.invitation` events on the ProtocolBus and
 * automatically calls `onOpenModal` with the extracted POST path.
 */
export function useAutoMockUpload({
  bus,
  onOpenModal,
  isModalOpen,
}: UseAutoMockUploadOptions): void {
  // Subscribe to upload.invitation events
  useEffect(() => {
    return bus.on('upload.invitation', (event) => {
      if (isModalOpen()) return;
      onOpenModal(event.uploadPath);
    });
  }, [bus, isModalOpen, onOpenModal]);
}
