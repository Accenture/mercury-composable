import { useState, useCallback, useRef } from 'react';

export type ToastType = 'info' | 'success' | 'error';

export interface Toast {
  id:      number;
  message: string;
  type:    ToastType;
}

export interface UseToastReturn {
  toasts:      Toast[];
  addToast:    (message: string, type?: ToastType) => void;
  removeToast: (id: number) => void;
}

export const useToast = (): UseToastReturn => {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(0);

  const addToast = useCallback((message: string, type: ToastType = 'info'): void => {
    const id = ++nextId.current;
    setToasts(prev => [...prev, { id, message, type }]);

    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 3000);
  }, []);

  const removeToast = useCallback((id: number): void => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return { toasts, addToast, removeToast };
};
