package kkmljh.borrow.ai.dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * A-03에서 Claude가 채우는 구조화 출력 스키마.
 * 후보 공간별 적합도 점수와 추천 이유를 산출한다.
 */
@JsonClassDescription("후보 공간별 적합도 점수 목록")
public record SpaceScores(
        @JsonPropertyDescription("입력으로 준 모든 후보 공간에 대한 점수. 각 spaceId마다 하나씩")
        List<SpaceScore> scores
) {
    @JsonClassDescription("공간 한 곳의 적합도 평가")
    public record SpaceScore(
            @JsonPropertyDescription("평가 대상 공간의 spaceId")
            long spaceId,
            @JsonPropertyDescription("적합도 점수 0~100. 요구조건 충족도가 높을수록 높게")
            int score,
            @JsonPropertyDescription("추천/비추천 이유 한 문장(한국어). 시설·인원·가격·분야 관점에서 구체적으로")
            String reason
    ) {
    }
}