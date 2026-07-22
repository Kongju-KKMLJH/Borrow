package kkmljh.borrow.activity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kkmljh.borrow.domain.ActivityField;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 취미 모임 개설 (U-06 기본정보 / U-07 일정·정원 / U-08 공간 요구조건).
 * 일반 사용자가 개설하는 활동은 항상 HOBBY 이므로 type은 받지 않는다.
 */
public record ActivityCreateRequest(
        @NotBlank String hostNickname,
        @NotNull ActivityField field,
        @NotBlank String title,
        String description,
        List<String> imageUrls,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive int capacity,
        @Min(0) int entryFee,
        @Valid SpaceRequirementDto requirement
) {
}
