package kkmljh.borrow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 계정. Spring Security의 {@code User}와 헷갈리므로 클래스명은 AppUser 로 고정한다.
 *
 * <p>{@code loginId} 값이 기존 게스트 식별자(guestId) 자리에 그대로 들어간다 —
 * Activity.guestId, Participation.guestId, Space.ownerId 는 모두 이 값이다.
 * password 는 반드시 BCrypt 해시로만 저장하고 응답 DTO에 담지 않는다.
 */
@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디 — 소유권 판정의 기준값 */
    @Column(nullable = false, unique = true)
    private String loginId;

    /** BCrypt 해시 (평문 저장 금지) */
    @Column(nullable = false)
    private String password;

    /** 활동 개설자·참여자 표시 이름의 원본 */
    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** 가입일 — 관리자 회원 목록의 표시 항목 (기능명세 7.1.1 display) */
    private LocalDateTime createdAt;

    @Builder
    private AppUser(String loginId, String password, String nickname, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    /** 예술가가 개설한 활동은 CLASS + 인증 배지(F-01) */
    public boolean isArtist() {
        return this.role == Role.ARTIST;
    }

    /**
     * 관리자 콘솔의 회원 정보 수정 경로 (기능명세 7.1.2).
     *
     * <p>{@code loginId} 는 바꾸지 않는다 — 이 값이 활동·참여·공간의 소유자 키라서
     * 갈아치우면 그 회원이 만든 데이터가 전부 주인을 잃는다.
     * 일반 회원이 자기 정보를 고치는 경로가 아니다 — 호출부는 {@code AdminUserService} 뿐이다.
     */
    public void updateByAdmin(String nickname, Role role) {
        this.nickname = nickname;
        this.role = role;
    }

    /** 이미 인코딩된 해시만 받는다 (평문 저장 금지). */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
