package kkmljh.borrow.activity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** U-08 공간 요구조건 수정 (직접 입력 또는 AI 분석 결과 반영) */
public record RequirementUpdateRequest(
        @NotNull @Valid SpaceRequirementDto requirement
) {
}
