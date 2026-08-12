package kkmljh.borrow.activity.controller;

import kkmljh.borrow.activity.dto.HostingRequestResponse;
import kkmljh.borrow.activity.service.ActivityHostingRequestService;
import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.RequestStatus;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityHostingRequestController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/activities/{id}/hosting-request — 개최 요청 전송 · 상태 (U-11, U-12)")
class ActivityHostingRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityHostingRequestService hostingRequestService;

    @Test
    @DisplayName("U-11 선택한 공간으로 개최 요청을 보내면 PENDING 요청이 생성된다")
    void send() throws Exception {
        given(hostingRequestService.send(TestUsers.MEMBER, 1L, 5L))
                .willReturn(new HostingRequestResponse(7L, 1L, 5L, "불당동 스튜디오", RequestStatus.PENDING, null));

        mockMvc.perform(post("/api/activities/1/hosting-request").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"spaceId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.spaceId").value(5))
                .andExpect(jsonPath("$.data.spaceName").value("불당동 스튜디오"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(hostingRequestService).send(TestUsers.MEMBER, 1L, 5L);
    }

    @Test
    @DisplayName("ARTIST 도 개최 요청을 보낼 수 있다")
    void artistCanSend() throws Exception {
        given(hostingRequestService.send(eq(TestUsers.ARTIST), eq(1L), eq(5L)))
                .willReturn(new HostingRequestResponse(7L, 1L, 5L, "불당동 스튜디오", RequestStatus.PENDING, null));

        mockMvc.perform(post("/api/activities/1/hosting-request").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"spaceId\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HOST 는 개최 요청을 보낼 수 없다 — 403")
    void hostCannotSend() throws Exception {
        mockMvc.perform(post("/api/activities/1/hosting-request").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"spaceId\":5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("spaceId 가 없으면 400")
    void spaceIdRequired() throws Exception {
        mockMvc.perform(post("/api/activities/1/hosting-request").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("이미 요청한 활동이면 409 ALREADY_REQUESTED")
    void alreadyRequested() throws Exception {
        given(hostingRequestService.send(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.ALREADY_REQUESTED));

        mockMvc.perform(post("/api/activities/1/hosting-request").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"spaceId\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_REQUESTED"));
    }

    @Test
    @DisplayName("남의 활동으로 요청하면 403 FORBIDDEN")
    void notOwner() throws Exception {
        given(hostingRequestService.send(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/activities/1/hosting-request").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"spaceId\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("U-12 요청 상태 조회 — 거절이면 사유도 함께 온다")
    void readStatus() throws Exception {
        given(hostingRequestService.status(TestUsers.MEMBER, 1L)).willReturn(
                new HostingRequestResponse(7L, 1L, 5L, "불당동 스튜디오", RequestStatus.REJECTED, "예약이 있습니다."));

        mockMvc.perform(get("/api/activities/1/hosting-request").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReason").value("예약이 있습니다."));
    }

    @Test
    @DisplayName("보낸 요청이 없으면 404 REQUEST_NOT_FOUND")
    void statusNotFound() throws Exception {
        given(hostingRequestService.status(any(), any()))
                .willThrow(new BusinessException(ErrorCode.REQUEST_NOT_FOUND));

        mockMvc.perform(get("/api/activities/1/hosting-request").with(TestUsers.member()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REQUEST_NOT_FOUND"));
    }

    @Test
    @DisplayName("회귀: 비로그인 상태 조회는 401 — 개설자 전용 정보가 새면 안 된다")
    void anonymousCannotReadStatus() throws Exception {
        mockMvc.perform(get("/api/activities/1/hosting-request"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
