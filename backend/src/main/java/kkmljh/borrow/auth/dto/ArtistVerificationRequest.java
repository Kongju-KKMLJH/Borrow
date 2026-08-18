package kkmljh.borrow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 예술가 인증 신청 요청 (기능명세 1.2).
 * 신청자는 로그인 계정에서 정하므로 요청 DTO에 loginId를 두지 않는다 (남의 이름으로 신청 방지).
 */
public record ArtistVerificationRequest(

        @NotBlank(message = "포트폴리오 URL은 필수입니다.")
        @Size(max = 1000, message = "포트폴리오 URL은 1000자 이하여야 합니다.")
        String portfolioUrl,

        @Size(max = 2000, message = "활동 경력은 2000자 이하여야 합니다.")
        String career
) {
}
