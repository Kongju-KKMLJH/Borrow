package kkmljh.borrow.ai.dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;

import java.util.List;

/**
 * A-01에서 Claude가 채우는 구조화 출력 스키마(structured output).
 * 활동 설명 텍스트를 읽고 공간 요구조건으로 변환한다.
 */
@JsonClassDescription("취미 모임 설명에서 추출한 공간 요구조건")
public record AnalyzedRequirement(
        @JsonPropertyDescription("활동 분야. 그림 그리기/드로잉/미술이면 ART, 사진/영상 촬영이면 PHOTO")
        ActivityField field,

        @JsonPropertyDescription("필요한 최소 수용 인원(주최자 포함 예상 참여 인원). 언급이 없으면 합리적으로 추정")
        int headcount,

        @JsonPropertyDescription("활동에 필요한 시설 목록. 확실히 필요한 것만 포함")
        List<FacilityType> requiredFacilities,

        @JsonPropertyDescription("소음이 발생하는 활동인지 여부(악기/큰 대화/장비 소리 등)")
        boolean noisy,

        @JsonPropertyDescription("오염이 발생하는 활동인지 여부(물감/흙/음식물 등으로 바닥·집기가 더러워질 수 있음)")
        boolean messy
) {
}