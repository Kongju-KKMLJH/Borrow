package kkmljh.borrow.activity.dto;

import kkmljh.borrow.activity.dto.ActivityDetailResponse.SpaceInfo;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** U-01 목록 / U-13 내 활동 카드용 요약 응답 */
public record ActivitySummaryResponse(
        Long id,
        ActivityType type,
        ActivityField field,
        String title,
        List<String> imageUrls,
        String hostNickname,
        boolean hostCertified,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int currentHeadcount,
        int remainingCapacity,
        int entryFee,
        ActivityStatus status,
        SpaceInfo space,
        boolean alreadyJoined,
        boolean mine
) {
    public static ActivitySummaryResponse of(Activity a, int currentHeadcount, boolean alreadyJoined, boolean mine,
                                             SpaceInfo space) {
        return new ActivitySummaryResponse(
                a.getId(),
                a.getType(),
                a.getField(),
                a.getTitle(),
                a.getImageUrls(),
                a.getHostNickname(),
                a.isHostCertified(),
                a.getDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getCapacity(),
                currentHeadcount,
                ActivityDetailResponse.remaining(a.getCapacity(), currentHeadcount),
                a.getEntryFee(),
                a.getStatus(),
                space,
                alreadyJoined,
                mine
        );
    }

    /** 개최지가 아직 확정되지 않은 경우(DRAFT·PENDING·REJECTED) */
    public static ActivitySummaryResponse of(Activity a, int currentHeadcount, boolean alreadyJoined, boolean mine) {
        return of(a, currentHeadcount, alreadyJoined, mine, null);
    }

    /** 게스트 컨텍스트가 없을 때(alreadyJoined·mine 모두 false 고정) */
    public static ActivitySummaryResponse of(Activity a, int currentHeadcount) {
        return of(a, currentHeadcount, false, false, null);
    }
}
