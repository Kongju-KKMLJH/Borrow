package kkmljh.borrow.activity.dto;

import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.SpaceRequirement;

import java.util.HashSet;
import java.util.Set;

/**
 * 활동의 공간 요구조건 (U-08). 개설 요청·응답 양쪽에서 사용.
 * AI 자동 분석(A-01) 결과를 그대로 실어 보낼 수도 있다.
 */
public record SpaceRequirementDto(
        String region,
        int headcount,
        Set<FacilityType> requiredFacilities,
        boolean noisy,
        boolean messy
) {
    public SpaceRequirement toEntity() {
        return SpaceRequirement.builder()
                .region(region)
                .headcount(headcount)
                .requiredFacilities(requiredFacilities != null ? requiredFacilities : new HashSet<>())
                .noisy(noisy)
                .messy(messy)
                .build();
    }

    public static SpaceRequirementDto from(SpaceRequirement r) {
        if (r == null) {
            return null;
        }
        return new SpaceRequirementDto(
                r.getRegion(),
                r.getHeadcount(),
                new HashSet<>(r.getRequiredFacilities()),
                r.isNoisy(),
                r.isMessy()
        );
    }
}
