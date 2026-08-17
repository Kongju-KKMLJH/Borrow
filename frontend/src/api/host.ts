import type { HostHome, HostingRequestDetail, RequestStatus, Schedule } from '@/data/types';

import { http } from './client';

/** GET /api/host/home — B-01 운영 현황 */
export function getHome(): Promise<HostHome> {
  return http<HostHome>('/api/host/home');
}

/** GET /api/host/schedules — B-11 확정 일정 */
export function getSchedules(): Promise<Schedule[]> {
  return http<Schedule[]>('/api/host/schedules');
}

/** GET /api/host/requests — B-07 받은 개최요청 목록 */
export function listRequests(params?: { spaceId?: number; status?: RequestStatus }): Promise<HostingRequestDetail[]> {
  const qs = new URLSearchParams();
  if (params?.spaceId) qs.set('spaceId', String(params.spaceId));
  if (params?.status) qs.set('status', params.status);
  const suffix = qs.toString();
  return http<HostingRequestDetail[]>(`/api/host/requests${suffix ? `?${suffix}` : ''}`);
}

/** GET /api/host/requests/{id} — B-08 개최요청 상세 */
export function getRequest(id: number): Promise<HostingRequestDetail> {
  return http<HostingRequestDetail>(`/api/host/requests/${id}`);
}

/** POST /api/host/requests/{id}/approve — B-09 (승인 시 활동이 자동 공개된다) */
export function approveRequest(id: number): Promise<HostingRequestDetail> {
  return http<HostingRequestDetail>(`/api/host/requests/${id}/approve`, { method: 'POST' });
}

/** POST /api/host/requests/{id}/reject — B-10 */
export function rejectRequest(id: number, reason?: string): Promise<HostingRequestDetail> {
  return http<HostingRequestDetail>(`/api/host/requests/${id}/reject`, {
    method: 'POST',
    body: reason ? { reason } : undefined,
  });
}
