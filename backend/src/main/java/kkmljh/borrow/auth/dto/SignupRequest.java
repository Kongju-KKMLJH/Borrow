package kkmljh.borrow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kkmljh.borrow.domain.Role;

/**
 * 회원가입 요청. 역할은 가입 시 하나만 고르고 이후 변경 API는 없다.
 * MEMBER = 일반 회원, HOST = 공간 제공자, ARTIST = 예술가.
 */
public record SignupRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(min = 3, max = 30, message = "로그인 아이디는 3~30자여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 64, message = "비밀번호는 4자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname,

        @NotNull(message = "회원 유형(role)은 필수입니다.")
        Role role
) {
}
