package kkmljh.borrow.activity.dto;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;

import java.time.LocalDate;
import java.time.LocalTime;

/** U-03 활동 상세. hostCertified 배지(F-01 Mock) 표시 포함 */
public record ActivityDetailResponse(
        Long id,
        ActivityType type,
        ActivityField field,
        String title,
        String description,
        String hostNickname,
        boolean hostCertified,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int currentHeadcount,
        int entryFee,
        ActivityStatus status,
        SpaceRequirementDto requirement,
        boolean alreadyJoined,
        boolean mine
) {
    public static ActivityDetailResponse of(Activity a, int currentHeadcount, boolean alreadyJoined, boolean mine) {
        return new ActivityDetailResponse(
                a.getId(),
                a.getType(),
                a.getField(),
                a.getTitle(),
                a.getDescription(),
                a.getHostNickname(),
                a.isHostCertified(),
                a.getDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getCapacity(),
                currentHeadcount,
                a.getEntryFee(),
                a.getStatus(),
                SpaceRequirementDto.from(a.getRequirement()),
                alreadyJoined,
                mine
        );
    }

    /** 게스트 컨텍스트가 없을 때(alreadyJoined·mine 모두 false 고정) — 개설 직후 등 */
    public static ActivityDetailResponse of(Activity a, int currentHeadcount) {
        return of(a, currentHeadcount, false, false);
    }
}
