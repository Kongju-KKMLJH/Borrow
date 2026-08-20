package kkmljh.borrow.admin.dto;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.space.dto.SpaceSlotResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 관리자 공간 목록의 한 줄 (기능명세 7.3.1 display — 공간명·등록자·위치·등록일).
 *
 * <p><b>주소 전문을 담는다.</b> 공개 응답({@code SpaceResponse.from})이 동 단위까지만 주는 것은
 * 시민·예술가에게 감추기 위한 규칙이고(기능명세 6.1 rules), 관리자는 통제 조치 대상을 특정해야 하는
 * 주체다. 이 DTO는 {@code /api/admin/**}(ADMIN 전용)에서만 쓰며 <b>다른 경로로 새 나가지 않게</b> 한다.
 *
 * <p><b>목록 표시에 쓰지 않는 필드까지 담는 이유</b>(7.3.2): 관리자 콘솔의 수정 폼이 이 목록 응답을
 * 그대로 프리필 소스로 쓴다. 여기 없는 필드는 폼이 빈 값으로 시작하고, 수정 요청이 전체 교체라
 * <b>저장하는 순간 원본이 지워진다</b>. 특히 슬롯이 날아가면 AI 매칭 후보에서 빠진다.
 */
public record AdminSpaceResponse(
        Long id,
        String name,
        String ownerId,
        String region,
        String address,
        int capacity,
        int hourlyFee,
        LocalDateTime createdAt,
        List<String> imageUrls,
        String conditions,
        Set<FacilityType> facilities,
        Set<ActivityField> allowedFields,
        boolean noiseAllowed,
        boolean messAllowed,
        List<SpaceSlotResponse> slots
) {
    /** 슬롯을 별도로 조회한 경우 (수정 폼 프리필). */
    public static AdminSpaceResponse of(Space space, List<SpaceSlot> slots) {
        return new AdminSpaceResponse(
                space.getId(),
                space.getName(),
                space.getOwnerId(),
                space.getRegion(),
                space.getAddress(),
                space.getCapacity(),
                space.getHourlyFee(),
                space.getCreatedAt(),
                List.copyOf(space.getImageUrls()),
                space.getConditions(),
                Set.copyOf(space.getFacilities()),
                Set.copyOf(space.getAllowedFields()),
                space.isNoiseAllowed(),
                space.isMessAllowed(),
                slots.stream().map(SpaceSlotResponse::from).toList());
    }

    public static AdminSpaceResponse from(Space space) {
        return of(space, List.of());
    }
}
