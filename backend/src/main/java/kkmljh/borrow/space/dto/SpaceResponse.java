package kkmljh.borrow.space.dto;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;

import java.util.List;
import java.util.Set;

/** 공간 상세/목록 응답 */
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
    public static SpaceResponse from(Space space) {
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
