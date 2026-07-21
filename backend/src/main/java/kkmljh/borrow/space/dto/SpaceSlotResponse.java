package kkmljh.borrow.space.dto;

import kkmljh.borrow.domain.SpaceSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** 유휴 시간대 응답 (B-05) */
public record SpaceSlotResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static SpaceSlotResponse from(SpaceSlot slot) {
        return new SpaceSlotResponse(
                slot.getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime()
        );
    }
}
