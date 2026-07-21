package kkmljh.borrow.activity.controller;

import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.MyParticipationResponse;
import kkmljh.borrow.activity.service.ActivityService;
import kkmljh.borrow.activity.service.ParticipationService;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 내 활동 (U-13 내가 개설한 활동, U-14 내가 참여한 활동) */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final ActivityService activityService;
    private final ParticipationService participationService;

    /** U-13 내가 개설한 활동 */
    @GetMapping("/activities")
    public ApiResponse<List<ActivitySummaryResponse>> myActivities(@GuestId String guestId) {
        return ApiResponse.ok(activityService.myActivities(guestId));
    }

    /** U-14 내가 참여한 활동 */
    @GetMapping("/participations")
    public ApiResponse<List<MyParticipationResponse>> myParticipations(@GuestId String guestId) {
        return ApiResponse.ok(participationService.myParticipations(guestId));
    }
}
