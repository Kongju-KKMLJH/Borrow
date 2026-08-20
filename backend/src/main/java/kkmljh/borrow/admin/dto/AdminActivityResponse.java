package kkmljh.borrow.admin.dto;

import kkmljh.borrow.activity.repository.ConfirmedSpace;
import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 관리자 프로그램 목록의 한 줄 (기능명세 7.2.1 display —
 * 프로그램명·담당 예술가·공간·일정·상태).
 *
 * <p>상태는 백엔드 {@code ActivityStatus} 를 그대로 내려준다. 명세의 "승인대기·모집중·마감·종료"
 * 라벨은 <b>화면에서 매핑</b>한다 — 관리자 목록 하나 때문에 상태 모델을 늘리지 않는다.
 *
 * <p><b>목록 표시에 쓰지 않는 필드까지 담는 이유</b>(7.2.2): 관리자 콘솔의 수정 폼이 이 목록 응답을
 * 그대로 프리필 소스로 쓴다. 여기 없는 필드는 폼이 빈 값·기본값으로 시작하고, 수정 요청이 전체
 * 교체라 <b>저장하는 순간 원본이 지워진다</b>.
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
        ActivityStatus status,
        ActivityField field,
        String description,
        int entryFee,
        List<String> imageUrls
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
                activity.getStatus(),
                activity.getField(),
                activity.getDescription(),
                activity.getEntryFee(),
                List.copyOf(activity.getImageUrls()));
    }
}
