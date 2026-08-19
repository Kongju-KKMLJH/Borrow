import type { ActivityStatus, AdminUserResponse, AdminVerificationLabel, Role } from '@/lib/api/types';

/** 관리자 콘솔 표시용 라벨 (기능명세 7). 화면 3개가 같은 문구를 쓰도록 한 곳에 모은다. */

export const RoleLabel: Record<Role, string> = {
  MEMBER: '시민',
  HOST: '공간 파트너',
  ARTIST: '예술가',
  ADMIN: '관리자',
};

export const VerificationLabel: Record<AdminVerificationLabel, string> = {
  NONE: '미신청',
  PENDING: '심사 대기',
  APPROVED: '인증 완료',
  REJECTED: '거절됨',
};

/**
 * 기능명세 7.2 의 "승인대기·모집중·마감·종료" 라벨 매핑.
 *
 * <p>전수 매핑으로 짜지 않고 모르는 값은 원문 그대로 보여준다. 백엔드에는 이미 MATCHED
 * (승인 후 결제 대기)가 있는데 프론트 {@code ActivityStatus} 에는 아직 없다 — 그 타입을 여기서
 * 손대면 같은 줄을 고치는 다른 작업과 부딪히므로, 라벨 쪽만 느슨하게 두고 값이 들어오면
 * 자연히 표시되게 한다.
 */
const ACTIVITY_STATUS_LABEL: Record<string, string> = {
  DRAFT: '작성 중',
  PENDING: '승인 대기',
  MATCHED: '결제 대기',
  PUBLISHED: '모집 중',
  REJECTED: '거절됨',
};

export function activityStatusLabel(status: ActivityStatus): string {
  return ACTIVITY_STATUS_LABEL[status] ?? status;
}

/** "2026-08-19T10:07:24" → "2026.08.19". 값이 없으면 대시. */
export function formatDateOnly(iso: string | null): string {
  if (!iso) return '—';
  const [date] = iso.split('T');
  return date.replaceAll('-', '.');
}

/** 회원 목록의 한 줄 부제 — 아이디 · 가입일 (기능명세 7.1.1 display) */
export function userSubtitle(user: AdminUserResponse): string {
  return `${user.loginId} · 가입 ${formatDateOnly(user.createdAt)}`;
}
