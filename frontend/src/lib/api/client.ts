/**
 * 백엔드 API fetch 클라이언트 래퍼.
 *
 * - BASE_URL: LAN PC 서버 주소 (환경 변수 또는 기본값)
 * - HTTP Basic 인증 헤더 자동 부착
 * - ApiResponse<T> 래퍼 언래핑 + 에러 정규화
 * - JSON 직렬화 시 Set → Array 변환 (Java record가 Set을 받지만 JSON은 배열)
 */

import type { ApiResponse, ApiError } from './types';

// LAN PC 서버 주소. 실기기: PC LAN IP, 에뮬레이터: 10.0.2.2 (Android) / localhost (iOS 시뮬레이터)
export const BASE_URL =
  process.env.EXPO_PUBLIC_API_URL ??
  'http://localhost:8080';

/** 상대 경로(/files/xxx.jpg)를 전체 URL로 변환 */
export function imageUri(path: string | null | undefined): string | undefined {
  if (!path) return undefined;
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  return `${BASE_URL}${path}`;
}

// ── 인증 자격증명 (메모리 캐시) ─────────────────────────────────────────

let cachedCredentials: { loginId: string; password: string } | null = null;

export function setCredentials(loginId: string, password: string): void {
  cachedCredentials = { loginId, password };
}

export function clearCredentials(): void {
  cachedCredentials = null;
}

export function getCredentials(): { loginId: string; password: string } | null {
  return cachedCredentials;
}

export function getAuthHeader(): string | undefined {
  if (!cachedCredentials) return undefined;
  const { loginId, password } = cachedCredentials;
  return `Basic ${btoa(`${loginId}:${password}`)}`;
}

// ── Set ↔ Array 변환 (JSON 직렬화) ──────────────────────────────────────

function normalizeBody(body: unknown): unknown {
  if (body === null || body === undefined) return body;
  if (typeof body !== 'object') return body;
  if (body instanceof FormData) return body;
  const obj = body as Record<string, unknown>;
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value instanceof Set) {
      result[key] = Array.from(value);
    } else if (Array.isArray(value)) {
      result[key] = value.map((v) =>
        v instanceof Set ? Array.from(v) : v
      );
    } else if (typeof value === 'object' && value !== null) {
      result[key] = normalizeBody(value);
    } else {
      result[key] = value;
    }
  }
  return result;
}

// ── 핵심 fetch 래퍼 ─────────────────────────────────────────────────────

export class ApiException extends Error {
  constructor(
    public readonly error: ApiError,
    public readonly status: number
  ) {
    super(error.message);
    this.name = 'ApiException';
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  /** 인증 헤더 생략 (회원가입 등 비로그인 API) */
  noAuth?: boolean;
  /** multipart/form-data 등 직접 body 전달 시 Content-Type 생략 */
  rawBody?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, noAuth = false, rawBody = false } = options;

  const headers: Record<string, string> = {};

  if (!noAuth) {
    const auth = getAuthHeader();
    if (auth) headers['Authorization'] = auth;
  }

  let fetchBody: BodyInit | undefined;
  if (body !== undefined && body !== null) {
    if (rawBody) {
      fetchBody = body as BodyInit;
    } else {
      headers['Content-Type'] = 'application/json';
      fetchBody = JSON.stringify(normalizeBody(body));
    }
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: fetchBody,
  });

  // 빈 응답 처리 (DELETE 등)
  const text = await res.text();
  if (!text) {
    if (!res.ok) {
      throw new ApiException(
        { code: 'HTTP_ERROR', message: `HTTP ${res.status}` },
        res.status
      );
    }
    return undefined as T;
  }

  let json: ApiResponse<T>;
  try {
    json = JSON.parse(text) as ApiResponse<T>;
  } catch {
    // JSON이 아니면 HTTP 에러
    throw new ApiException(
      { code: 'HTTP_ERROR', message: text || `HTTP ${res.status}` },
      res.status
    );
  }

  if (!json.success) {
    const err = json.error ?? { code: 'UNKNOWN', message: '알 수 없는 오류' };
    throw new ApiException(err, res.status);
  }

  return json.data;
}

export const apiClient = {
  get: <T>(path: string, noAuth = false) =>
    request<T>(path, { method: 'GET', noAuth }),

  post: <T>(path: string, body?: unknown, noAuth = false) =>
    request<T>(path, { method: 'POST', body, noAuth }),

  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body }),

  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body }),

  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),

  /** multipart/form-data 업로드용 */
  upload: <T>(path: string, formData: FormData) =>
    request<T>(path, { method: 'POST', body: formData, rawBody: true }),
};
