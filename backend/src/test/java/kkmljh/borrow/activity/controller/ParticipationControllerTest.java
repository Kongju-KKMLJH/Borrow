package kkmljh.borrow.activity.controller;

import kkmljh.borrow.activity.dto.ParticipationResponse;
import kkmljh.borrow.activity.service.ParticipationService;
import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParticipationController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/activities/{id}/participations — 참여 신청 · 취소")
class ParticipationControllerTest {

    private static final String BODY = "{\"headcount\":2}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipationService participationService;

    @Test
    @DisplayName("U-04 참여 신청 성공 — 닉네임은 서버가 채운 값이 나간다")
    void participate() throws Exception {
        given(participationService.participate(eq(TestUsers.MEMBER), eq(1L), any()))
                .willReturn(new ParticipationResponse(10L, 1L, "일반회원", 2));

        mockMvc.perform(post("/api/activities/1/participations").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.activityId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("일반회원"))
                .andExpect(jsonPath("$.data.headcount").value(2));
    }

    @Test
    @DisplayName("요청에 nickname 을 보내도 무시된다 (사칭 차단)")
    void nicknameInRequestIsIgnored() throws Exception {
        given(participationService.participate(eq(TestUsers.MEMBER), eq(1L), any()))
                .willReturn(new ParticipationResponse(10L, 1L, "일반회원", 2));

        mockMvc.perform(post("/api/activities/1/participations").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headcount\":2,\"nickname\":\"사칭\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("일반회원"));
    }

    @Test
    @DisplayName("HOST 계정도 참여할 수 있다 (역할 무관)")
    void hostCanParticipate() throws Exception {
        given(participationService.participate(eq(TestUsers.HOST), eq(1L), any()))
                .willReturn(new ParticipationResponse(11L, 1L, "공간주인", 2));

        mockMvc.perform(post("/api/activities/1/participations").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비로그인 참여는 401")
    void anonymousCannotParticipate() throws Exception {
        mockMvc.perform(post("/api/activities/1/participations")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("인원이 0 이하면 400")
    void headcountMustBePositive() throws Exception {
        mockMvc.perform(post("/api/activities/1/participations").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"headcount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("모집 중이 아니면 400 ACTIVITY_NOT_PUBLISHED")
    void notPublished() throws Exception {
        given(participationService.participate(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.ACTIVITY_NOT_PUBLISHED));

        mockMvc.perform(post("/api/activities/1/participations").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ACTIVITY_NOT_PUBLISHED"));
    }

    @Test
    @DisplayName("정원 초과는 400 CAPACITY_EXCEEDED")
    void capacityExceeded() throws Exception {
        given(participationService.participate(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.CAPACITY_EXCEEDED));

        mockMvc.perform(post("/api/activities/1/participations").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CAPACITY_EXCEEDED"));
    }

    @Test
    @DisplayName("중복 신청은 409 ALREADY_PARTICIPATED")
    void alreadyParticipated() throws Exception {
        given(participationService.participate(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.ALREADY_PARTICIPATED));

        mockMvc.perform(post("/api/activities/1/participations").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_PARTICIPATED"));
    }

    @Test
    @DisplayName("U-05 참여 취소는 본문 없는 성공 응답")
    void cancel() throws Exception {
        mockMvc.perform(delete("/api/activities/1/participations").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(participationService).cancel(TestUsers.MEMBER, 1L);
    }

    @Test
    @DisplayName("신청 내역이 없으면 404 PARTICIPATION_NOT_FOUND")
    void cancelNotFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND))
                .given(participationService).cancel(any(), any());

        mockMvc.perform(delete("/api/activities/1/participations").with(TestUsers.member()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PARTICIPATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("비로그인 취소는 401")
    void anonymousCannotCancel() throws Exception {
        mockMvc.perform(delete("/api/activities/1/participations"))
                .andExpect(status().isUnauthorized());
    }
}
