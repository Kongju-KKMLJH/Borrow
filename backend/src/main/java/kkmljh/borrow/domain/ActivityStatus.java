package kkmljh.borrow.domain;

/**
 * 활동 상태 흐름:
 * DRAFT(개설 입력 완료) → PENDING(개최 요청 전송, U-11) → MATCHED(승인, 결제 대기, B-09)
 *                                                       → PUBLISHED(Mock 결제 완료 후 공개, 기능명세 3.3/3.3.1)
 *                                                       ↘ REJECTED(거절, B-10)
 */
public enum ActivityStatus {
    DRAFT,
    PENDING,
    MATCHED,
    PUBLISHED,
    REJECTED
}
