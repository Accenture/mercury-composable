import { useEffect } from 'react';
import { type ProtocolBus } from '../protocol/bus';

export interface UseAutoMockUploadOptions {
  bus:         ProtocolBus;
  onOpenModal: (uploadPath: string) => void;
}

/**
 * Subscribes to `upload.invitation` events on the ProtocolBus and
 * automatically calls `onOpenModal` with the extracted POST path.
 */
export function useAutoMockUpload({
  bus,
  onOpenModal,
}: UseAutoMockUploadOptions): void {
  // Subscribe to upload.invitation events
  useEffect(() => {
    return bus.on('upload.invitation', (event) => {
      onOpenModal(event.uploadPath);
    });
  }, [bus, onOpenModal]);
}
