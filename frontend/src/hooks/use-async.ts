import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * 마운트 시 async 함수를 실행해 { data, loading, error, refetch }를 반환하는 간단한 훅.
 * opts.refetchOnFocus를 켜면 화면이 다시 포커스될 때(예: 상세에서 뒤로 돌아올 때) 자동으로 재요청한다.
 * 최초 포커스는 마운트 fetch와 겹치므로 건너뛴다.
 */
export function useAsync<T>(fn: () => Promise<T>, deps: unknown[] = [], opts?: { refetchOnFocus?: boolean }) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [tick, setTick] = useState(0);
  const refetch = useCallback(() => setTick((t) => t + 1), []);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    fn()
      .then((d) => alive && setData(d))
      .catch((e) => alive && setError(e as Error))
      .finally(() => alive && setLoading(false));
    return () => {
      alive = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  const focusReady = useRef(false);
  useFocusEffect(
    useCallback(() => {
      if (!opts?.refetchOnFocus) return;
      if (!focusReady.current) {
        focusReady.current = true;
        return;
      }
      refetch();
    }, [opts?.refetchOnFocus, refetch]),
  );

  return { data, loading, error, refetch };
}
