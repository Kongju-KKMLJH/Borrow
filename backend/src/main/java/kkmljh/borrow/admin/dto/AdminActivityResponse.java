package kkmljh.borrow.admin.dto;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 관리자 프로그램 목록의 한 줄 (기능명세 7.2.1 display —
 * 프로그램명·담당 예술가·공간·일정·상태).
 *
 * <p>상태는 백엔드 {@code ActivityStatus} 를 그대로 내려준다. 명세의 "승인대기·모집중·마감·종료"
 * 라벨은 <b>화면에서 매핑</b>한다 — 관리자 목록 하나 때문에 상태 모델을 늘리지 않는다.
 *
 * @param spaceName 확정된 개최지 이름. 아직 승인된 개최 요청이 없으면 null
 */
public record AdminActivityResponse(
        Long id,
        String title,
        ActivityType type,
        String hostLoginId,
        String hostNickname,
        Long spaceId,
        String spaceName,
        String spaceRegion,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        ActivityStatus status
) {
    public static AdminActivityResponse of(Activity activity, ConfirmedSpace space) {
        return new AdminActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getType(),
                activity.getGuestId(),
                activity.getHostNickname(),
                space != null ? space.spaceId() : null,
                space != null ? space.name() : null,
                space != null ? space.region() : null,
                activity.getDate(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getCapacity(),
                activity.getStatus());
    }
}
