package kkmljh.borrow.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.space.dto.HostingRequestResponse;
import kkmljh.borrow.space.dto.RejectRequest;
import kkmljh.borrow.space.service.HostingRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 사업자 개최요청 처리 (B-07~B-10) */
@Tag(name = "개최요청 처리 (B)", description = "공간 제공자가 받은 개최요청 조회·승인·거절 (B-07~B-10)")
@RestController
@RequestMapping("/api/host/requests")
@RequiredArgsConstructor
public class HostingRequestController {

    private final HostingRequestService hostingRequestService;

    @Operation(summary = "B-07 받은 개최요청 목록", description = "spaceId/status로 선택 필터링 가능.")
    @GetMapping
    public ApiResponse<List<HostingRequestResponse>> list(
            @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) RequestStatus status) {
        return ApiResponse.ok(hostingRequestService.findRequests(spaceId, status));
    }

    @Operation(summary = "B-08 개최요청 상세", description = "개최요청 단건 상세 조회.")
    @GetMapping("/{requestId}")
    public ApiResponse<HostingRequestResponse> detail(@PathVariable Long requestId) {
        return ApiResponse.ok(hostingRequestService.findById(requestId));
    }

    @Operation(summary = "B-09 개최요청 승인", description = "승인 시 해당 활동이 자동 공개된다(S-01).")
    @PostMapping("/{requestId}/approve")
    public ApiResponse<HostingRequestResponse> approve(@PathVariable Long requestId) {
        return ApiResponse.ok(hostingRequestService.approve(requestId));
    }

    @Operation(summary = "B-10 개최요청 거절", description = "거절 사유(reason)는 선택. 본문 생략 가능.")
    @PostMapping("/{requestId}/reject")
    public ApiResponse<HostingRequestResponse> reject(@PathVariable Long requestId,
                                                      @RequestBody(required = false) RejectRequest request) {
        String reason = (request == null) ? null : request.reason();
        return ApiResponse.ok(hostingRequestService.reject(requestId, reason));
    }
}
