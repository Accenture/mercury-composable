import { useEffect, useRef } from 'react';
import { type ProtocolBus } from '../protocol/bus';

export interface UseAutoMarkdownPinOptions {
  bus:                ProtocolBus;
  connected:          boolean;
  setPinnedMessageId: (id: number) => void;
  onAutoPin?:         () => void;
}

/**
 * Subscribes to the ProtocolBus for help/describe command echoes, then
 * auto-pins the first `docs.response` event that follows.
 */
export function useAutoMarkdownPin({
  bus,
  connected,
  setPinnedMessageId,
  onAutoPin,
}: UseAutoMarkdownPinOptions): void {

  const waitingForResponseRef = useRef(false);
  const onAutoPinRef = useRef(onAutoPin);
  useEffect(() => { onAutoPinRef.current = onAutoPin; });

  // Reset pending flag on disconnect
  useEffect(() => {
    if (!connected) {
      waitingForResponseRef.current = false;
    }
  }, [connected]);

  // Arm flag on help/describe command + consume next docs.response as pin target
  useEffect(() => {
    const unsubArm = bus.on('command.helpOrDescribe', () => {
      waitingForResponseRef.current = true;
    });

    const unsubPin = bus.on('docs.response', (event) => {
      if (waitingForResponseRef.current) {
        waitingForResponseRef.current = false;
        setPinnedMessageId(event.msgId);
        onAutoPinRef.current?.();
      }
    });

    return () => { unsubArm(); unsubPin(); };
  }, [bus, setPinnedMessageId]);

  // Clear the waiting flag on non-pinnable responses to prevent flag accumulation
  useEffect(() => {
    const clearWaiting = () => {
      if (waitingForResponseRef.current) {
        waitingForResponseRef.current = false;
      }
    };

    const unsubs = [
      bus.on('json.response',     clearWaiting),
      bus.on('graph.link',        clearWaiting),
      bus.on('upload.invitation', clearWaiting),
      bus.on('lifecycle',         clearWaiting),
      bus.on('payload.large',     clearWaiting),
    ];
    return () => unsubs.forEach(fn => fn());
  }, [bus]);
}
