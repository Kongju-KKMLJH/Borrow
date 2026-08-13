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
        Integer headcount,
        Set<FacilityType> requiredFacilities,
        Boolean noisy,
        Boolean messy
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
        // 요구조건 없이 개설한 활동은 값이 전부 null인 인스턴스로 되살아난다 → null로 정규화
        if (r == null || r.isEmpty()) {
            return null;
        }
        return new SpaceRequirementDto(
                r.getRegion(),
                r.getHeadcount(),
                new HashSet<>(r.getRequiredFacilities()),
                r.getNoisy(),
                r.getMessy()
        );
    }
}
