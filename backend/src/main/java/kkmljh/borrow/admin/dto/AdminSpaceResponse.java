package kkmljh.borrow.admin.dto;

import kkmljh.borrow.domain.Space;

import java.time.LocalDateTime;

/**
 * 관리자 공간 목록의 한 줄 (기능명세 7.3.1 display — 공간명·등록자·위치·등록일).
 *
 * <p><b>주소 전문을 담는다.</b> 공개 응답({@code SpaceResponse.from})이 동 단위까지만 주는 것은
 * 시민·예술가에게 감추기 위한 규칙이고(기능명세 6.1 rules), 관리자는 통제 조치 대상을 특정해야 하는
 * 주체다. 이 DTO는 {@code /api/admin/**}(ADMIN 전용)에서만 쓰며 <b>다른 경로로 새 나가지 않게</b> 한다.
 */
public record AdminSpaceResponse(
        Long id,
        String name,
        String ownerId,
        String region,
        String address,
        int capacity,
        int hourlyFee,
        LocalDateTime createdAt
) {
    public static AdminSpaceResponse from(Space space) {
        return new AdminSpaceResponse(
                space.getId(),
                space.getName(),
                space.getOwnerId(),
                space.getRegion(),
                space.getAddress(),
                space.getCapacity(),
                space.getHourlyFee(),
                space.getCreatedAt());
    }
}
