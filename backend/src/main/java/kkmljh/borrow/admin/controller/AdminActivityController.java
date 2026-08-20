package kkmljh.borrow.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.admin.dto.AdminActivityRequest;
import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.service.AdminActivityService;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 콘솔 — 프로그램 관리 (기능명세 7.2) */
@Tag(name = "관리자 콘솔 — 프로그램", description = "전체 프로그램 목록과 생성·수정·삭제 (기능명세 7.2)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    @Operation(summary = "7.2.1 전체 프로그램 목록 조회",
            description = "상태(DRAFT~PUBLISHED)를 가리지 않는 관리용 목록.")
    @GetMapping("/activities")
    public ApiResponse<List<AdminActivityResponse>> activities() {
        return ApiResponse.ok(adminActivityService.findAll());
    }

    @Operation(summary = "7.2.2 프로그램 생성",
            description = "담당 예술가·공간·일정·상태를 지정해 실제 프로그램을 만든다. "
                    + "유형과 인증 배지는 담당 계정의 역할에서 서버가 정한다.")
    @PostMapping("/activities")
    public ApiResponse<AdminActivityResponse> createActivity(@Valid @RequestBody AdminActivityRequest req) {
        return ApiResponse.ok(adminActivityService.create(req));
    }

    @Operation(summary = "7.2.2 프로그램 수정",
            description = "모든 실제 프로그램을 수정할 수 있다. 기존 개최 요청은 지우고 요청한 상태에 맞게 다시 만들므로 "
                    + "파트너의 승인·거절 이력은 남지 않는다.")
    @PutMapping("/activities/{activityId}")
    public ApiResponse<AdminActivityResponse> updateActivity(@PathVariable Long activityId,
                                                             @Valid @RequestBody AdminActivityRequest req) {
        return ApiResponse.ok(adminActivityService.update(activityId, req));
    }

    @Operation(summary = "7.2.2 프로그램 삭제",
            description = "프로그램 행을 실제로 지운다. 이 프로그램의 참여 신청과 개최 요청이 함께 삭제된다.")
    @DeleteMapping("/activities/{activityId}")
    public ApiResponse<Void> deleteActivity(@PathVariable Long activityId) {
        adminActivityService.delete(activityId);
        return ApiResponse.ok();
    }
}
