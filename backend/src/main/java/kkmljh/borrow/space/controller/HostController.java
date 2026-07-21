package kkmljh.borrow.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.space.dto.HostHomeResponse;
import kkmljh.borrow.space.dto.ScheduleResponse;
import kkmljh.borrow.space.service.HostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 사업자 홈(B-01) · 확정 일정(B-11) */
@Tag(name = "사업자 홈 (B)", description = "공간 제공자 운영 현황 및 확정 일정 (B-01, B-11)")
@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostController {

    private final HostService hostService;

    @Operation(summary = "B-01 운영 현황 조회", description = "사업자 홈 대시보드 요약.")
    @GetMapping("/home")
    public ApiResponse<HostHomeResponse> home() {
        return ApiResponse.ok(hostService.getHome());
    }

    @Operation(summary = "B-11 확정 일정 조회", description = "승인되어 확정된 개최 일정 목록.")
    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleResponse>> schedules() {
        return ApiResponse.ok(hostService.getConfirmedSchedules());
    }
}
