package kkmljh.borrow.domain;

/** 활동 유형 (U-02 필터, U-06): 취미 모임 / 전문 클래스 */
public enum ActivityType {
    HOBBY, // 취미 모임 — 일반 사용자가 개설 (U-06~U-08)
    CLASS  // 전문 클래스 — 인증 예술가 연계(F-01/F-02, Mock). MVP에서는 필터·시드 데이터로만 사용
}
