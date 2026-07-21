package kkmljh.borrow.activity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.activity.dto.HostingRequestCreateRequest;
import kkmljh.borrow.activity.dto.HostingRequestResponse;
import kkmljh.borrow.activity.service.ActivityHostingRequestService;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 개최 요청 전송/상태 조회 (U-11, U-12). HostingRequest는 B 소유 — C는 생성/조회만. */
@Tag(name = "개최요청 전송 (C)", description = "AI 매칭으로 선택한 공간에 개최요청 전송·상태 조회 (U-11, U-12)")
@RestController
@RequestMapping("/api/activities/{activityId}/hosting-request")
@RequiredArgsConstructor
public class ActivityHostingRequestController {

    private final ActivityHostingRequestService hostingRequestService;

    @Operation(summary = "U-11 개최 요청 전송", description = "AI 매칭으로 선택한 공간(spaceId)에 개최요청을 보낸다.")
    @PostMapping
    public ApiResponse<HostingRequestResponse> send(
            @GuestId String guestId,
            @PathVariable Long activityId,
            @Valid @RequestBody HostingRequestCreateRequest request) {
        return ApiResponse.ok(hostingRequestService.send(guestId, activityId, request.spaceId()));
    }

    @Operation(summary = "U-12 개최 요청 상태 조회", description = "해당 활동의 개최요청 현재 상태.")
    @GetMapping
    public ApiResponse<HostingRequestResponse> status(
            @GuestId String guestId,
            @PathVariable Long activityId) {
        return ApiResponse.ok(hostingRequestService.status(guestId, activityId));
    }
}
