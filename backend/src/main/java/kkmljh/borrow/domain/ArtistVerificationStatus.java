package kkmljh.borrow.domain;

/**
 * 예술가 인증 신청 상태 (기능명세 1.2).
 *
 * <p><b>"신청한 적 없음"에 해당하는 값(NONE)은 여기에 두지 않는다.</b>
 * 신청하지 않은 상태는 <b>행이 없는 것</b>이고, 저장될 일이 없는 값이 상태 전이 표에 끼면
 * 분기가 두 벌이 된다. 미신청은 응답 DTO에서만 {@code "NONE"} 문자열로 표현한다.
 */
public enum ArtistVerificationStatus {

    /** 심사 대기 */
    PENDING,

    /** 승인됨 — 이 상태에서만 인증 배지(F-01)가 붙는다 */
    APPROVED,

    /** 거절됨 — 이 상태에서만 재신청할 수 있다 */
    REJECTED
}
