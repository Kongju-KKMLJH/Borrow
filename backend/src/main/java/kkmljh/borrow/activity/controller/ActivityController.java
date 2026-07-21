package kkmljh.borrow.activity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.activity.dto.ActivityCreateRequest;
import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.RequirementUpdateRequest;
import kkmljh.borrow.activity.service.ActivityService;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 취미 모임 개설/목록/검색/상세 (U-01~U-03, U-06~U-08) */
@Tag(name = "활동 (C)", description = "취미 모임 개설·목록·검색·상세·요구조건 수정 (U-01~U-03, U-06~U-08)")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "U-06~U-08 취미 모임 개설",
            description = "취미 모임(HOBBY)을 개설한다. 개설자는 X-Guest-Id로 식별.")
    @PostMapping
    public ApiResponse<ActivityDetailResponse> create(@GuestId String guestId,
                                                      @Valid @RequestBody ActivityCreateRequest request) {
        return ApiResponse.ok(activityService.create(guestId, request));
    }

    @Operation(summary = "U-01/U-02 활동 목록·검색",
            description = "type/field 필터 + keyword 검색. X-Guest-Id가 있으면 참여 여부(alreadyJoined) 표시.")
    @GetMapping
    public ApiResponse<List<ActivitySummaryResponse>> list(
            @GuestId(required = false) String guestId,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivityField field,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(activityService.search(guestId, type, field, keyword));
    }

    @Operation(summary = "U-03 활동 상세",
            description = "활동 단건 상세. X-Guest-Id가 있으면 참여 여부(alreadyJoined) 표시.")
    @GetMapping("/{activityId}")
    public ApiResponse<ActivityDetailResponse> detail(@GuestId(required = false) String guestId,
                                                      @PathVariable Long activityId) {
        return ApiResponse.ok(activityService.detail(guestId, activityId));
    }

    @Operation(summary = "U-08 공간 요구조건 수정", description = "개설자 본인만 수정 가능.")
    @PatchMapping("/{activityId}/requirement")
    public ApiResponse<ActivityDetailResponse> updateRequirement(
            @GuestId String guestId,
            @PathVariable Long activityId,
            @Valid @RequestBody RequirementUpdateRequest request) {
        return ApiResponse.ok(activityService.updateRequirement(guestId, activityId, request));
    }
}
