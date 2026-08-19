package kkmljh.borrow.admin.dto;

import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.ArtistVerificationStatus;

import java.time.LocalDateTime;

/**
 * 관리자 심사 화면의 예술가 인증 신청 한 건 (기능명세 7.1.4 display).
 * 닉네임은 신청 엔티티에 없어 회원 조회로 채운다(없으면 null).
 */
public record AdminVerificationResponse(
        Long id,
        String loginId,
        String nickname,
        String portfolioUrl,
        String career,
        ArtistVerificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminVerificationResponse of(ArtistVerification verification, String nickname) {
        return new AdminVerificationResponse(
                verification.getId(),
                verification.getLoginId(),
                nickname,
                verification.getPortfolioUrl(),
                verification.getCareer(),
                verification.getStatus(),
                verification.getCreatedAt(),
                verification.getUpdatedAt());
    }
}
