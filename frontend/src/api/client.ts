/**
 * API 클라이언트 베이스.
 * 백엔드 공통 규약: 응답은 { success, data, error } 로 감싸지고, 인증은 X-Guest-Id 헤더(UUID)로 한다.
 */
import Constants from 'expo-constants';

import { getGuestId } from '@/lib/guest';
import type { ApiResponse } from '@/data/types';

export const API_BASE_URL: string =
  (Constants.expoConfig?.extra?.apiBaseUrl as string) ??
  process.env.EXPO_PUBLIC_API_BASE_URL ??
  'http://localhost:8080';

/** 서버가 주는 상대 이미지 경로("/files/a.jpg")에 base URL을 붙인다. 이미 절대 URL이면 그대로 둔다. */
export function imageUri(path?: string | null): string | undefined {
  if (!path) return undefined;
  return /^https?:\/\//.test(path) ? path : `${API_BASE_URL}${path}`;
}

export class ApiError extends Error {
  code: string;
  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

type HttpOptions = { method?: string; body?: unknown };

export async function http<T>(path: string, opts: HttpOptions = {}): Promise<T> {
  const guestId = await getGuestId();
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: opts.method ?? 'GET',
    headers: { 'Content-Type': 'application/json', 'X-Guest-Id': guestId },
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  });

  if (res.status === 204) return undefined as T;

  const json = (await res.json()) as ApiResponse<T>;
  if (!json.success) {
    throw new ApiError(json.error?.code ?? 'UNKNOWN', json.error?.message ?? `API ${res.status}: ${path}`);
  }
  return json.data;
}
