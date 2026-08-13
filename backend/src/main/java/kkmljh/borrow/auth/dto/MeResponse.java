package kkmljh.borrow.auth.dto;

import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.Role;

/** 내 정보 응답. PK(id)와 비밀번호는 절대 담지 않는다. */
public record MeResponse(
        String loginId,
        String nickname,
        Role role
) {
    public static MeResponse from(AppUser user) {
        return new MeResponse(user.getLoginId(), user.getNickname(), user.getRole());
    }
}
