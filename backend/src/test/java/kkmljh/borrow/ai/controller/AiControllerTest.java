package kkmljh.borrow.ai.controller;

import kkmljh.borrow.ai.dto.RequirementResponse;
import kkmljh.borrow.ai.dto.SpaceMatchResponse;
import kkmljh.borrow.ai.dto.SpaceMatchResult;
import kkmljh.borrow.ai.service.ActivityAnalysisService;
import kkmljh.borrow.ai.service.SpaceMatchingService;
import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/ai — A-01 분석 · A-02/A-03 매칭")
class AiControllerTest {

    private static final String MATCH_BODY = """
            {"region":"천안시 서북구","headcount":6,"requiredFacilities":["WATER"],
             "noisy":false,"messy":true,"field":"ART","date":"2026-09-12",
             "startTime":"14:00:00","endTime":"16:00:00"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityAnalysisService analysisService;

    @MockitoBean
    private SpaceMatchingService matchingService;

    @Test
    @DisplayName("A-01 활동 설명을 구조화된 요구조건으로 돌려준다")
    void analyze() throws Exception {
        given(analysisService.analyze(any())).willReturn(new RequirementResponse(
                "천안시 서북구", 6, List.of(FacilityType.WATER), false, true, ActivityField.ART,
                List.of(), List.of()));

        mockMvc.perform(post("/api/ai/analyze").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"수채화를 그리는 모임\",\"region\":\"천안시 서북구\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.field").value("ART"))
                .andExpect(jsonPath("$.data.headcount").value(6))
                .andExpect(jsonPath("$.data.requiredFacilities[0]").value("WATER"))
                .andExpect(jsonPath("$.data.messy").value(true));
    }

    @Test
    @DisplayName("활동 설명이 비면 400 INVALID_REQUEST")
    void analyzeValidation() throws Exception {
        mockMvc.perform(post("/api/ai/analyze").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("description: 활동 설명은 필수입니다."));
    }

    @Test
    @DisplayName("분석 실패는 500 AI_ANALYSIS_FAILED")
    void analyzeFailure() throws Exception {
        given(analysisService.analyze(any())).willThrow(new BusinessException(ErrorCode.AI_ANALYSIS_FAILED));

        mockMvc.perform(post("/api/ai/analyze").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"수채화\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("AI_ANALYSIS_FAILED"));
    }

    @Test
    @DisplayName("A-02/A-03 매칭 결과를 점수와 함께 돌려준다")
    void match() throws Exception {
        given(matchingService.match(any())).willReturn(SpaceMatchResult.matched(List.of(new SpaceMatchResponse(
                1L, "불당동 스튜디오", "천안시 서북구 불당동", 10, 10_000,
                List.of("/files/s.jpg"), Set.of(FacilityType.WATER), Set.of(ActivityField.ART),
                92, "물 사용이 가능하고 인원도 여유롭습니다.",
                List.of("이용 조건을 확인하세요 — 음료 1잔 주문 필수"), true))));

        mockMvc.perform(post("/api/ai/match").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(MATCH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched[0].spaceId").value(1))
                .andExpect(jsonPath("$.data.matched[0].score").value(92))
                .andExpect(jsonPath("$.data.matched[0].aiScored").value(true))
                .andExpect(jsonPath("$.data.matched[0].reason").value("물 사용이 가능하고 인원도 여유롭습니다."))
                .andExpect(jsonPath("$.data.matched[0].cautions[0]").value("이용 조건을 확인하세요 — 음료 1잔 주문 필수"))
                .andExpect(jsonPath("$.data.suggestions").isEmpty());
    }

    @Test
    @DisplayName("기능명세 3.1.1 — 조건에 맞는 공간이 없으면 에러가 아니라 200 + 조건 수정 안내")
    void matchEmpty() throws Exception {
        given(matchingService.match(any())).willReturn(
                SpaceMatchResult.noMatch(List.of("희망 지역을 넓혀 보세요 — 동 단위 대신 구·시 단위로 검색하면 후보가 늘어납니다.")));

        mockMvc.perform(post("/api/ai/match").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(MATCH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matched").isArray())
                .andExpect(jsonPath("$.data.matched").isEmpty())
                .andExpect(jsonPath("$.data.suggestions[0]")
                        .value("희망 지역을 넓혀 보세요 — 동 단위 대신 구·시 단위로 검색하면 후보가 늘어납니다."));
    }

    @Test
    @DisplayName("분야·날짜·시각이 빠지면 400")
    void matchValidation() throws Exception {
        mockMvc.perform(post("/api/ai/match").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"region\":\"천안\",\"headcount\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("인원이 0 이하면 400")
    void matchHeadcountValidation() throws Exception {
        mockMvc.perform(post("/api/ai/match").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"region":"천안","headcount":0,"field":"ART","date":"2026-09-12",
                                 "startTime":"14:00:00","endTime":"16:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("headcount: 수용 인원은 1명 이상이어야 합니다."));
    }

    @Test
    @DisplayName("AI 호출은 역할과 무관하게 로그인만 하면 된다")
    void anyRoleCanCall() throws Exception {
        given(matchingService.match(any())).willReturn(SpaceMatchResult.matched(List.of()));

        mockMvc.perform(post("/api/ai/match").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(MATCH_BODY))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ai/match").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON).content(MATCH_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비로그인 호출은 401")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"수채화\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/ai/match")
                        .contentType(MediaType.APPLICATION_JSON).content(MATCH_BODY))
                .andExpect(status().isUnauthorized());
    }
}
