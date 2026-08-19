package kkmljh.borrow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.domain.Role;

/**
 * 임시(mock) 회원 생성·수정 요청 (기능명세 7.1.2 dataSpec — 아이디·닉네임·역할·인증 상태).
 *
 * <p>제약은 {@code SignupRequest} 와 같은 값으로 맞춘다 — 임시 회원도 <b>실제로 로그인해서
 * 화면을 확인하는 용도</b>이므로 가입 경로보다 느슨하면 만들어 놓고 로그인이 안 되는 계정이 생긴다.
 *
 * @param password           수정 시에는 비워 둘 수 있다(그대로 유지). 생성 시에는 필수.
 * @param verificationStatus 예술가 인증 상태. {@code null} 이면 "신청한 적 없음"으로 두고,
 *                           역할이 ARTIST 가 아닐 때 값을 주면 거절한다.
 */
public record AdminUserRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(min = 3, max = 30, message = "로그인 아이디는 3~30자여야 합니다.")
        String loginId,

        @Size(min = 4, max = 64, message = "비밀번호는 4자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname,

        @NotNull(message = "회원 유형(role)은 필수입니다.")
        Role role,

        ArtistVerificationStatus verificationStatus
) {
    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}
