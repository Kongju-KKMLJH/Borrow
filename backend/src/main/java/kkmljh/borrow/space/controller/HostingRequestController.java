package kkmljh.borrow.space.controller;

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
@RestController
@RequestMapping("/api/host/requests")
@RequiredArgsConstructor
public class HostingRequestController {

    private final HostingRequestService hostingRequestService;

    /** 받은 개최요청 목록 (B-07) — spaceId/status로 선택 필터링 */
    @GetMapping
    public ApiResponse<List<HostingRequestResponse>> list(
            @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) RequestStatus status) {
        return ApiResponse.ok(hostingRequestService.findRequests(spaceId, status));
    }

    /** 개최요청 상세 (B-08) */
    @GetMapping("/{requestId}")
    public ApiResponse<HostingRequestResponse> detail(@PathVariable Long requestId) {
        return ApiResponse.ok(hostingRequestService.findById(requestId));
    }

    /** 개최요청 승인 (B-09) */
    @PostMapping("/{requestId}/approve")
    public ApiResponse<HostingRequestResponse> approve(@PathVariable Long requestId) {
        return ApiResponse.ok(hostingRequestService.approve(requestId));
    }

    /** 개최요청 거절 (B-10) */
    @PostMapping("/{requestId}/reject")
    public ApiResponse<HostingRequestResponse> reject(@PathVariable Long requestId,
                                                      @RequestBody(required = false) RejectRequest request) {
        String reason = (request == null) ? null : request.reason();
        return ApiResponse.ok(hostingRequestService.reject(requestId, reason));
    }
}
