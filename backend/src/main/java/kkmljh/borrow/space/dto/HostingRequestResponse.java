package kkmljh.borrow.space.dto;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.domain.Space;

import java.time.LocalDate;
import java.time.LocalTime;

/** 개최 요청 응답 (B-07 목록, B-08 상세) — 사업자가 승인/거절 판단에 필요한 정보 */
public record HostingRequestResponse(
        Long id,
        RequestStatus status,
        String rejectReason,
        SpaceInfo space,
        ActivityInfo activity
) {
    public record SpaceInfo(Long id, String name, String region) {
        static SpaceInfo from(Space space) {
            return new SpaceInfo(space.getId(), space.getName(), space.getRegion());
        }
    }

    public record ActivityInfo(
            Long id,
            String title,
            ActivityField field,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int capacity,
            int entryFee,
            String hostNickname
    ) {
        static ActivityInfo from(Activity activity) {
            return new ActivityInfo(
                    activity.getId(),
                    activity.getTitle(),
                    activity.getField(),
                    activity.getDate(),
                    activity.getStartTime(),
                    activity.getEndTime(),
                    activity.getCapacity(),
                    activity.getEntryFee(),
                    activity.getHostNickname()
            );
        }
    }

    public static HostingRequestResponse from(HostingRequest request) {
        return new HostingRequestResponse(
                request.getId(),
                request.getStatus(),
                request.getRejectReason(),
                SpaceInfo.from(request.getSpace()),
                ActivityInfo.from(request.getActivity())
        );
    }
}
