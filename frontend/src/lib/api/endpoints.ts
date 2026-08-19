/**
 * 도메인별 API 호출 함수.
 * 각 함수는 백엔드 컨트롤러 엔드포인트와 1:1 매핑.
 */

import { apiClient } from './client';
import type {
  SignupRequest,
  MeResponse,
  ActivityCreateRequest,
  ActivityDetailResponse,
  ActivitySummaryResponse,
  RequirementUpdateRequest,
  ParticipationRequest,
  ParticipationResponse,
  MyParticipationResponse,
  HostingRequestCreateRequest,
  ActivityHostingRequestResponse,
  ActivityType,
  ActivityField,
  SpaceRequest,
  SpaceResponse,
  SpaceSlotRequest,
  SpaceSlotResponse,
  HostHomeResponse,
  ScheduleResponse,
  HostingRequestResponse,
  RequestStatus,
  RejectRequest,
  AnalyzeRequest,
  AdminUserResponse,
  AdminVerificationResponse,
  AdminActivityResponse,
  AdminSpaceResponse,
  ArtistVerificationStatus,
  RequirementResponse,
  MatchRequest,
  SpaceMatchResponse,
  SpaceMatchResult,
  UploadResponse,
} from './types';

// ── Auth ───────────────────────────────────────────────────────────────

export const authApi = {
  /** 회원가입 (비로그인) */
  signup: (body: SignupRequest) =>
    apiClient.post<MeResponse>('/api/auth/signup', body, true),

  /** 내 정보 조회 / 로그인 확인 */
  me: () => apiClient.get<MeResponse>('/api/auth/me'),
};

// ── Activity ───────────────────────────────────────────────────────────

export const activityApi = {
  /** 활동 목록·검색 (비로그인 가능, 로그인 시 alreadyJoined/mine 채워짐) */
  list: (params?: {
    type?: ActivityType;
    field?: ActivityField;
    keyword?: string;
    region?: string;
    dateFrom?: string;
    dateTo?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.type) query.set('type', params.type);
    if (params?.field) query.set('field', params.field);
    if (params?.keyword) query.set('keyword', params.keyword);
    if (params?.region) query.set('region', params.region);
    if (params?.dateFrom) query.set('dateFrom', params.dateFrom);
    if (params?.dateTo) query.set('dateTo', params.dateTo);
    const qs = query.toString();
    return apiClient.get<ActivitySummaryResponse[]>(
      `/api/activities${qs ? `?${qs}` : ''}`
    );
  },

  /** 활동 상세 (비로그인 가능) */
  detail: (id: number) =>
    apiClient.get<ActivityDetailResponse>(`/api/activities/${id}`),

  /** 활동 개설 (MEMBER/ARTIST) */
  create: (body: ActivityCreateRequest) =>
    apiClient.post<ActivityDetailResponse>('/api/activities', body),

  /** 공간 요구조건 수정 (개설자 본인) */
  updateRequirement: (id: number, body: RequirementUpdateRequest) =>
    apiClient.patch<ActivityDetailResponse>(
      `/api/activities/${id}/requirement`,
      body
    ),

  /** 참여 신청 (로그인) */
  participate: (activityId: number, body: ParticipationRequest) =>
    apiClient.post<ParticipationResponse>(
      `/api/activities/${activityId}/participations`,
      body
    ),

  /** 참여 취소 (로그인) */
  cancelParticipation: (activityId: number) =>
    apiClient.delete<void>(`/api/activities/${activityId}/participations`),

  /** 개최 요청 전송 (MEMBER/ARTIST) */
  sendHostingRequest: (activityId: number, body: HostingRequestCreateRequest) =>
    apiClient.post<ActivityHostingRequestResponse>(
      `/api/activities/${activityId}/hosting-request`,
      body
    ),

  /** 개최 요청 상태 조회 (MEMBER/ARTIST) */
  hostingRequestStatus: (activityId: number) =>
    apiClient.get<ActivityHostingRequestResponse>(
      `/api/activities/${activityId}/hosting-request`
    ),
};

// ── Me (내 활동) ────────────────────────────────────────────────────────

export const meApi = {
  /** 내가 개설한 활동 */
  myActivities: () =>
    apiClient.get<ActivitySummaryResponse[]>('/api/me/activities'),

  /** 내가 참여한 활동 */
  myParticipations: () =>
    apiClient.get<MyParticipationResponse[]>('/api/me/participations'),
};

// ── Space ──────────────────────────────────────────────────────────────

export const spaceApi = {
  /** 공간 목록 (비로그인) */
  list: () => apiClient.get<SpaceResponse[]>('/api/spaces'),

  /** 내 공간 목록 (HOST) */
  mine: () => apiClient.get<SpaceResponse[]>('/api/spaces/mine'),

  /** 공간 상세 (비로그인) */
  detail: (id: number) => apiClient.get<SpaceResponse>(`/api/spaces/${id}`),

  /** 공간 등록 (HOST) */
  create: (body: SpaceRequest) =>
    apiClient.post<SpaceResponse>('/api/spaces', body),

  /** 공간 수정 (소유자) */
  update: (id: number, body: SpaceRequest) =>
    apiClient.put<SpaceResponse>(`/api/spaces/${id}`, body),

  /** 공간 삭제 (소유자) */
  delete: (id: number) => apiClient.delete<void>(`/api/spaces/${id}`),

  /** 유휴시간 슬롯 목록 (비로그인) */
  slots: (spaceId: number) =>
    apiClient.get<SpaceSlotResponse[]>(`/api/spaces/${spaceId}/slots`),

  /** 유휴시간 슬롯 추가 (소유자) */
  addSlot: (spaceId: number, body: SpaceSlotRequest) =>
    apiClient.post<SpaceSlotResponse>(`/api/spaces/${spaceId}/slots`, body),

  /** 유휴시간 슬롯 삭제 (소유자) */
  deleteSlot: (spaceId: number, slotId: number) =>
    apiClient.delete<void>(`/api/spaces/${spaceId}/slots/${slotId}`),
};

