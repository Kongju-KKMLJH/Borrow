import type {
  ActivityCreatePayload,
  ActivityDetail,
  ActivityField,
  ActivityHostingRequest,
  ActivitySummary,
  ActivityType,
  MyParticipation,
  Participation,
  ParticipationPayload,
  SpaceRequirement,
} from '@/data/types';

import { http } from './client';

export type ActivityFilter = { type?: ActivityType; field?: ActivityField; keyword?: string };

/** GET /api/activities */
export function listActivities(filter?: ActivityFilter): Promise<ActivitySummary[]> {
  const params = new URLSearchParams();
  if (filter?.type) params.set('type', filter.type);
  if (filter?.field) params.set('field', filter.field);
  if (filter?.keyword) params.set('keyword', filter.keyword);
  const qs = params.toString();
  return http<ActivitySummary[]>(`/api/activities${qs ? `?${qs}` : ''}`);
}

/** GET /api/activities/{id} */
export function getActivity(id: number): Promise<ActivityDetail> {
  return http<ActivityDetail>(`/api/activities/${id}`);
}

/** POST /api/activities — U-06~U-08 취미 모임 개설 */
export function createActivity(payload: ActivityCreatePayload): Promise<ActivityDetail> {
  return http<ActivityDetail>('/api/activities', { method: 'POST', body: payload });
}

/** PATCH /api/activities/{id}/requirement — U-08 공간 요구조건 수정 */
export function updateRequirement(id: number, requirement: SpaceRequirement): Promise<ActivityDetail> {
  return http<ActivityDetail>(`/api/activities/${id}/requirement`, { method: 'PATCH', body: { requirement } });
}

/** POST /api/activities/{id}/hosting-request — U-11 개최 요청 전송 */
export function sendHostingRequest(activityId: number, spaceId: number): Promise<ActivityHostingRequest> {
  return http<ActivityHostingRequest>(`/api/activities/${activityId}/hosting-request`, {
    method: 'POST',
    body: { spaceId },
  });
}

/** GET /api/activities/{id}/hosting-request — U-12 개최 요청 상태 조회 */
export function getHostingRequestStatus(activityId: number): Promise<ActivityHostingRequest> {
  return http<ActivityHostingRequest>(`/api/activities/${activityId}/hosting-request`);
}

/** POST /api/activities/{id}/participations — U-04 참여 신청 */
export function participate(activityId: number, payload: ParticipationPayload): Promise<Participation> {
  return http<Participation>(`/api/activities/${activityId}/participations`, { method: 'POST', body: payload });
}

/** DELETE /api/activities/{id}/participations — U-05 참여 취소 */
export function cancelParticipation(activityId: number): Promise<void> {
  return http<void>(`/api/activities/${activityId}/participations`, { method: 'DELETE' });
}

/** GET /api/me/activities — U-13 내가 개설한 활동 */
export function getMyActivities(): Promise<ActivitySummary[]> {
  return http<ActivitySummary[]>('/api/me/activities');
}

/** GET /api/me/participations — U-14 내가 참여한 활동 */
export function getMyParticipations(): Promise<MyParticipation[]> {
  return http<MyParticipation[]>('/api/me/participations');
}
