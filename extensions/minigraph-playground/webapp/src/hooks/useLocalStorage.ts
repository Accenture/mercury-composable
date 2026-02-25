import { useState, useEffect } from 'react';

export const useLocalStorage = <T>(
  key: string,
  initialValue: T
): [T, React.Dispatch<React.SetStateAction<T>>] => {
  const [value, setValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? (JSON.parse(item) as T) : initialValue;
    } catch (error) {
      console.error(`Error reading localStorage key "${key}":`, error);
      return initialValue;
    }
  });

  // When the key changes (e.g. Playground unmounts and remounts with a different
  // storageKey), re-read from localStorage so the new key's stored value is used
  // rather than stale state from the previous key.
  useEffect(() => {
    try {
      const item = window.localStorage.getItem(key);
      setValue(item ? (JSON.parse(item) as T) : initialValue);
    } catch (error) {
      console.error(`Error reading localStorage key "${key}":`, error);
      setValue(initialValue);
    }
    // initialValue is intentionally excluded: we only want to react to key changes,
    // not to referentially-unstable default values passed by callers.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  // Persist to localStorage whenever the value changes.
  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.error(`Error setting localStorage key "${key}":`, error);
    }
  }, [key, value]);

  return [value, setValue];
};
