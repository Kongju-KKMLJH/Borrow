package kkmljh.borrow.activity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.activity.dto.ActivityCreateRequest;
import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.ActivityUpdateRequest;
import kkmljh.borrow.activity.dto.RequirementUpdateRequest;
import kkmljh.borrow.activity.service.ActivityService;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 활동 개설/수정/삭제/목록/검색/상세 (U-01~U-03, U-06~U-08, 기능명세 2.1) */
@Tag(name = "활동 (C)", description = "활동 개설·수정·삭제·목록·검색·상세·요구조건 수정 (U-01~U-03, U-06~U-08)")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "U-06~U-08 취미 모임 개설",
            description = "활동을 개설한다. MEMBER는 HOBBY, ARTIST는 CLASS(인증 배지 포함)로 서버가 분기한다.")
    @PostMapping
    public ApiResponse<ActivityDetailResponse> create(@GuestId String guestId,
                                                      @Valid @RequestBody ActivityCreateRequest request) {
        return ApiResponse.ok(activityService.create(guestId, request));
    }

    @Operation(summary = "U-01/U-02 활동 목록·검색",
            description = "type/field/region/dateFrom/dateTo 필터 + keyword 검색 (기능명세 4.1 지역·일정 탐색). "
                    + "파라미터를 비우면 그 조건은 무시한다. region은 승인된 개최지(Space.region) 부분일치. "
                    + "비로그인 열람 가능하며, 로그인 상태면 참여 여부(alreadyJoined) 표시.")
    @GetMapping
    public ApiResponse<List<ActivitySummaryResponse>> list(
            @GuestId(required = false) String guestId,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivityField field,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ApiResponse.ok(activityService.search(guestId, type, field, keyword, region, dateFrom, dateTo));
    }

    @Operation(summary = "U-03 활동 상세",
            description = "활동 단건 상세. 비로그인 열람 가능하며, 로그인 상태면 참여 여부(alreadyJoined) 표시.")
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

    @Operation(summary = "활동 수정",
            description = "개설자 본인만 수정 가능. 시민 공개 전(DRAFT/REJECTED) 단계에서만 허용한다. "
                    + "공간 요구조건은 PATCH /api/activities/{activityId}/requirement 가 담당한다.")
    @PutMapping("/{activityId}")
    public ApiResponse<ActivityDetailResponse> update(@GuestId String guestId,
                                                      @PathVariable Long activityId,
                                                      @Valid @RequestBody ActivityUpdateRequest request) {
        return ApiResponse.ok(activityService.update(guestId, activityId, request));
    }

    @Operation(summary = "활동 삭제",
            description = "개설자 본인만 삭제 가능. 시민 공개 전(DRAFT/REJECTED) 단계에서만 허용하며, "
                    + "참여 신청이 남아 있으면 거부한다.")
    @DeleteMapping("/{activityId}")
    public ApiResponse<Void> delete(@GuestId String guestId, @PathVariable Long activityId) {
        activityService.delete(guestId, activityId);
        return ApiResponse.ok();
    }
}
