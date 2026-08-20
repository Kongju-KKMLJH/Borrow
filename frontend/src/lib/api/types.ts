/**
 * 백엔드 DTO와 1:1 매핑되는 TypeScript 타입 정의.
 * 모든 타입은 backend/src/main/java/kkmljh/borrow/{domain,dto}/*.java 의 record 기반.
 */

// ── Enums ──────────────────────────────────────────────────────────────

/** ADMIN 은 가입 화면에 노출하지 않는다 — 백엔드가 signup 으로는 만들지 못하게 막는다 (기능명세 7). */
export type Role = 'MEMBER' | 'HOST' | 'ARTIST' | 'ADMIN';

export type ActivityType = 'HOBBY' | 'CLASS';

export type ActivityField = 'ART' | 'PHOTO';

export type ActivityStatus = 'DRAFT' | 'PENDING' | 'MATCHED' | 'PUBLISHED' | 'REJECTED';

export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type FacilityType =
  | 'TABLE'
  | 'LIGHTING'
  | 'NATURAL_LIGHT'
  | 'WATER'
  | 'OUTLET'
  | 'WIFI';

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

// ── 공통 응답 래퍼 ─────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
}

export interface ApiError {
  code: string;
  message: string;
}

// ── Auth ───────────────────────────────────────────────────────────────

export interface SignupRequest {
  loginId: string;
  password: string;
  nickname: string;
  role: Role;
}

export interface MeResponse {
  loginId: string;
  nickname: string;
  role: Role;
}

export type ArtistVerificationStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ArtistVerificationRequest {
  portfolioUrl: string;
  career: string | null;
}

export interface ArtistVerificationResponse {
  status: ArtistVerificationStatus;
  portfolioUrl: string | null;
  career: string | null;
  reason: string | null;
  appliedAt: string | null;
  updatedAt: string | null;
}

// ── Activity ───────────────────────────────────────────────────────────

export interface SpaceRequirementDto {
  region: string | null;
  headcount: number | null;
    requiredFacilities: FacilityType[] | null;
  noisy: boolean | null;
  messy: boolean | null;
}

export interface ActivityCreateRequest {
  field: ActivityField;
  title: string;
  description: string | null;
  imageUrls: string[] | null;
  date: string; // ISO LocalDate: "2026-10-24"
  startTime: string; // LocalTime: "14:00"
  endTime: string; // LocalTime: "16:00"
  capacity: number;
  entryFee: number;
  requirement: SpaceRequirementDto | null;
}

export interface ActivityUpdateRequest {
  field: ActivityField;
  title: string;
  description: string | null;
  imageUrls: string[] | null;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  entryFee: number;
}

export interface ActivityDetailResponse {
  id: number;
  type: ActivityType;
  field: ActivityField;
  title: string;
  description: string | null;
  imageUrls: string[];
  hostNickname: string | null;
  hostCertified: boolean;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  currentHeadcount: number;
  remainingCapacity: number;
  space: ActivitySpaceInfo | null;
  entryFee: number;
  status: ActivityStatus;
  requirement: SpaceRequirementDto | null;
  alreadyJoined: boolean;
  mine: boolean;
}

export interface ActivitySummaryResponse {
  id: number;
  type: ActivityType;
  field: ActivityField;
  title: string;
  imageUrls: string[];
  hostNickname: string | null;
  hostCertified: boolean;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  currentHeadcount: number;
  remainingCapacity: number;
  space: ActivitySpaceInfo | null;
  entryFee: number;
  status: ActivityStatus;
  alreadyJoined: boolean;
  mine: boolean;
}

/** 활동 응답에 실리는 확정 공간 요약 (개최 요청이 APPROVED일 때만, address 없음) */
export interface ActivitySpaceInfo {
  id: number;
  name: string;
  region: string;
}

export interface RequirementUpdateRequest {
  requirement: SpaceRequirementDto;
}

export interface ParticipationRequest {
  headcount: number;
}

export interface ParticipationResponse {
  id: number;
  activityId: number;
  nickname: string;
  headcount: number;
}

