package kkmljh.borrow.activity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.activity.dto.ParticipationRequest;
import kkmljh.borrow.activity.dto.ParticipationResponse;
import kkmljh.borrow.activity.service.ParticipationService;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게스트 참여 신청/취소 (U-04, U-05) */
@Tag(name = "활동 참여 (C)", description = "게스트의 활동 참여 신청·취소 (U-04, U-05)")
@RestController
@RequestMapping("/api/activities/{activityId}/participations")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    @Operation(summary = "U-04 참여 신청", description = "게스트가 활동에 참여 신청한다.")
    @PostMapping
    public ApiResponse<ParticipationResponse> participate(
            @GuestId String guestId,
            @PathVariable Long activityId,
            @Valid @RequestBody ParticipationRequest request) {
        return ApiResponse.ok(participationService.participate(guestId, activityId, request));
    }

    @Operation(summary = "U-05 참여 취소", description = "게스트가 참여 신청을 취소한다.")
    @DeleteMapping
    public ApiResponse<Void> cancel(@GuestId String guestId, @PathVariable Long activityId) {
        participationService.cancel(guestId, activityId);
        return ApiResponse.ok();
    }
}
