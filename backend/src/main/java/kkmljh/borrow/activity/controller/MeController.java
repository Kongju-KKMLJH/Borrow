package kkmljh.borrow.activity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "내 활동 (C)", description = "로그인한 회원 본인이 개설/참여한 활동 (U-13, U-14)")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final ActivityService activityService;
    private final ParticipationService participationService;

    @Operation(summary = "U-13 내가 개설한 활동", description = "로그인 계정 기준 개설 활동 목록. 역할과 무관하게 로그인만 하면 호출 가능.")
    @GetMapping("/activities")
    public ApiResponse<List<ActivitySummaryResponse>> myActivities(@GuestId String guestId) {
        return ApiResponse.ok(activityService.myActivities(guestId));
    }

    @Operation(summary = "U-14 내가 참여한 활동", description = "로그인 계정 기준 참여 활동 목록. 역할과 무관하게 로그인만 하면 호출 가능.")
    @GetMapping("/participations")
    public ApiResponse<List<MyParticipationResponse>> myParticipations(@GuestId String guestId) {
        return ApiResponse.ok(participationService.myParticipations(guestId));
    }
}
