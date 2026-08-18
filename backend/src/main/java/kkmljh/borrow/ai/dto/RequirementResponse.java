package kkmljh.borrow.ai.dto;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;

import java.util.List;

/**
 * A-01 분석 결과. 프론트는 이 값으로 U-08 요구조건 입력을 자동 채우고,
 * 그대로 A-02/A-03 매칭 요청에 사용할 수 있다.
 * (활동 저장은 C 담당 — A는 분석 결과만 반환하고 Activity를 건드리지 않는다.)
 *
 * @param missingFields      기능명세 2.2.1 — 비어 있어 정확한 추천이 어려운 항목의 필드명.
 *                           모두 채워졌으면 빈 목록
 * @param followUpQuestions  {@code missingFields}에 대응하는 보완 질문(한국어). 빈 목록일 수 있다
 */
public record RequirementResponse(
        String region,
        int headcount,
        List<FacilityType> requiredFacilities,
        boolean noisy,
        boolean messy,
        ActivityField field,
        List<String> missingFields,
        List<String> followUpQuestions
) {
}