import type { AnalyzePayload, AnalyzedRequirement, MatchPayload, SpaceMatch } from '@/data/types';

import { http } from './client';

/** POST /api/ai/analyze — A-01 활동 설명 자유 텍스트 → 구조화된 공간 요구조건 */
export function analyze(payload: AnalyzePayload): Promise<AnalyzedRequirement> {
  return http<AnalyzedRequirement>('/api/ai/analyze', { method: 'POST', body: payload });
}

/** POST /api/ai/match — A-02/A-03 공간 매칭. excludeSpaceIds를 채우면 A-04 대체 추천 */
export function match(payload: MatchPayload): Promise<SpaceMatch[]> {
  return http<SpaceMatch[]>('/api/ai/match', { method: 'POST', body: payload });
}
