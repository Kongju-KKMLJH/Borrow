package kkmljh.borrow.admin.dto;

import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.domain.Role;

import java.time.LocalDateTime;

/**
 * 관리자 회원 목록의 한 줄 (기능명세 7.1.1 display).
 *
 * <p>비밀번호는 물론 PK도 담지 않는 {@code MeResponse} 와 달리 <b>{@code id} 를 담는다</b> —
 * 강제 탈퇴 API가 이 값을 대상으로 받는다. 비밀번호는 여기서도 절대 담지 않는다.
 *
 * @param verificationStatus 예술가 인증 상태. 신청 행이 없으면 {@code "NONE"}
 *                           ({@code ArtistVerificationStatus} 에 NONE 값을 만들지 않는다는 규칙 유지)
 */
public record AdminUserResponse(
        Long id,
        String loginId,
        String nickname,
        Role role,
        LocalDateTime createdAt,
        String verificationStatus,
        boolean mock,
        boolean withdrawn,
        LocalDateTime withdrawnAt
) {
    private static final String NOT_APPLIED = "NONE";

    public static AdminUserResponse of(AppUser user, ArtistVerificationStatus verificationStatus) {
        return new AdminUserResponse(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt(),
                verificationStatus != null ? verificationStatus.name() : NOT_APPLIED,
                user.isMock(),
                user.isWithdrawn(),
                user.getWithdrawnAt());
    }
}
