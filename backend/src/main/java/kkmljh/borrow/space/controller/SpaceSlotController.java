package kkmljh.borrow.space.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.response.ApiResponse;
import kkmljh.borrow.space.dto.SpaceSlotRequest;
import kkmljh.borrow.space.dto.SpaceSlotResponse;
import kkmljh.borrow.space.service.SpaceSlotService;
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

/** 공간 유휴시간 관리 (B-05) */
@Tag(name = "공간 유휴시간 (B)", description = "공간별 대여 가능한 유휴시간 슬롯 관리 (B-05)")
@RestController
@RequestMapping("/api/spaces/{spaceId}/slots")
@RequiredArgsConstructor
public class SpaceSlotController {

    private final SpaceSlotService spaceSlotService;

    @Operation(summary = "유휴시간 목록", description = "특정 공간의 유휴시간 슬롯 목록.")
    @GetMapping
    public ApiResponse<List<SpaceSlotResponse>> list(@PathVariable Long spaceId) {
        return ApiResponse.ok(spaceSlotService.findBySpace(spaceId));
    }

    @Operation(summary = "유휴시간 추가", description = "공간에 대여 가능한 유휴시간 슬롯을 추가한다. 소유자 본인만 가능.")
    @PostMapping
    public ApiResponse<SpaceSlotResponse> add(@GuestId String ownerId,
                                              @PathVariable Long spaceId,
                                              @RequestBody @Valid SpaceSlotRequest request) {
        return ApiResponse.ok(spaceSlotService.add(ownerId, spaceId, request));
    }

    @Operation(summary = "유휴시간 수정", description = "공간의 유휴시간 슬롯의 요일·시작/종료 시각을 수정한다. 소유자 본인만 가능.")
    @PutMapping("/{slotId}")
    public ApiResponse<SpaceSlotResponse> update(@GuestId String ownerId,
                                                 @PathVariable Long spaceId,
                                                 @PathVariable Long slotId,
                                                 @RequestBody @Valid SpaceSlotRequest request) {
        return ApiResponse.ok(spaceSlotService.update(ownerId, spaceId, slotId, request));
    }

    @Operation(summary = "유휴시간 삭제", description = "공간의 유휴시간 슬롯을 삭제한다. 소유자 본인만 가능.")
    @DeleteMapping("/{slotId}")
    public ApiResponse<Void> delete(@GuestId String ownerId,
                                    @PathVariable Long spaceId,
                                    @PathVariable Long slotId) {
        spaceSlotService.delete(ownerId, spaceId, slotId);
        return ApiResponse.ok();
    }
}
