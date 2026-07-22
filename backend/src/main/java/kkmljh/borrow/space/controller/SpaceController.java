package kkmljh.borrow.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.space.dto.SpaceRequest;
import kkmljh.borrow.space.dto.SpaceResponse;
import kkmljh.borrow.space.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 공간 CRUD (B-02~B-06) */
@Tag(name = "공간 (B)", description = "공간 등록·조회·수정·삭제 (B-02~B-06)")
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    @Operation(summary = "B-02 공간 등록", description = "새 공간을 등록한다.")
    @PostMapping
    public ApiResponse<SpaceResponse> create(@RequestBody @Valid SpaceRequest request) {
        return ApiResponse.ok(spaceService.create(request));
    }

    @Operation(summary = "B-03 공간 목록", description = "등록된 공간 전체 목록.")
    @GetMapping
    public ApiResponse<List<SpaceResponse>> list() {
        return ApiResponse.ok(spaceService.findAll());
    }

    @Operation(summary = "B-04 공간 상세", description = "공간 단건 상세 조회.")
    @GetMapping("/{id}")
    public ApiResponse<SpaceResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(spaceService.findById(id));
    }

    @Operation(summary = "B-05 공간 수정", description = "공간 정보를 수정한다.")
    @PutMapping("/{id}")
    public ApiResponse<SpaceResponse> update(@PathVariable Long id, @RequestBody @Valid SpaceRequest request) {
        return ApiResponse.ok(spaceService.update(id, request));
    }

    @Operation(summary = "B-06 공간 삭제", description = "공간을 삭제한다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        spaceService.delete(id);
        return ApiResponse.ok();
    }
}
