package kkmljh.borrow.ai.service;

import kkmljh.borrow.ai.dto.AnalyzeRequest;
import kkmljh.borrow.ai.dto.AnalyzedRequirement;
import kkmljh.borrow.ai.dto.RequirementResponse;
import kkmljh.borrow.ai.llm.LlmClient;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityAnalysisService — A-01 활동 설명 분석")
class ActivityAnalysisServiceTest {

    @Mock
    private LlmClient llm;

    @InjectMocks
    private ActivityAnalysisService analysisService;

    private AnalyzedRequirement analyzed(ActivityField field, int headcount,
                                         List<FacilityType> facilities, boolean noisy, boolean messy) {
        return new AnalyzedRequirement(field, headcount, facilities, noisy, messy);
    }

    @Test
    @DisplayName("LLM 결과를 요구조건 응답으로 변환한다")
    void analyze() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 6, List.of(FacilityType.WATER), false, true));

        RequirementResponse response =
                analysisService.analyze(new AnalyzeRequest("수채화를 그리는 모임입니다.", "천안시 서북구"));

        assertThat(response.field()).isEqualTo(ActivityField.ART);
        assertThat(response.headcount()).isEqualTo(6);
        assertThat(response.requiredFacilities()).containsExactly(FacilityType.WATER);
        assertThat(response.noisy()).isFalse();
        assertThat(response.messy()).isTrue();
        assertThat(response.region()).isEqualTo("천안시 서북구");
    }

    @Test
    @DisplayName("활동 설명이 프롬프트에 실려 나간다")
    void promptContainsDescription() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 4, List.of(), false, false));

        analysisService.analyze(new AnalyzeRequest("수채화를 그리는 모임입니다.", null));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(llm).complete(prompt.capture(), eq(AnalyzedRequirement.class), anyLong());
        assertThat(prompt.getValue()).contains("수채화를 그리는 모임입니다.");
    }

    @Test
    @DisplayName("지역은 LLM이 아니라 요청 값을 그대로 쓰고 공백을 정리한다")
    void regionComesFromRequest() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.PHOTO, 4, List.of(), false, false));

        assertThat(analysisService.analyze(new AnalyzeRequest("설명", "  천안시 동남구  ")).region())
                .isEqualTo("천안시 동남구");
    }

    @Test
    @DisplayName("지역이 없으면 빈 문자열 — 매칭에서 '지역 무관'으로 쓰인다")
    void regionDefaultsToEmpty() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.PHOTO, 4, List.of(), false, false));

        assertThat(analysisService.analyze(new AnalyzeRequest("설명", null)).region()).isEmpty();
    }

    @Test
    @DisplayName("인원이 0 이하로 오면 최소 1명으로 보정한다")
    void headcountIsAtLeastOne() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 0, List.of(), false, false));

        assertThat(analysisService.analyze(new AnalyzeRequest("설명", null)).headcount()).isEqualTo(1);
    }

    @Test
    @DisplayName("필요 시설이 null 이면 빈 목록으로 바꾼다")
    void facilitiesDefaultToEmpty() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 4, null, false, false));

        assertThat(analysisService.analyze(new AnalyzeRequest("설명", null)).requiredFacilities())
                .isNotNull().isEmpty();
    }

    @Test
    @DisplayName("LLM이 결과를 못 내면 AI_ANALYSIS_FAILED")
    void nullResult() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong())).willReturn(null);

        assertThatThrownBy(() -> analysisService.analyze(new AnalyzeRequest("설명", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_ANALYSIS_FAILED);
    }

    @Test
    @DisplayName("API 호출이 실패해도 내부 예외를 노출하지 않고 AI_ANALYSIS_FAILED 로 감싼다")
    void apiFailure() {
        given(llm.complete(anyString(), any(), anyLong()))
                .willThrow(new RuntimeException("401 Unauthorized: invalid api key"));

        assertThatThrownBy(() -> analysisService.analyze(new AnalyzeRequest("설명", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.AI_ANALYSIS_FAILED.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_ANALYSIS_FAILED);
    }

    @Test
    @DisplayName("기능명세 2.2.1 — 다 채워졌으면 보완 질문이 없다")
    void noFollowUpWhenComplete() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 6, List.of(FacilityType.WATER), false, true));

        RequirementResponse response =
                analysisService.analyze(new AnalyzeRequest("수채화 모임", "천안시 서북구"));

        assertThat(response.missingFields()).isEmpty();
        assertThat(response.followUpQuestions()).isEmpty();
    }

    @Test
    @DisplayName("기능명세 2.2.1 — 지역이 없으면 무엇이 비었는지와 보완 질문을 함께 준다")
    void asksForMissingRegion() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 6, List.of(FacilityType.WATER), false, true));

        RequirementResponse response = analysisService.analyze(new AnalyzeRequest("수채화 모임", null));

        assertThat(response.missingFields()).containsExactly(RequirementGuide.REGION);
        assertThat(response.followUpQuestions()).singleElement()
                .satisfies(question -> assertThat(question).contains("지역"));
    }

    @Test
    @DisplayName("필요 시설이 비면 시설을 물어본다")
    void asksForMissingFacilities() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 6, List.of(), false, false));

        RequirementResponse response =
                analysisService.analyze(new AnalyzeRequest("수채화 모임", "천안시 서북구"));

        assertThat(response.missingFields()).containsExactly(RequirementGuide.REQUIRED_FACILITIES);
        assertThat(response.followUpQuestions()).singleElement()
                .satisfies(question -> assertThat(question).contains("시설"));
    }

    @Test
    @DisplayName("인원 판정은 1 이상으로 보정하기 <b>전</b> 원본값으로 한다 — 보정 후 값으로 보면 영영 못 짚는다")
    void headcountJudgedBeforeClamping() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 0, List.of(FacilityType.WATER), false, false));

        RequirementResponse response =
                analysisService.analyze(new AnalyzeRequest("수채화 모임", "천안시 서북구"));

        assertThat(response.headcount()).isEqualTo(1);              // 응답 값은 보정된다
        assertThat(response.missingFields()).containsExactly(RequirementGuide.HEADCOUNT);
        assertThat(response.followUpQuestions()).singleElement()
                .satisfies(question -> assertThat(question).contains("몇 명"));
    }

    @Test
    @DisplayName("보완 질문 때문에 LLM을 다시 부르지 않는다 (호출은 1회)")
    void doesNotCallLlmAgain() {
        given(llm.complete(anyString(), eq(AnalyzedRequirement.class), anyLong()))
                .willReturn(analyzed(ActivityField.ART, 0, List.of(), false, false));

        analysisService.analyze(new AnalyzeRequest("수채화 모임", null));

        verify(llm, times(1)).complete(anyString(), any(), anyLong());
    }
}
