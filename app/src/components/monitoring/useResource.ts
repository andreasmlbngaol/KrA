import { useCallback, useEffect, useRef, useState } from 'react';

type ResourceState<T> = {
  data: T | null;
  loading: boolean;
  error: string | null;
  updatedAt: Date | null;
  reload: () => void;
};

/**
 * Loads a resource once, keeps the last value while refreshing, and optionally polls.
 * Errors never clear the previous data, so a transient failure does not blank the page.
 */
export function useResource<T>(
  load: () => Promise<T>,
  deps: ReadonlyArray<unknown>,
  intervalMs = 0,
  errorMessage = 'Gagal memuat data. Pastikan server berjalan.',
): ResourceState<T> {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatedAt, setUpdatedAt] = useState<Date | null>(null);

  const loadRef = useRef(load);
  loadRef.current = load;

  const run = useCallback(
    async (showSpinner: boolean) => {
      if (showSpinner) setLoading(true);
      try {
        const value = await loadRef.current();
        setData(value);
        setError(null);
        setUpdatedAt(new Date());
      } catch {
        setError(errorMessage);
      } finally {
        setLoading(false);
      }
    },
    [errorMessage],
  );

  useEffect(() => {
    void run(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    if (!intervalMs) return;
    const id = window.setInterval(() => void run(false), intervalMs);
    return () => window.clearInterval(id);
  }, [intervalMs, run]);

  return {
    data,
    loading,
    error,
    updatedAt,
    reload: () => void run(true),
  };
}
