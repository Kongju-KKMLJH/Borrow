package kkmljh.borrow.space.dto;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Space;

import java.time.LocalDate;
import java.time.LocalTime;

/** 확정 일정 (B-11): 승인된 활동의 날짜·시간·공간 요약 */
public record ScheduleResponse(
        Long requestId,
        Long activityId,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        Long spaceId,
        String spaceName
) {
    public static ScheduleResponse from(HostingRequest request) {
        Activity activity = request.getActivity();
        Space space = request.getSpace();
        return new ScheduleResponse(
                request.getId(),
                activity.getId(),
                activity.getTitle(),
                activity.getDate(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getCapacity(),
                space.getId(),
                space.getName()
        );
    }
}
