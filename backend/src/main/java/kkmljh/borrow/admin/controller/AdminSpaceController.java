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
@Tag(name = "관리자 콘솔 — 공간", description = "전체 공간 목록과 생성·수정·삭제 (기능명세 7.3)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSpaceController {

    private final AdminSpaceService adminSpaceService;

    @Operation(summary = "7.3.1 전체 공간 목록 조회",
            description = "등록된 모든 공간의 관리용 목록. 관리 대상 특정을 위해 주소 전문을 포함한다.")
    @GetMapping("/spaces")
    public ApiResponse<List<AdminSpaceResponse>> spaces() {
        return ApiResponse.ok(adminSpaceService.findAll());
    }

    @Operation(summary = "7.3.2 공간 생성",
            description = "등록자를 지정해 실제 공간을 만든다. 이용 가능 시간을 함께 넣어야 AI 추천 후보에 걸린다.")
    @PostMapping("/spaces")
    public ApiResponse<AdminSpaceResponse> createSpace(@Valid @RequestBody AdminSpaceRequest req) {
        return ApiResponse.ok(adminSpaceService.create(req));
    }

    @Operation(summary = "7.3.2 공간 수정",
            description = "모든 실제 공간을 수정할 수 있다. 이용 가능 시간은 전체 교체된다.")
    @PutMapping("/spaces/{spaceId}")
    public ApiResponse<AdminSpaceResponse> updateSpace(@PathVariable Long spaceId,
                                                       @Valid @RequestBody AdminSpaceRequest req) {
        return ApiResponse.ok(adminSpaceService.update(spaceId, req));
    }

    @Operation(summary = "7.3.2 공간 삭제",
            description = "공간 행을 실제로 지운다. 이용 시간과 개최 요청이 함께 삭제되고, "
                    + "이 공간에서 승인됐던 프로그램은 거절 상태로 되돌아가 다른 공간에 재요청할 수 있다. "
                    + "프로그램 자체와 그 참여 신청은 지우지 않는다.")
    @DeleteMapping("/spaces/{spaceId}")
    public ApiResponse<Void> deleteSpace(@PathVariable Long spaceId) {
        adminSpaceService.delete(spaceId);
        return ApiResponse.ok();
    }
}
