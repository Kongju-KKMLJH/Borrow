package kkmljh.borrow.activity.controller;

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
@RestController
@RequestMapping("/api/activities/{activityId}/participations")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    /** U-04 참여 신청 */
    @PostMapping
    public ApiResponse<ParticipationResponse> participate(
            @GuestId String guestId,
            @PathVariable Long activityId,
            @Valid @RequestBody ParticipationRequest request) {
        return ApiResponse.ok(participationService.participate(guestId, activityId, request));
    }

    /** U-05 참여 취소 */
    @DeleteMapping
    public ApiResponse<Void> cancel(@GuestId String guestId, @PathVariable Long activityId) {
        participationService.cancel(guestId, activityId);
        return ApiResponse.ok();
    }
}
