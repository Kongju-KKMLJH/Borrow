/**
 * API 클라이언트 베이스.
 * 현재는 목업을 반환하는 `simulate()`를 쓰고, 서버 준비 시 `http`로 교체한다.
 */
import Constants from 'expo-constants';

export const API_BASE_URL: string =
  (Constants.expoConfig?.extra?.apiBaseUrl as string) ??
  process.env.EXPO_PUBLIC_API_BASE_URL ??
  'http://localhost:8080';

/** 목업 지연 시뮬레이션 — 실제 네트워크 대체. */
export function simulate<T>(data: T, ms = 250): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(data), ms));
}

type HttpOptions = { method?: string; body?: unknown; headers?: Record<string, string> };

/**
 * 실제 서버 연동용 fetch 래퍼. 엔드포인트 확정 후 서비스에서 simulate() 대신 사용.
 * 예) return http<Activity[]>('/activities');
 */
export async function http<T>(path: string, opts: HttpOptions = {}): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: opts.method ?? 'GET',
    headers: { 'Content-Type': 'application/json', ...opts.headers },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`);
  return res.json() as Promise<T>;
}
