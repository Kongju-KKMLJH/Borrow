package kkmljh.borrow.space.controller;

import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.space.dto.HostHomeResponse;
import kkmljh.borrow.space.dto.ScheduleResponse;
import kkmljh.borrow.space.service.HostService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HostController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/host — 사업자 홈(B-01) · 확정 일정(B-11)")
class HostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HostService hostService;

    private ScheduleResponse schedule() {
        return new ScheduleResponse(1L, 1L, "수채화 모임",
                LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), LocalTime.of(16, 0),
                8, 5L, "불당동 스튜디오");
    }

    @Test
    @DisplayName("B-01 홈 요약은 로그인한 사업자 기준으로 집계된다")
    void home() throws Exception {
        given(hostService.getHome(TestUsers.HOST))
                .willReturn(new HostHomeResponse(3L, 2L, 1L, List.of(), List.of(schedule())));

        mockMvc.perform(get("/api/host/home").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingRequestCount").value(3))
                .andExpect(jsonPath("$.data.confirmedScheduleCount").value(2))
                .andExpect(jsonPath("$.data.spaceCount").value(1))
                .andExpect(jsonPath("$.data.upcomingSchedules[0].spaceName").value("불당동 스튜디오"));

        verify(hostService).getHome(TestUsers.HOST);
    }

    @Test
    @DisplayName("B-11 확정 일정 목록")
    void schedules() throws Exception {
        given(hostService.getConfirmedSchedules(TestUsers.HOST)).willReturn(List.of(schedule()));

        mockMvc.perform(get("/api/host/schedules").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].requestId").value(1))
                .andExpect(jsonPath("$.data[0].title").value("수채화 모임"))
                .andExpect(jsonPath("$.data[0].date").value("2026-09-12"))
                .andExpect(jsonPath("$.data[0].startTime").value("14:00:00"));
    }

    @Test
    @DisplayName("확정 일정이 없으면 빈 배열")
    void emptySchedules() throws Exception {
        given(hostService.getConfirmedSchedules(TestUsers.HOST)).willReturn(List.of());

        mockMvc.perform(get("/api/host/schedules").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("MEMBER · ARTIST 는 사업자 홈에 접근할 수 없다 — 403")
    void nonHostIsForbidden() throws Exception {
        mockMvc.perform(get("/api/host/home").with(TestUsers.member()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/host/schedules").with(TestUsers.artist()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비로그인 접근은 401")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/host/home"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/host/schedules"))
                .andExpect(status().isUnauthorized());
    }
}
