package kkmljh.borrow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
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

    /**
     * 관리자가 만든 임시(mock) 회원인지 (기능명세 7.1.2).
     * 관리자 콘솔의 수정·삭제는 이 값이 true 인 회원에게만 허용한다.
     */
    @Column(nullable = false)
    private boolean mock;

    /** 강제 탈퇴 처리 일시 — null 이면 정상 회원 (기능명세 7.1.3 dataSpec) */
    private LocalDateTime withdrawnAt;

    @Builder
    private AppUser(String loginId, String password, String nickname, Role role, boolean mock) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.mock = mock;
        this.createdAt = LocalDateTime.now();
    }

    /** 예술가가 개설한 활동은 CLASS + 인증 배지(F-01) */
    public boolean isArtist() {
        return this.role == Role.ARTIST;
    }

    /**
     * 관리자의 임시(mock) 회원 정보 수정 (기능명세 7.1.2).
     *
     * <p>{@code loginId} 는 바꾸지 않는다 — 이 값이 활동·참여·공간의 소유자 키라서
     * 갈아치우면 그 회원이 만든 데이터가 전부 주인을 잃는다.
     * 호출부가 임시 회원인지 먼저 확인한다.
     */
    public void updateByAdmin(String nickname, Role role) {
        this.nickname = nickname;
        this.role = role;
    }

    /** 이미 인코딩된 해시만 받는다 (평문 저장 금지). */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 관리자의 강제 탈퇴 (기능명세 7.1.3). 데이터는 지우지 않고 비활성 상태로만 남긴다 —
     * 탈퇴 회원이 개설했던 활동·참여 내역이 함께 사라지면 남은 참여자 화면이 깨진다.
     * 로그인 차단은 {@code AppUserDetailsService} 가 이 값을 보고 처리한다.
     */
    public void withdraw() {
        if (isWithdrawn()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 탈퇴 처리된 회원입니다.");
        }
        this.withdrawnAt = LocalDateTime.now();
    }

    public boolean isWithdrawn() {
        return this.withdrawnAt != null;
    }
}
