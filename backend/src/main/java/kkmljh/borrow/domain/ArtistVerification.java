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
 * 예술가 인증 신청 (기능명세 1.2).
 *
 * <p><b>인증 상태의 진실은 이 엔티티 한 곳이다.</b> {@code AppUser}에 인증 상태 필드를 두면
 * 승인 시점에 값이 갈린다 — Activity에 Space FK를 두지 않은 것과 같은 이유다.
 *
 * <p><b>회원당 1행.</b> 재신청은 새 행을 만들지 않고 기존 행을 PENDING으로 되돌린다
 * ({@link #reapply}). 신청 이력이 필요해지면 별도 이슈로 다룬다.
 *
 * <p>심사는 관리자 화면 없이 <b>DB에서 status를 직접 바꾸는 방식</b>으로 운영한다
 * (PRD가 관리자 심사 화면을 MVP에서 제외). {@link #approve()}·{@link #reject(String)} 는
 * 그 전이 규칙을 코드로 못 박아 두는 것이며, 관리자 API가 생기면 그대로 진입점이 된다.
 */
@Entity
@Table(name = "artist_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청자의 로그인 아이디 — 소유 판정 기준값 (AppUser.loginId). 회원당 1행이라 유니크. */
    @Column(nullable = false, unique = true)
    private String loginId;

    /** 포트폴리오 URL (작품·활동을 확인할 수 있는 링크) */
    @Column(nullable = false, length = 1000)
    private String portfolioUrl;

    /** 활동 경력 자유 텍스트 */
    @Column(length = 2000)
    private String career;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtistVerificationStatus status;

    /** 거절 사유 — REJECTED 일 때만 값이 있다 */
    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ArtistVerification(String loginId, String portfolioUrl, String career) {
        this.loginId = loginId;
        this.portfolioUrl = portfolioUrl;
        this.career = career;
        this.status = ArtistVerificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /** 승인 — 이 시점부터 개설하는 활동에 인증 배지(F-01)가 붙는다. */
    public void approve() {
        ensurePending();
        this.status = ArtistVerificationStatus.APPROVED;
        this.reason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** 거절 — 사유를 남긴다. 이 상태에서만 재신청할 수 있다. */
    public void reject(String reason) {
        ensurePending();
        this.status = ArtistVerificationStatus.REJECTED;
        this.reason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 재신청 — 거절된 신청만 다시 올릴 수 있다.
     * 심사 중(PENDING)이거나 이미 승인(APPROVED)된 신청을 다시 올리는 것은 ALREADY_REQUESTED.
     */
    public void reapply(String portfolioUrl, String career) {
        if (this.status != ArtistVerificationStatus.REJECTED) {
            throw new BusinessException(ErrorCode.ALREADY_REQUESTED);
        }
        this.portfolioUrl = portfolioUrl;
        this.career = career;
        this.status = ArtistVerificationStatus.PENDING;
        this.reason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 회원의 인증 상태를 <b>전이 규칙을 건너뛰고</b> 관리자가 직접 맞춘다 (기능명세 7.1.2 dataSpec).
     *
     * <p>{@link #approve()}·{@link #reject(String)} 는 PENDING 에서만 움직이는데, 관리자 콘솔은
     * "이미 승인된 예술가"를 한 번에 만들어야 한다. <b>관리자 콘솔 전용</b>이다 —
     * 실제 심사는 {@code approve()} 를 거치고, 그 전이 규칙을 무르게 만들지 않기 위해 진입점을 나눈다.
     */
    public void forceStatus(ArtistVerificationStatus status) {
        this.status = status;
        this.reason = (status == ArtistVerificationStatus.REJECTED) ? "관리자가 지정한 상태" : null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return this.status == ArtistVerificationStatus.APPROVED;
    }

    private void ensurePending() {
        if (this.status != ArtistVerificationStatus.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_ALREADY_HANDLED);
        }
    }
}
