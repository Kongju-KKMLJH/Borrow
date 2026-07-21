package kkmljh.borrow.activity.dto;

import kkmljh.borrow.domain.Participation;

/** U-04 참여 신청 결과 */
public record ParticipationResponse(
        Long id,
        Long activityId,
        String nickname,
        int headcount
) {
    public static ParticipationResponse from(Participation p) {
        return new ParticipationResponse(
                p.getId(),
                p.getActivity().getId(),
                p.getNickname(),
                p.getHeadcount()
        );
    }
}
