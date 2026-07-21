package kkmljh.borrow.ai.dto;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;

import java.util.List;
import java.util.Set;

/**
 * A-03 매칭 결과 1건. 적합도 점수(score, 0~100) 내림차순으로 정렬되어 반환된다.
 *
 * @param aiScored true면 Claude 점수, false면 API 실패로 규칙 기반 폴백 점수
 */
public record SpaceMatchResponse(
        Long spaceId,
        String name,
        String region,
        int capacity,
        int hourlyFee,
        List<String> imageUrls,
        Set<FacilityType> facilities,
        Set<ActivityField> allowedFields,
        int score,
        String reason,
        boolean aiScored
) {
    public static SpaceMatchResponse of(kkmljh.borrow.domain.Space s, int score, String reason, boolean aiScored) {
        return new SpaceMatchResponse(
                s.getId(), s.getName(), s.getRegion(), s.getCapacity(), s.getHourlyFee(),
                s.getImageUrls(), s.getFacilities(), s.getAllowedFields(),
                score, reason, aiScored);
    }

    public static List<SpaceMatchResponse> emptyList() {
        return List.of();
    }
}