package kkmljh.borrow.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** U-04 참여 신청 */
public record ParticipationRequest(
        @NotBlank String nickname,
        @Positive int headcount
) {
}
