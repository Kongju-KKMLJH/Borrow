package kkmljh.borrow.activity.dto;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** U-03 활동 상세. hostCertified 배지(F-01 Mock) 표시 포함 */
public record ActivityDetailResponse(
        Long id,
        ActivityType type,
        ActivityField field,
        String title,
        String description,
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
        SpaceRequirementDto requirement,
        boolean alreadyJoined,
        boolean mine
) {

    /**
     * 기능명세 4.1 상세에 표시할 <b>확정 개최지</b> 요약. 개최 요청이 승인(APPROVED)되기 전에는 null이다.
     *
     * <p><b>주소({@code address}) 필드를 추가하지 마라.</b> 공개 응답의 주소는 동 단위({@code region})까지라는
     * 기능명세 6.1 {@code rules} 정책이 여기서 뚫린다. 주소 전문은 소유 HOST 본인에게만 준다
     * ({@code SpaceResponse.forOwner}).
     */
    public record SpaceInfo(Long id, String name, String region) {
        public static SpaceInfo from(ConfirmedSpace confirmed) {
            return confirmed == null
                    ? null
                    : new SpaceInfo(confirmed.spaceId(), confirmed.name(), confirmed.region());
        }
    }

    public static ActivityDetailResponse of(Activity a, int currentHeadcount, boolean alreadyJoined, boolean mine,
                                            SpaceInfo space) {
        return new ActivityDetailResponse(
                a.getId(),
                a.getType(),
                a.getField(),
                a.getTitle(),
                a.getDescription(),
                a.getImageUrls(),
                a.getHostNickname(),
                a.isHostCertified(),
                a.getDate(),
                a.getStartTime(),
                a.getEndTime(),
                a.getCapacity(),
                currentHeadcount,
                remaining(a.getCapacity(), currentHeadcount),
                a.getEntryFee(),
                a.getStatus(),
                space,
                SpaceRequirementDto.from(a.getRequirement()),
                alreadyJoined,
                mine
        );
    }

    /** 개최지가 아직 확정되지 않은 경우(DRAFT·PENDING·REJECTED) */
    public static ActivityDetailResponse of(Activity a, int currentHeadcount, boolean alreadyJoined, boolean mine) {
        return of(a, currentHeadcount, alreadyJoined, mine, null);
    }

    /** 게스트 컨텍스트가 없을 때(alreadyJoined·mine 모두 false 고정) — 개설 직후 등 */
    public static ActivityDetailResponse of(Activity a, int currentHeadcount) {
        return of(a, currentHeadcount, false, false, null);
    }

    /**
     * 잔여 인원 (기능명세 4.1). 0이 하한이다 —
     * 정원 검증 공백(이슈 #9)으로 참여 합계가 정원을 넘어도 음수를 그대로 내려보내지 않는다.
     */
    static int remaining(int capacity, int currentHeadcount) {
        return Math.max(0, capacity - currentHeadcount);
    }
}
