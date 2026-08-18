package kkmljh.borrow.ai.service;

import kkmljh.borrow.domain.FacilityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RequirementGuide — 기능명세 2.2.1 운영 조건 보완 질문")
class RequirementGuideTest {

    @Test
    @DisplayName("모두 채워져 있으면 빠진 항목도 질문도 없다")
    void nothingMissing() {
        List<String> missing = RequirementGuide.missingFields(
                "천안시 서북구 불당동", 6, List.of(FacilityType.WATER));

        assertThat(missing).isEmpty();
        assertThat(RequirementGuide.questionsFor(missing)).isEmpty();
    }

    @Test
    @DisplayName("지역이 비면 region 을 짚는다")
    void blankRegion() {
        assertThat(RequirementGuide.missingFields("   ", 6, List.of(FacilityType.WATER)))
                .containsExactly(RequirementGuide.REGION);
    }

    @Test
    @DisplayName("지역이 null 이어도 region 을 짚는다")
    void nullRegion() {
        assertThat(RequirementGuide.missingFields(null, 6, List.of(FacilityType.WATER)))
                .containsExactly(RequirementGuide.REGION);
    }

    @Test
    @DisplayName("인원이 0 이하면 headcount 를 짚는다")
    void nonPositiveHeadcount() {
        assertThat(RequirementGuide.missingFields("천안시", 0, List.of(FacilityType.WATER)))
                .containsExactly(RequirementGuide.HEADCOUNT);
        assertThat(RequirementGuide.missingFields("천안시", -3, List.of(FacilityType.WATER)))
                .containsExactly(RequirementGuide.HEADCOUNT);
    }

    @Test
    @DisplayName("인원 1명은 빠진 것으로 보지 않는다 (경계)")
    void oneIsEnough() {
        assertThat(RequirementGuide.missingFields("천안시", 1, List.of(FacilityType.WATER))).isEmpty();
    }

    @Test
    @DisplayName("필요 시설이 비거나 null 이면 requiredFacilities 를 짚는다")
    void emptyFacilities() {
        assertThat(RequirementGuide.missingFields("천안시", 6, List.of()))
                .containsExactly(RequirementGuide.REQUIRED_FACILITIES);
        assertThat(RequirementGuide.missingFields("천안시", 6, null))
                .containsExactly(RequirementGuide.REQUIRED_FACILITIES);
    }

    @Test
    @DisplayName("여러 항목이 비면 정해진 순서로 모두 짚는다")
    void allMissingInOrder() {
        assertThat(RequirementGuide.missingFields("", 0, List.of()))
                .containsExactly(RequirementGuide.REGION, RequirementGuide.HEADCOUNT,
                        RequirementGuide.REQUIRED_FACILITIES);
    }

    @Test
    @DisplayName("빠진 항목마다 질문이 하나씩 나온다")
    void questionsMatchMissingFields() {
        List<String> missing = RequirementGuide.missingFields("", 0, List.of());
        List<String> questions = RequirementGuide.questionsFor(missing);

        assertThat(questions).hasSameSizeAs(missing);
        assertThat(questions.get(0)).contains("지역");
        assertThat(questions.get(1)).contains("몇 명");
        assertThat(questions.get(2)).contains("시설");
    }

    @Test
    @DisplayName("질문 문구를 모르는 항목은 조용히 건너뛴다 — 답을 받아도 채울 자리가 없는 것은 묻지 않는다")
    void unknownFieldIsSkipped() {
        assertThat(RequirementGuide.questionsFor(List.of("durationMinutes"))).isEmpty();
    }

    @Test
    @DisplayName("빠진 항목이 없으면 질문도 없다")
    void noQuestionsWhenNothingMissing() {
        assertThat(RequirementGuide.questionsFor(List.of())).isEmpty();
    }
}
