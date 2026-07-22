import type { ActivityField, ActivityStatus, ActivityType, DayOfWeek, FacilityType, RequestStatus } from '@/data/types';

/** 백엔드가 내려주는 raw 값(LocalDate "2026-08-01", LocalTime "14:00" 또는 "14:00:00")을 한글 표시 문자열로 변환. */

const WEEKDAY_KO = ['일', '월', '화', '수', '목', '금', '토'];

export function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return `${m}월 ${d}일 (${WEEKDAY_KO[date.getDay()]})`;
}

export function formatTime(hhmm: string): string {
  const [hStr, mStr] = hhmm.split(':');
  const h = Number(hStr);
  const m = Number(mStr);
  const period = h < 12 ? '오전' : '오후';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${period} ${h12}:${String(m).padStart(2, '0')}`;
}

export function formatTimeRange(start: string, end: string): string {
  return `${formatTime(start)} ~ ${formatTime(end)}`;
}

export function formatDateTime(dateIso: string, start: string, end: string): string {
  return `${formatDate(dateIso)} ${formatTimeRange(start, end)}`;
}

export function formatCurrency(amount: number): string {
  return `${amount.toLocaleString()}원`;
}

export const ActivityTypeLabel: Record<ActivityType, string> = { HOBBY: '취미 모임', CLASS: '전문 클래스' };
export const ActivityFieldLabel: Record<ActivityField, string> = { ART: '그림', PHOTO: '촬영' };
export const ActivityStatusLabel: Record<ActivityStatus, string> = {
  DRAFT: '작성 중', PENDING: '공간 승인 대기', PUBLISHED: '모집 중', REJECTED: '거절',
};
export const RequestStatusLabel: Record<RequestStatus, string> = { PENDING: '승인 대기', APPROVED: '승인', REJECTED: '거절' };
export const FacilityTypeLabel: Record<FacilityType, string> = {
  TABLE: '넓은 테이블', LIGHTING: '조명', NATURAL_LIGHT: '자연광', WATER: '물 사용/세면시설', OUTLET: '콘센트', WIFI: '와이파이',
};
export const DayOfWeekLabel: Record<DayOfWeek, string> = {
  MONDAY: '월', TUESDAY: '화', WEDNESDAY: '수', THURSDAY: '목', FRIDAY: '금', SATURDAY: '토', SUNDAY: '일',
};
