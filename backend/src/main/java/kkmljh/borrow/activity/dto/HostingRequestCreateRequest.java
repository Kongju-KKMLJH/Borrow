package kkmljh.borrow.activity.dto;

import jakarta.validation.constraints.NotNull;

/**
 * U-11 개최 요청 전송. AI 매칭(A-02/A-03)으로 추천된 공간 중 사용자가 선택한 spaceId.
 */
public record HostingRequestCreateRequest(
        @NotNull Long spaceId
) {
}
