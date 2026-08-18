package kkmljh.borrow.activity.controller;

import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.MyParticipationResponse;
import kkmljh.borrow.activity.service.ActivityService;
import kkmljh.borrow.activity.service.ParticipationService;
import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/me — 내가 개설한 활동 · 참여한 활동 (U-13, U-14)")
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private ParticipationService participationService;

    private ActivitySummaryResponse summary(Long id, boolean joined, boolean mine) {
        return new ActivitySummaryResponse(
                id, ActivityType.HOBBY, ActivityField.ART, "수채화 모임", List.of(),
                "일반회원", false, LocalDate.of(2026, 9, 12),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 8, 3, 5, 10_000,
                ActivityStatus.PUBLISHED, null, joined, mine);
    }

    @Test
    @DisplayName("U-13 내가 개설한 활동은 모두 mine=true")
    void myActivities() throws Exception {
        given(activityService.myActivities(TestUsers.MEMBER))
                .willReturn(List.of(summary(1L, false, true)));

        mockMvc.perform(get("/api/me/activities").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].mine").value(true));
    }

    @Test
    @DisplayName("U-14 내가 참여한 활동은 참여 인원과 함께 내려온다")
    void myParticipations() throws Exception {
        given(participationService.myParticipations(TestUsers.MEMBER))
                .willReturn(List.of(new MyParticipationResponse(10L, 2, summary(1L, true, false))));

        mockMvc.perform(get("/api/me/participations").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].participationId").value(10))
                .andExpect(jsonPath("$.data[0].myHeadcount").value(2))
                .andExpect(jsonPath("$.data[0].activity.alreadyJoined").value(true))
                .andExpect(jsonPath("$.data[0].activity.mine").value(false));
    }

    @Test
    @DisplayName("역할과 무관하게 로그인만 하면 호출할 수 있다")
    void anyRoleCanCall() throws Exception {
        given(activityService.myActivities(TestUsers.HOST)).willReturn(List.of());
        given(participationService.myParticipations(TestUsers.ARTIST)).willReturn(List.of());

        mockMvc.perform(get("/api/me/activities").with(TestUsers.host()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me/participations").with(TestUsers.artist()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비로그인 호출은 401")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me/activities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/me/participations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내역이 없으면 빈 배열")
    void emptyList() throws Exception {
        given(activityService.myActivities(TestUsers.MEMBER)).willReturn(List.of());

        mockMvc.perform(get("/api/me/activities").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
