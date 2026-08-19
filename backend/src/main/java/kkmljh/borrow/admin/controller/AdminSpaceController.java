package kkmljh.borrow.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.admin.dto.AdminSpaceRequest;
import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.service.AdminSpaceService;
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

    @Operation(summary = "7.3.2 임시(mock) 공간 생성",
            description = "등록자를 지정해 만든다. 이용 가능 시간을 함께 넣어야 AI 추천 후보에 걸린다.")
    @PostMapping("/spaces")
    public ApiResponse<AdminSpaceResponse> createSpace(@Valid @RequestBody AdminSpaceRequest req) {
        return ApiResponse.ok(adminSpaceService.create(req));
    }

    @Operation(summary = "7.3.2 임시 공간 수정",
            description = "임시 공간만 수정할 수 있다. 이용 가능 시간은 전체 교체된다.")
    @PutMapping("/spaces/{spaceId}")
    public ApiResponse<AdminSpaceResponse> updateSpace(@PathVariable Long spaceId,
                                                       @Valid @RequestBody AdminSpaceRequest req) {
        return ApiResponse.ok(adminSpaceService.update(spaceId, req));
    }

    @Operation(summary = "7.3.2 임시 공간 삭제",
            description = "임시 공간만 삭제할 수 있다. 개최 요청이 걸려 있으면 거절한다.")
    @DeleteMapping("/spaces/{spaceId}")
    public ApiResponse<Void> deleteSpace(@PathVariable Long spaceId) {
        adminSpaceService.delete(spaceId);
        return ApiResponse.ok();
    }

    @Operation(summary = "7.3.3 공간 강제 삭제",
            description = "AI 추천·개최 요청 대상에서 제외하고, 진행 중인 개최 요청은 모두 자동 거절한다.")
    @PostMapping("/spaces/{spaceId}/force-delete")
    public ApiResponse<AdminSpaceResponse> forceDelete(@PathVariable Long spaceId) {
        return ApiResponse.ok(adminSpaceService.forceDelete(spaceId));
    }
}
