package kkmljh.borrow.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.service.AdminActivityService;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 콘솔 — 프로그램 관리 (기능명세 7.2) */
@Tag(name = "관리자 콘솔 — 프로그램", description = "전체 프로그램 목록과 강제 삭제 (기능명세 7.2)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    @Operation(summary = "7.2.1 전체 프로그램 목록 조회",
            description = "상태(DRAFT~PUBLISHED)와 강제 삭제 여부를 가리지 않는 관리용 목록.")
    @GetMapping("/activities")
    public ApiResponse<List<AdminActivityResponse>> activities() {
        return ApiResponse.ok(adminActivityService.findAll());
    }

    @Operation(summary = "7.2.3 프로그램 강제 삭제",
            description = "시민 탐색·상세·참여 신청에서 제외한다. 기존 참여 신청 내역은 보존한다.")
    @PostMapping("/activities/{activityId}/force-delete")
    public ApiResponse<AdminActivityResponse> forceDelete(@PathVariable Long activityId) {
        return ApiResponse.ok(adminActivityService.forceDelete(activityId));
    }
}
