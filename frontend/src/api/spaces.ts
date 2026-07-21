import { spaceMatches, spaces } from '@/data/mock';
import type { Space, SpaceMatch } from '@/data/types';

import { simulate } from './client';

export type SpaceConditions = {
  region?: string;
  headcount?: number;
  facilities?: string[];
  notes?: string;
};

/** POST /spaces/recommend — AI 공간 추천 (만들기 Step5) */
export function recommendSpaces(conditions: SpaceConditions): Promise<SpaceMatch[]> {
  return simulate(spaceMatches, 600);
}

/** GET /me/space — 공간 제공자 본인 공간 */
export function getMySpace(): Promise<Space> {
  return simulate(spaces[0]);
}

/** PUT /me/space — 공간 정보 저장 */
export function saveSpace(payload: Partial<Space>): Promise<Space> {
  return simulate({ ...spaces[0], ...payload });
}