// ── Host (사업자) ───────────────────────────────────────────────────────

export const hostApi = {
  /** 운영 현황 대시보드 (HOST) */
  home: () => apiClient.get<HostHomeResponse>('/api/host/home'),

  /** 확정 일정 목록 (HOST) */
  schedules: () => apiClient.get<ScheduleResponse[]>('/api/host/schedules'),

  /** 받은 개최요청 목록 (HOST, 필터 가능) */
  requests: (params?: { spaceId?: number; status?: RequestStatus }) => {
    const query = new URLSearchParams();
    if (params?.spaceId) query.set('spaceId', String(params.spaceId));
    if (params?.status) query.set('status', params.status);
    const qs = query.toString();
    return apiClient.get<HostingRequestResponse[]>(
      `/api/host/requests${qs ? `?${qs}` : ''}`
    );
  },

  /** 개최요청 상세 (HOST) */
  requestDetail: (id: number) =>
    apiClient.get<HostingRequestResponse>(`/api/host/requests/${id}`),

  /** 개최요청 승인 (HOST) → 활동 자동 공개 */
  approve: (id: number) =>
    apiClient.post<HostingRequestResponse>(
      `/api/host/requests/${id}/approve`
    ),

  /** 개최요청 거절 (HOST, reason 선택) */
  reject: (id: number, body?: RejectRequest) =>
    apiClient.post<HostingRequestResponse>(
      `/api/host/requests/${id}/reject`,
      body ?? {}
    ),
};

// ── AI ─────────────────────────────────────────────────────────────────

export const aiApi = {
  /** 활동 분석 → 구조화된 공간 요구조건 (로그인) */
  analyze: (body: AnalyzeRequest) =>
    apiClient.post<RequirementResponse>('/api/ai/analyze', body),

  /** 공간 매칭 (점수 순 추천, excludeSpaceIds로 A-04 대체 추천) */
  match: (body: MatchRequest) =>
    apiClient.post<SpaceMatchResult>('/api/ai/match', body),
};

// ── Upload ─────────────────────────────────────────────────────────────

export const uploadApi = {
  /** 이미지 업로드 (multipart, 파트명 "files") → 상대 URL 배열 */
  upload: (uris: string[]): Promise<UploadResponse> => {
    const formData = new FormData();
    for (const uri of uris) {
      const name = uri.split('/').pop() ?? 'photo.jpg';
      formData.append('files', {
        uri,
        name,
        type: 'image/jpeg',
      } as unknown as Blob);
    }
    return apiClient.upload<UploadResponse>('/api/uploads', formData);
  },
};

// ── Admin (관리자 콘솔, 기능명세 7) ────────────────────────────────────

export const adminApi = {
  /** 7.1.1 전체 회원 목록 (ADMIN) — 탈퇴 회원까지 포함한 관리용 목록 */
  users: () => apiClient.get<AdminUserResponse[]>('/api/admin/users'),

  /** 7.1.3 회원 강제 탈퇴 (ADMIN) — 로그인·서비스 이용이 차단된다 */
  withdrawUser: (userId: number) =>
    apiClient.post<AdminUserResponse>(`/api/admin/users/${userId}/withdraw`),

  /** 7.1.4 예술가 인증 신청 목록 (ADMIN) — status 생략 시 심사 대기(PENDING)만 */
  verifications: (status?: ArtistVerificationStatus) => {
    const qs = status ? `?${new URLSearchParams({ status }).toString()}` : '';
    return apiClient.get<AdminVerificationResponse[]>(`/api/admin/artist-verifications${qs}`);
  },

  /** 7.1.4 예술가 인증 승인 (ADMIN) */
  approveVerification: (verificationId: number) =>
    apiClient.post<AdminVerificationResponse>(
      `/api/admin/artist-verifications/${verificationId}/approve`
    ),

  /** 7.2.1 전체 프로그램 목록 (ADMIN) — 상태·삭제 여부를 가리지 않는다 */
  activities: () => apiClient.get<AdminActivityResponse[]>('/api/admin/activities'),

  /** 7.2.3 프로그램 강제 삭제 (ADMIN) — 시민 탐색·참여 신청에서 제외된다 */
  forceDeleteActivity: (activityId: number) =>
    apiClient.post<AdminActivityResponse>(`/api/admin/activities/${activityId}/force-delete`),

  /** 7.3.1 전체 공간 목록 (ADMIN) — 강제 삭제된 공간도 포함한다 */
  spaces: () => apiClient.get<AdminSpaceResponse[]>('/api/admin/spaces'),

  /** 7.3.3 공간 강제 삭제 (ADMIN) — 진행 중인 개최 요청은 모두 자동 거절된다 */
  forceDeleteSpace: (spaceId: number) =>
    apiClient.post<AdminSpaceResponse>(`/api/admin/spaces/${spaceId}/force-delete`),
};
