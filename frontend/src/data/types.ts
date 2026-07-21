/**
 * Borrow 도메인 모델. 서버 스키마 확정 시 이 파일을 기준으로 맞춘다.
 */

export type ActivityType = 'hobby' | 'class'; // 취미 모임 / 전문 클래스
export type FieldType = 'drawing' | 'photo'; // 그림 / 촬영
export type Difficulty = 'beginner' | 'experienced' | 'any';

export type ActivityStatus =
  | 'draft' // 작성 중
  | 'pending' // 공간 승인 대기
  | 'recruiting' // 모집 중
  | 'closed' // 모집 마감
  | 'ended' // 종료
  | 'cancelled' // 취소
  | 'rejected'; // 거절

export type RequestStatus = 'pending' | 'approved' | 'rejected'; // 승인 대기 / 승인 / 거절
export type JoinStatus = 'available' | 'applied' | 'cancelled' | 'unavailable';

export interface Host {
  id: string;
  name: string;
  avatar?: string;
  verifiedArtist: boolean;
  bio?: string;
}

export interface Space {
  id: string;
  name: string;
  address: string;
  district: string;
  capacity: number;
  facilities: string[];
  pricePerHour: number;
  notes?: string;
}

export interface Activity {
  id: string;
  type: ActivityType;
  field: FieldType;
  title: string;
  intro: string;
  description?: string;
  host: Host;
  date: string; // 표시용 문자열 (예: '10월 24일 (토)')
  time: string; // 예: '오후 2:00 ~ 4:00'
  deadline: string;
  capacity: number;
  joined: number;
  fee: number;
  difficulty: Difficulty;
  status: ActivityStatus;
  space?: Space;
  preparation?: string[];
  region: string;
  joinStatus?: JoinStatus;
}

/** AI 공간 추천 결과 */
export interface SpaceMatch {
  space: Space;
  score: number; // 적합도 %
  reason: string;
  matched: string[]; // 일치 조건
  cautions: string[]; // 주의 조건
}

/** 사업자에게 전달되는 개최 요청 */
export interface HostingRequest {
  id: string;
  activity: Activity;
  requester: Host;
  requestedTime: string;
  spaceUseTime: string;
  headcount: number;
  matchScore: number;
  matched: string[];
  cautions: string[];
  status: RequestStatus;
  rejectReason?: string;
}

export interface User {
  id: string;
  name: string;
  avatar?: string;
  businessName?: string; // 공간 제공자일 때 사업장명
}

// ── 라벨 매핑 (배지/칩 표시용) ─────────────────────────────
export const ActivityTypeLabel: Record<ActivityType, string> = { hobby: '취미 모임', class: '전문 클래스' };
export const FieldLabel: Record<FieldType, string> = { drawing: '그림', photo: '촬영' };
export const DifficultyLabel: Record<Difficulty, string> = { beginner: '초보 환영', experienced: '경험자', any: '제한 없음' };
export const ActivityStatusLabel: Record<ActivityStatus, string> = {
  draft: '작성 중', pending: '공간 승인 대기', recruiting: '모집 중', closed: '모집 마감', ended: '종료', cancelled: '취소', rejected: '거절',
};
export const RequestStatusLabel: Record<RequestStatus, string> = { pending: '승인 대기', approved: '승인', rejected: '거절' };
