package kkmljh.borrow.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A-01 활동 분석 요청. 게스트가 입력한 자유 텍스트 설명 → 구조화 공간 요구조건.
 *
 * @param description 활동 설명 자유 텍스트 (필수)
 * @param region      희망 지역 힌트 (선택, 예: "천안시 서북구"). 결과에 그대로 반영된다.
 */
public record AnalyzeRequest(
        @NotBlank(message = "활동 설명은 필수입니다.")
        String description,
        String region
) {
}