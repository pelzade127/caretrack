import type React from 'react';
import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from './api';

/**
 * Loads data whenever `load` changes and tracks loading / error state.
 * A response that arrives after the inputs changed is ignored, so a slow
 * request can't overwrite a newer one.
 */
export function useLoader<T>(load: () => Promise<T>) {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(undefined);
    load()
      .then((result) => {
        if (current) setData(result);
      })
      .catch((e: unknown) => {
        if (current) setError(errorMessage(e));
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => {
      current = false;
    };
  }, [load, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data, setData, error, loading, reload };
}

/**
 * On narrow screens the detail panel sits below the list, so scroll it into view
 * when a request is opened. On wide screens both are visible; do nothing.
 */
export function useScrollIntoViewOnNarrow<T extends HTMLElement>(ref: React.RefObject<T | null>, active: boolean) {
  useEffect(() => {
    if (active && window.matchMedia('(max-width: 899px)').matches) {
      ref.current?.scrollIntoView({ block: 'start' });
    }
  }, [active, ref]);
}
