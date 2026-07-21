package kkmljh.borrow.space.controller;

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
@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;

    /** 공간 등록 */
    @PostMapping
    public ApiResponse<SpaceResponse> create(@RequestBody @Valid SpaceRequest request) {
        return ApiResponse.ok(spaceService.create(request));
    }

    /** 공간 목록 */
    @GetMapping
    public ApiResponse<List<SpaceResponse>> list() {
        return ApiResponse.ok(spaceService.findAll());
    }

    /** 공간 상세 */
    @GetMapping("/{id}")
    public ApiResponse<SpaceResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(spaceService.findById(id));
    }

    /** 공간 수정 */
    @PutMapping("/{id}")
    public ApiResponse<SpaceResponse> update(@PathVariable Long id, @RequestBody @Valid SpaceRequest request) {
        return ApiResponse.ok(spaceService.update(id, request));
    }

    /** 공간 삭제 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        spaceService.delete(id);
        return ApiResponse.ok();
    }
}
