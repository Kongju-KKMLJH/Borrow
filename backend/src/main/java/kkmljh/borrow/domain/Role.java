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
    ARTIST,

    /**
     * 관리자 — 관리자 콘솔에서 회원·프로그램·공간 조회와 통제 조치 (기능명세 7).
     *
     * <p><b>회원가입으로는 만들 수 없다.</b> {@code POST /api/auth/signup} 은 비로그인 허용이라
     * 이 값을 그대로 받으면 누구나 관리자가 된다 — {@code AuthService.signup} 이 거부하고,
     * 계정은 {@code AdminAccountInitializer} 가 환경변수를 읽어 만든다.
     */
    ADMIN;

    /** Spring Security 권한 문자열 (hasRole("MEMBER") 은 ROLE_MEMBER 를 찾는다) */
    public String authority() {
        return "ROLE_" + name();
    }
}
