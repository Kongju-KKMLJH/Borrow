package kkmljh.borrow.activity.dto;

import jakarta.validation.constraints.Positive;

/** U-04 참여 신청. 닉네임은 로그인 계정에서 서버가 채운다(사칭 방지). */
public record ParticipationRequest(
        @Positive int headcount
) {
}