export interface MyParticipationResponse {
  participationId: number;
  myHeadcount: number;
  activity: ActivitySummaryResponse;
}

export interface HostingRequestCreateRequest {
  spaceId: number;
}

/**
 * 개최 요청 가격 구성 (U-11/U-12 응답의 price 객체).
 * 백엔드 activity.dto.HostingRequestResponse.PriceBreakdown 매핑.
 * 매칭 이용료(platformMatchingFee)는 서버 설정값이라 하드코딩 금지 — 응답값 그대로 표시.
 */
export interface PriceBreakdown {
  participantPrice: number;
  expectedParticipantRevenue: number;
  spaceRentalFee: number;
  platformMatchingFee: number;
  expectedOperatingProfit: number;
}

export interface ActivityHostingRequestResponse {
  id: number;
  activityId: number;
  spaceId: number;
  spaceName: string;
  status: RequestStatus;
  rejectReason: string | null;
  price: PriceBreakdown;
}

// ── Space ──────────────────────────────────────────────────────────────

export interface SpaceRequest {
  name: string;
  region: string;
  address: string | null;
  imageUrls: string[] | null;
  capacity: number;
  hourlyFee: number;
  conditions: string | null;
  facilities: FacilityType[] | null;
  allowedFields: ActivityField[] | null;
  noiseAllowed: boolean;
  messAllowed: boolean;
}

export interface SpaceResponse {
  id: number;
  name: string;
  region: string;
  address: string | null;
  imageUrls: string[];
  capacity: number;
  hourlyFee: number;
  conditions: string | null;
  facilities: FacilityType[];
  allowedFields: ActivityField[];
  noiseAllowed: boolean;
  messAllowed: boolean;
}

