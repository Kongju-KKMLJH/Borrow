package kkmljh.borrow.space.dto;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;

import java.util.List;
import java.util.Set;

/**
 * 공간 상세/목록 응답.
 * <p>공개 응답은 {@link #from}, 소유 HOST 본인 응답은 {@link #forOwner} 를 쓴다 —
 * 주소 전문을 흘리는 구멍이 하나가 되게 팩토리로 가른다 (기능명세 6.1 rules).
 */
public record SpaceResponse(
        Long id,
        String name,
        String region,
        String address,
        List<String> imageUrls,
        int capacity,
        int hourlyFee,
        String conditions,
        Set<FacilityType> facilities,
        Set<ActivityField> allowedFields,
        boolean noiseAllowed,
        boolean messAllowed
) {
    /**
     * 공개용 — 시민·예술가에게는 동 단위({@code region})까지만 (기능명세 6.1 rules).
     * <p>주소 문자열을 잘라 쓰지 않는다. {@code region} 이 이미 동 단위 값이라 {@code address} 를 빼면 그만이다.
     */
    public static SpaceResponse from(Space space) {
        return new SpaceResponse(
                space.getId(),
                space.getName(),
                space.getRegion(),
                null,
                space.getImageUrls(),
                space.getCapacity(),
                space.getHourlyFee(),
                space.getConditions(),
                space.getFacilities(),
                space.getAllowedFields(),
                space.isNoiseAllowed(),
                space.isMessAllowed()
        );
    }

    /** 소유자용 — 본인 공간이므로 주소 전문을 담는다. */
    public static SpaceResponse forOwner(Space space) {
        return new SpaceResponse(
                space.getId(),
                space.getName(),
                space.getRegion(),
                space.getAddress(),
                space.getImageUrls(),
                space.getCapacity(),
                space.getHourlyFee(),
                space.getConditions(),
                space.getFacilities(),
                space.getAllowedFields(),
                space.isNoiseAllowed(),
                space.isMessAllowed()
        );
    }
}
