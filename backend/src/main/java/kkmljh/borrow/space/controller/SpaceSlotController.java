package kkmljh.borrow.space.controller;

import jakarta.validation.Valid;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.space.dto.SpaceSlotRequest;
import kkmljh.borrow.space.dto.SpaceSlotResponse;
import kkmljh.borrow.space.service.SpaceSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 공간 유휴시간 관리 (B-05) */
@RestController
@RequestMapping("/api/spaces/{spaceId}/slots")
@RequiredArgsConstructor
public class SpaceSlotController {

    private final SpaceSlotService spaceSlotService;

    /** 유휴시간 목록 */
    @GetMapping
    public ApiResponse<List<SpaceSlotResponse>> list(@PathVariable Long spaceId) {
        return ApiResponse.ok(spaceSlotService.findBySpace(spaceId));
    }

    /** 유휴시간 추가 */
    @PostMapping
    public ApiResponse<SpaceSlotResponse> add(@PathVariable Long spaceId,
                                              @RequestBody @Valid SpaceSlotRequest request) {
        return ApiResponse.ok(spaceSlotService.add(spaceId, request));
    }

    /** 유휴시간 삭제 */
    @DeleteMapping("/{slotId}")
    public ApiResponse<Void> delete(@PathVariable Long spaceId, @PathVariable Long slotId) {
        spaceSlotService.delete(spaceId, slotId);
        return ApiResponse.ok();
    }
}
