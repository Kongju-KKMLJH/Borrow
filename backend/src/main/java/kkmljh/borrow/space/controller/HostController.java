package kkmljh.borrow.space.controller;

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
@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostController {

    private final HostService hostService;

    /** 운영 현황 조회 (B-01) */
    @GetMapping("/home")
    public ApiResponse<HostHomeResponse> home() {
        return ApiResponse.ok(hostService.getHome());
    }

    /** 확정 일정 조회 (B-11) */
    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleResponse>> schedules() {
        return ApiResponse.ok(hostService.getConfirmedSchedules());
    }
}
