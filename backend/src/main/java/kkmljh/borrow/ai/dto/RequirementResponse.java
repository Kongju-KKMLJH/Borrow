package kkmljh.borrow.ai.dto;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;

import java.util.List;

/**
 * A-01 분석 결과. 프론트는 이 값으로 U-08 요구조건 입력을 자동 채우고,
 * 그대로 A-02/A-03 매칭 요청에 사용할 수 있다.
 * (활동 저장은 C 담당 — A는 분석 결과만 반환하고 Activity를 건드리지 않는다.)
 */
public record RequirementResponse(
        String region,
        int headcount,
        List<FacilityType> requiredFacilities,
        boolean noisy,
        boolean messy,
        ActivityField field
) {
}