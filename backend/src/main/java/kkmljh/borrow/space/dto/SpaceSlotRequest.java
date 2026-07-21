package kkmljh.borrow.space.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** 유휴 시간대 등록 요청 (B-05): 요일 + 시작/종료 시각 */
public record SpaceSlotRequest(
        @NotNull(message = "요일은 필수입니다.")
        DayOfWeek dayOfWeek,

        @NotNull(message = "시작 시각은 필수입니다.")
        LocalTime startTime,

        @NotNull(message = "종료 시각은 필수입니다.")
        LocalTime endTime
) {
}
