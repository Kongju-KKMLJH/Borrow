/**
 * 백엔드 DTO와 1:1 매핑되는 TypeScript 타입 정의.
 * 모든 타입은 backend/src/main/java/kkmljh/borrow/{domain,dto}/*.java 의 record 기반.
 */

// ── Enums ──────────────────────────────────────────────────────────────

/** ADMIN 은 가입 화면에 노출하지 않는다 — 백엔드가 signup 으로는 만들지 못하게 막는다 (기능명세 7). */
export type Role = 'MEMBER' | 'HOST' | 'ARTIST' | 'ADMIN';

export type ActivityType = 'HOBBY' | 'CLASS';

export type ActivityField = 'ART' | 'PHOTO';

export type ActivityStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED';

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

export interface ActivityHostingRequestResponse {
  id: number;
  activityId: number;
  spaceId: number;
  spaceName: string;
  status: RequestStatus;
  rejectReason: string | null;
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
export type ArtistVerificationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type AdminVerificationLabel = ArtistVerificationStatus | 'NONE';

/** 7.1.1 관리자 회원 목록의 한 줄 */
export interface AdminUserResponse {
  id: number;
  loginId: string;
  nickname: string;
  role: Role;
  createdAt: string | null;
  verificationStatus: AdminVerificationLabel;
  mock: boolean;
  withdrawn: boolean;
  withdrawnAt: string | null;
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
  mock: boolean;
  forceDeleted: boolean;
  forceDeletedAt: string | null;
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
  mock: boolean;
  forceDeleted: boolean;
  forceDeletedAt: string | null;
}
