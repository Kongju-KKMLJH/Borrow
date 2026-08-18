package kkmljh.borrow.auth.dto;

import kkmljh.borrow.domain.ArtistVerification;

import java.time.LocalDateTime;

/**
 * 예술가 인증 상태 응답 (기능명세 1.2).
 *
 * <p>신청한 적이 없으면 404가 아니라 <b>200 + status "NONE"</b> 이다 —
 * 404로 만들면 프론트가 "에러"와 "미신청"을 가르는 분기를 따로 짜야 한다.
 * {@code NONE} 은 저장되는 상태가 아니므로 enum이 아니라 이 응답에서만 문자열로 존재한다.
 */
public record ArtistVerificationResponse(
        String status,
        String portfolioUrl,
        String career,
        String reason,
        LocalDateTime appliedAt,
        LocalDateTime updatedAt
) {
    /** 신청 이력이 없는 상태 */
    public static final String NONE = "NONE";

    public static ArtistVerificationResponse none() {
        return new ArtistVerificationResponse(NONE, null, null, null, null, null);
    }

    public static ArtistVerificationResponse from(ArtistVerification verification) {
        return new ArtistVerificationResponse(
                verification.getStatus().name(),
                verification.getPortfolioUrl(),
                verification.getCareer(),
                verification.getReason(),
                verification.getCreatedAt(),
                verification.getUpdatedAt());
    }
}
