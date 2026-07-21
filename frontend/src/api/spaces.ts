import type { Space, SpacePayload, SpaceSlot, SpaceSlotPayload } from '@/data/types';

import { http } from './client';

/** GET /api/spaces — B-03 공간 목록 */
export function listSpaces(): Promise<Space[]> {
  return http<Space[]>('/api/spaces');
}

/** GET /api/spaces/{id} — B-04 공간 상세 */
export function getSpace(id: number): Promise<Space> {
  return http<Space>(`/api/spaces/${id}`);
}

/** POST /api/spaces — B-02 공간 등록 */
export function createSpace(payload: SpacePayload): Promise<Space> {
  return http<Space>('/api/spaces', { method: 'POST', body: payload });
}

/** PUT /api/spaces/{id} — B-05 공간 수정 */
export function updateSpace(id: number, payload: SpacePayload): Promise<Space> {
  return http<Space>(`/api/spaces/${id}`, { method: 'PUT', body: payload });
}

/** DELETE /api/spaces/{id} — B-06 공간 삭제 */
export function deleteSpace(id: number): Promise<void> {
  return http<void>(`/api/spaces/${id}`, { method: 'DELETE' });
}

/** GET /api/spaces/{spaceId}/slots — 유휴시간 목록 */
export function listSlots(spaceId: number): Promise<SpaceSlot[]> {
  return http<SpaceSlot[]>(`/api/spaces/${spaceId}/slots`);
}

/** POST /api/spaces/{spaceId}/slots — 유휴시간 추가 */
export function addSlot(spaceId: number, payload: SpaceSlotPayload): Promise<SpaceSlot> {
  return http<SpaceSlot>(`/api/spaces/${spaceId}/slots`, { method: 'POST', body: payload });
}

/** DELETE /api/spaces/{spaceId}/slots/{slotId} — 유휴시간 삭제 */
export function deleteSlot(spaceId: number, slotId: number): Promise<void> {
  return http<void>(`/api/spaces/${spaceId}/slots/${slotId}`, { method: 'DELETE' });
}
