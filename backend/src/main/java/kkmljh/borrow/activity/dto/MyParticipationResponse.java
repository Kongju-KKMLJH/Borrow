package kkmljh.borrow.activity.dto;

import kkmljh.borrow.domain.Participation;

/** U-14 내가 참여한 활동: 활동 요약 + 내 참여 정보 */
public record MyParticipationResponse(
        Long participationId,
        int myHeadcount,
        ActivitySummaryResponse activity
) {
    public static MyParticipationResponse of(Participation p, int currentHeadcount) {
        return new MyParticipationResponse(
                p.getId(),
                p.getHeadcount(),
                // 이 목록은 정의상 전부 내가 참여한 활동이므로 alreadyJoined=true.
                // 개설자는 자기 활동에 참여할 수 없으므로 mine=false.
                ActivitySummaryResponse.of(p.getActivity(), currentHeadcount, true, false)
        );
    }
}
