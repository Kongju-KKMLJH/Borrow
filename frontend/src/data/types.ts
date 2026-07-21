/**
 * Borrow 도메인 모델 — 백엔드 DTO(Borrow API v1)와 1:1로 맞춘 타입.
 * 필드명은 Jackson이 record 컴포넌트를 그대로 직렬화한 camelCase를 따른다.
 */

export type ApiResponse<T> = { success: boolean; data: T; error: { code: string; message: string } | null };

export type ActivityType = 'HOBBY' | 'CLASS'; // 취미 모임 / 전문 클래스
export type ActivityField = 'ART' | 'PHOTO'; // 그림 / 촬영
export type ActivityStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED';
export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type FacilityType = 'TABLE' | 'LIGHTING' | 'NATURAL_LIGHT' | 'WATER' | 'OUTLET' | 'WIFI';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

/** U-08 공간 요구조건. AI 분석(A-01) 결과를 그대로 실어 보낼 수도 있다. */
export interface SpaceRequirement {
  region: string;
  headcount: number;
  requiredFacilities: FacilityType[];
  noisy: boolean;
  messy: boolean;
}

/** U-01 목록 / U-13 내 활동 카드용 요약 */
export interface ActivitySummary {
  id: number;
  type: ActivityType;
  field: ActivityField;
  title: string;
  imageUrls: string[]; // 상대경로 목록 "/files/a.jpg" — 표시 시 base URL 부착
  hostNickname: string;
  hostCertified: boolean;
  date: string; // LocalDate "2026-08-01"
  startTime: string; // LocalTime "14:00" | "14:00:00"
  endTime: string;
  capacity: number;
  currentHeadcount: number;
  entryFee: number;
  status: ActivityStatus;
  alreadyJoined: boolean;
  mine: boolean;
}

/** U-03 활동 상세 */
export interface ActivityDetail extends Omit<ActivitySummary, never> {
  description?: string;
  requirement: SpaceRequirement | null;
}

export interface ActivityCreatePayload {
  hostNickname: string;
  field: ActivityField;
  title: string;
  description?: string;
  imageUrls?: string[]; // /api/uploads 에서 받은 상대경로 배열
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  entryFee: number;
  requirement?: SpaceRequirement;
}

/** U-04 참여 신청 */
export interface ParticipationPayload {
  nickname: string;
  headcount: number;
}

export interface Participation {
  id: number;
  activityId: number;
  nickname: string;
  headcount: number;
}

/** U-14 내가 참여한 활동 */
export interface MyParticipation {
  participationId: number;
  myHeadcount: number;
  activity: ActivitySummary;
}

/** U-11/U-12 개최 요청 (활동 개설자 관점) */
export interface ActivityHostingRequest {
  id: number;
  activityId: number;
  spaceId: number;
  spaceName: string;
  status: RequestStatus;
  rejectReason: string | null;
}

/** 공간 (B-02~B-06) */
export interface Space {
  id: number;
  name: string;
  region: string;
  address?: string;
  imageUrls: string[];
  capacity: number;
  hourlyFee: number;
  conditions?: string;
  facilities: FacilityType[];
  allowedFields: ActivityField[];
  noiseAllowed: boolean;
  messAllowed: boolean;
}

export interface SpacePayload {
  name: string;
  region: string;
  address?: string;
  imageUrls?: string[];
  capacity: number;
  hourlyFee: number;
  conditions?: string;
  facilities: FacilityType[];
  allowedFields: ActivityField[];
  noiseAllowed: boolean;
  messAllowed: boolean;
}

/** 공간 유휴시간 슬롯 (B-05) */
export interface SpaceSlot {
  id: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface SpaceSlotPayload {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

/** A-01 활동 분석 요청 */
export interface AnalyzePayload {
  description: string;
  region?: string;
}

/** A-01 활동 분석 결과 */
export interface AnalyzedRequirement {
  region: string;
  headcount: number;
  requiredFacilities: FacilityType[];
  noisy: boolean;
  messy: boolean;
  field: ActivityField;
}

/** A-02/A-03 공간 매칭 요청 */
export interface MatchPayload {
  region?: string;
  headcount: number;
  requiredFacilities?: FacilityType[];
  noisy?: boolean;
  messy?: boolean;
  field: ActivityField;
  date: string;
  startTime: string;
  endTime: string;
  excludeSpaceIds?: number[];
}

/** A-02/A-03 공간 매칭 결과 1건 */
export interface SpaceMatch {
  spaceId: number;
  name: string;
  region: string;
  capacity: number;
  hourlyFee: number;
  imageUrls?: string[];
  facilities: FacilityType[];
  allowedFields: ActivityField[];
  score: number;
  reason: string;
  aiScored: boolean;
}

/** 개최 요청 (공간 제공자 관점, B-07/B-08) */
export interface HostingRequestDetail {
  id: number;
  status: RequestStatus;
  rejectReason: string | null;
  space: { id: number; name: string; region: string };
  activity: {
    id: number;
    title: string;
    description?: string;
    field: ActivityField;
    date: string;
    startTime: string;
    endTime: string;
    capacity: number;
    entryFee: number;
    hostNickname: string;
    requirement: { headcount: number; requiredFacilities: FacilityType[]; noisy: boolean; messy: boolean } | null;
  };
}

/** 확정 일정 (B-11) */
export interface Schedule {
  requestId: number;
  activityId: number;
  title: string;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  spaceId: number;
  spaceName: string;
}

/** 사업자 홈 운영 현황 (B-01) */
export interface HostHome {
  pendingRequestCount: number;
  confirmedScheduleCount: number;
  spaceCount: number;
  recentPendingRequests: HostingRequestDetail[];
  upcomingSchedules: Schedule[];
}
