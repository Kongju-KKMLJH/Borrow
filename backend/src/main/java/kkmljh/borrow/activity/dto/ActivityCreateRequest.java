package kkmljh.borrow.activity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kkmljh.borrow.domain.ActivityField;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 활동 개설 (U-06 기본정보 / U-07 일정·정원 / U-08 공간 요구조건).
 *
 * <p>type·hostCertified·hostNickname 은 받지 않는다 — 서버가 로그인 역할·계정에서 채운다.
 * (클라이언트가 보내면 CLASS 배지나 남의 이름을 위조할 수 있다.)
 */
public record ActivityCreateRequest(
        @NotNull ActivityField field,
        @NotBlank String title,
        String description,
        List<String> imageUrls,
        @NotNull @FutureOrPresent LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive int capacity,
        @Min(0) int entryFee,
        @Valid SpaceRequirementDto requirement
) {
}
