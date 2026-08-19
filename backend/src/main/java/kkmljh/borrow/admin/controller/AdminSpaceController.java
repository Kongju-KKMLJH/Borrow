package kkmljh.borrow.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.service.AdminSpaceService;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 콘솔 — 공간 관리 (기능명세 7.3) */
@Tag(name = "관리자 콘솔 — 공간", description = "전체 공간 목록과 강제 삭제 (기능명세 7.3)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSpaceController {

    private final AdminSpaceService adminSpaceService;

    @Operation(summary = "7.3.1 전체 공간 목록 조회",
            description = "강제 삭제된 공간까지 포함한 관리용 목록. 관리 대상 특정을 위해 주소 전문을 포함한다.")
    @GetMapping("/spaces")
    public ApiResponse<List<AdminSpaceResponse>> spaces() {
        return ApiResponse.ok(adminSpaceService.findAll());
    }

    @Operation(summary = "7.3.3 공간 강제 삭제",
            description = "AI 추천·개최 요청 대상에서 제외하고, 진행 중인 개최 요청은 모두 자동 거절한다.")
    @PostMapping("/spaces/{spaceId}/force-delete")
    public ApiResponse<AdminSpaceResponse> forceDelete(@PathVariable Long spaceId) {
        return ApiResponse.ok(adminSpaceService.forceDelete(spaceId));
    }
}
