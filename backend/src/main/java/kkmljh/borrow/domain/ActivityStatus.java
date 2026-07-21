package kkmljh.borrow.domain;

/**
 * 활동 상태 흐름:
 * DRAFT(개설 입력 완료) → PENDING(개최 요청 전송, U-11) → PUBLISHED(승인 후 자동 공개, S-01)
 *                                                       ↘ REJECTED(거절, B-10)
 */
public enum ActivityStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    REJECTED
}