export interface SpaceSlotRequest {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface SpaceSlotResponse {
  id: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

// ── Host (사업자용 응답) ───────────────────────────────────────────────

export interface HostingRequestSpaceInfo {
  id: number;
  name: string;
  region: string;
}

export interface HostingRequestActivityInfo {
  id: number;
  title: string;
  description: string | null;
  field: ActivityField;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  entryFee: number;
  hostNickname: string | null;
  requirement: {
    headcount: number | null;
  requiredFacilities: FacilityType[] | null;
    noisy: boolean | null;
    messy: boolean | null;
  } | null;
}

export interface HostingRequestResponse {
  id: number;
  status: RequestStatus;
  rejectReason: string | null;
  space: HostingRequestSpaceInfo;
  activity: HostingRequestActivityInfo;
  /** 활동 일정이 공간 유휴시간 슬롯에 완전히 들어맞지 않으면 true — 승인·거절을 막지는 않는 판단 보조 정보 */
  scheduleMismatch: boolean;
}

export interface HostHomeResponse {
  pendingRequestCount: number;
  confirmedScheduleCount: number;
  spaceCount: number;
  recentPendingRequests: HostingRequestResponse[];
  upcomingSchedules: ScheduleResponse[];
}

export interface ScheduleResponse {
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

export interface RejectRequest {
  reason: string | null;
}

// ── AI ─────────────────────────────────────────────────────────────────

export interface AnalyzeRequest {
  description: string;
  region: string | null;
}

export interface RequirementResponse {
  region: string | null;
  headcount: number;
  requiredFacilities: FacilityType[];
  noisy: boolean;
  messy: boolean;
  field: ActivityField;
  missingFields: string[];
  followUpQuestions: string[];
}

export interface MatchRequest {
  region: string | null;
  headcount: number;
  requiredFacilities: FacilityType[] | null;
  noisy: boolean | null;
  messy: boolean | null;
  field: ActivityField;
  date: string;
  startTime: string;
  endTime: string;
  excludeSpaceIds: number[] | null;
}

export interface SpaceMatchResponse {
  spaceId: number;
  name: string;
  region: string;
  capacity: number;
  hourlyFee: number;
  imageUrls: string[];
  facilities: FacilityType[];
  allowedFields: ActivityField[];
  score: number;
  reason: string;
  cautions: string[];
  aiScored: boolean;
}

export interface SpaceMatchResult {
  matched: SpaceMatchResponse[];
  suggestions: string[];
}

// ── Upload ─────────────────────────────────────────────────────────────

export interface UploadResponse {
  urls: string[];
}

// ── Admin (관리자 콘솔, 기능명세 7) ────────────────────────────────────

/** 예술가 인증 신청 상태. 신청 행이 없는 회원은 'NONE' 으로 내려온다. */
export type AdminVerificationLabel = ArtistVerificationStatus;

/** 7.1.1 관리자 회원 목록의 한 줄 */
export interface AdminUserResponse {
  id: number;
  loginId: string;
  nickname: string;
  role: Role;
  createdAt: string | null;
  verificationStatus: AdminVerificationLabel;
}

/** 7.1.4 예술가 인증 신청 */
export interface AdminVerificationResponse {
  id: number;
  loginId: string;
  nickname: string | null;
  portfolioUrl: string;
  career: string | null;
  status: ArtistVerificationStatus;
  createdAt: string;
  updatedAt: string;
}

/** 7.2.1 관리자 프로그램 목록의 한 줄 */
export interface AdminActivityResponse {
  id: number;
  title: string;
  type: ActivityType;
  hostLoginId: string;
  hostNickname: string | null;
  spaceId: number | null;
  spaceName: string | null;
  spaceRegion: string | null;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  status: ActivityStatus;
  /** 아래 4개는 수정 폼 프리필용 — 없으면 저장할 때 원본이 지워진다 (기능명세 7.2.2) */
  field: ActivityField;
  description: string | null;
  entryFee: number;
  imageUrls: string[];
}

/** 7.3.1 관리자 공간 목록의 한 줄. 관리 목적이므로 주소 전문을 포함한다. */
export interface AdminSpaceResponse {
  id: number;
  name: string;
  ownerId: string;
  region: string;
  address: string | null;
  capacity: number;
  hourlyFee: number;
  createdAt: string | null;
  /** 아래 7개는 수정 폼 프리필용 — 없으면 저장할 때 원본이 지워진다 (기능명세 7.3.2).
   *  특히 slots 가 비면 이용 시간이 폼 기본값으로 덮여 AI 추천에서 빠진다. */
  imageUrls: string[];
  conditions: string | null;
  facilities: FacilityType[];
  allowedFields: ActivityField[];
  noiseAllowed: boolean;
  messAllowed: boolean;
  slots: SpaceSlotResponse[];
}

// ── Admin 데이터 생성·수정 요청 (기능명세 7.1.2 · 7.2.2 · 7.3.2) ────────

/** 회원 생성·수정. 수정 시 password 를 비우면 기존 값을 유지한다. */
export interface AdminUserRequest {
  loginId: string;
  password?: string;
  nickname: string;
  role: Role;
  /** 예술가일 때만 지정할 수 있다. 비우면 "신청한 적 없음". */
  verificationStatus?: ArtistVerificationStatus | null;
}

/** 프로그램 생성·수정. 유형·인증 배지는 담당 예술가 계정의 역할에서 서버가 정한다. */
export interface AdminActivityRequest {
  hostLoginId: string;
  /** 개최지. null 이면 공간 미확정. MATCHED·PUBLISHED 로 만들려면 필수. */
  spaceId: number | null;
  field: ActivityField;
  title: string;
  description?: string;
  imageUrls?: string[];
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  entryFee: number;
  status: ActivityStatus;
}

/** 공간 생성·수정. slots 는 전체 교체된다. */
export interface AdminSpaceRequest {
  ownerId: string;
  name: string;
  region: string;
  address?: string;
  imageUrls?: string[];
  capacity: number;
  hourlyFee: number;
  conditions?: string;
  facilities?: FacilityType[];
  allowedFields?: ActivityField[];
  noiseAllowed: boolean;
  messAllowed: boolean;
  slots?: SpaceSlotRequest[];
}
