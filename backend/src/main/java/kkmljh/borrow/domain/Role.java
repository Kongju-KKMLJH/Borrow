package kkmljh.borrow.domain;

/**
 * 회원 유형. 회원가입 시 하나만 선택하며 이후 변경하지 않는다.
 * 역할이 가르는 것은 "무엇을 개설·관리할 수 있는가"뿐이고,
 * 활동 열람·참여와 /api/me/** 는 역할과 무관하게 로그인한 모두에게 열려 있다.
 */
public enum Role {

    /** 일반 회원 — 취미 모임(HOBBY) 개설, 공개된 활동에 참여 */
    MEMBER,

    /** 공간 제공자 — 유휴공간·유휴 시간대 등록, 개최 요청 승인/거절 */
    HOST,

    /** 예술가 — MEMBER가 하는 일 전부 + 원데이클래스(CLASS) 개설 */
    ARTIST;

    /** Spring Security 권한 문자열 (hasRole("MEMBER") 은 ROLE_MEMBER 를 찾는다) */
    public String authority() {
        return "ROLE_" + name();
    }
}
