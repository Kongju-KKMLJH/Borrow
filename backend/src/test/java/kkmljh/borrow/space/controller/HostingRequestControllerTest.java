package kkmljh.borrow.space.controller;

import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.RequestStatus;
import kkmljh.borrow.space.dto.HostingRequestResponse;
import kkmljh.borrow.space.service.HostingRequestService;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;

@WebMvcTest(HostingRequestController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/host/requests — 개최요청 처리 (B-07~B-10)")
class HostingRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HostingRequestService hostingRequestService;

    private HostingRequestResponse response(Long id, RequestStatus status, String reason) {
        return response(id, status, reason, false);
    }

    private HostingRequestResponse response(Long id, RequestStatus status, String reason, boolean scheduleMismatch) {
        return new HostingRequestResponse(id, status, reason,
                new HostingRequestResponse.SpaceInfo(5L, "불당동 스튜디오", "천안시 서북구"),
                new HostingRequestResponse.ActivityInfo(1L, "수채화 모임", "설명", ActivityField.ART,
                        LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), LocalTime.of(16, 0),
                        8, 10_000, "일반회원",
                        new HostingRequestResponse.RequirementInfo(6, Set.of(FacilityType.WATER), false, true)),
                scheduleMismatch);
    }

    @Test
    @DisplayName("B-07 목록은 내 공간에 온 요청만 — 로그인 아이디로 조회한다")
    void list() throws Exception {
        given(hostingRequestService.findRequests(eq(TestUsers.HOST), isNull(), isNull()))
                .willReturn(List.of(response(1L, RequestStatus.PENDING, null)));

        mockMvc.perform(get("/api/host/requests").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].space.name").value("불당동 스튜디오"))
                .andExpect(jsonPath("$.data[0].activity.title").value("수채화 모임"))
                .andExpect(jsonPath("$.data[0].activity.requirement.headcount").value(6));

        verify(hostingRequestService).findRequests(TestUsers.HOST, null, null);
    }

    @Test
    @DisplayName("spaceId · status 필터가 서비스로 전달된다")
    void listWithFilters() throws Exception {
        given(hostingRequestService.findRequests(TestUsers.HOST, 5L, RequestStatus.APPROVED))
                .willReturn(List.of());

        mockMvc.perform(get("/api/host/requests")
                        .param("spaceId", "5").param("status", "APPROVED")
                        .with(TestUsers.host()))
                .andExpect(status().isOk());

        verify(hostingRequestService).findRequests(TestUsers.HOST, 5L, RequestStatus.APPROVED);
    }

    @Test
    @DisplayName("MEMBER 는 요청 목록을 볼 수 없다 — 403, 비로그인은 401")
    void listRequiresHost() throws Exception {
        mockMvc.perform(get("/api/host/requests").with(TestUsers.member()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/host/requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("B-08 상세 조회")
    void detail() throws Exception {
        given(hostingRequestService.findById(TestUsers.HOST, 1L))
                .willReturn(response(1L, RequestStatus.PENDING, null));

        mockMvc.perform(get("/api/host/requests/1").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.entryFee").value(10000))
                .andExpect(jsonPath("$.data.scheduleMismatch").value(false));
    }

    @Test
    @DisplayName("B-08 상세 조회 — 공간 유휴시간과 안 맞는 요청은 scheduleMismatch=true (F-XOKOSU)")
    void detailScheduleMismatch() throws Exception {
        given(hostingRequestService.findById(TestUsers.HOST, 1L))
                .willReturn(response(1L, RequestStatus.PENDING, null, true));

        mockMvc.perform(get("/api/host/requests/1").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleMismatch").value(true));
    }

    @Test
    @DisplayName("남의 공간 요청 상세는 404 REQUEST_NOT_FOUND (존재 자체를 숨긴다)")
    void detailOfOthersRequest() throws Exception {
        given(hostingRequestService.findById(any(), any()))
                .willThrow(new BusinessException(ErrorCode.REQUEST_NOT_FOUND));

        mockMvc.perform(get("/api/host/requests/1").with(TestUsers.host()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REQUEST_NOT_FOUND"));
    }

    @Test
    @DisplayName("B-09 승인하면 APPROVED 로 바뀐다")
    void approve() throws Exception {
        given(hostingRequestService.approve(TestUsers.HOST, 1L))
                .willReturn(response(1L, RequestStatus.APPROVED, null));

        mockMvc.perform(post("/api/host/requests/1/approve").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("이미 처리된 요청 승인은 409 REQUEST_ALREADY_HANDLED")
    void approveAlreadyHandled() throws Exception {
        given(hostingRequestService.approve(any(), any()))
                .willThrow(new BusinessException(ErrorCode.REQUEST_ALREADY_HANDLED));

        mockMvc.perform(post("/api/host/requests/1/approve").with(TestUsers.host()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REQUEST_ALREADY_HANDLED"));
    }

    @Test
    @DisplayName("B-10 거절하면 사유가 함께 저장된다")
    void reject() throws Exception {
        given(hostingRequestService.reject(TestUsers.HOST, 1L, "예약이 있습니다."))
                .willReturn(response(1L, RequestStatus.REJECTED, "예약이 있습니다."));

        mockMvc.perform(post("/api/host/requests/1/reject").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"예약이 있습니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectReason").value("예약이 있습니다."));
    }

    @Test
    @DisplayName("거절 본문은 생략할 수 있고 사유는 null 로 전달된다")
    void rejectWithoutBody() throws Exception {
        given(hostingRequestService.reject(TestUsers.HOST, 1L, null))
                .willReturn(response(1L, RequestStatus.REJECTED, null));

        mockMvc.perform(post("/api/host/requests/1/reject").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(hostingRequestService).reject(TestUsers.HOST, 1L, null);
    }

    @Test
    @DisplayName("MEMBER 는 승인·거절할 수 없다 — 403")
    void memberCannotHandle() throws Exception {
        mockMvc.perform(post("/api/host/requests/1/approve").with(TestUsers.member()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/host/requests/1/reject").with(TestUsers.member()))
                .andExpect(status().isForbidden());
    }
}
