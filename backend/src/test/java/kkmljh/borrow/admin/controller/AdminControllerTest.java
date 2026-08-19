package kkmljh.borrow.admin.controller;

import kkmljh.borrow.admin.dto.AdminActivityResponse;
import kkmljh.borrow.admin.dto.AdminSpaceResponse;
import kkmljh.borrow.admin.dto.AdminUserResponse;
import kkmljh.borrow.admin.dto.AdminVerificationResponse;
import kkmljh.borrow.admin.service.AdminActivityService;
import kkmljh.borrow.admin.service.AdminSpaceService;
import kkmljh.borrow.admin.service.AdminUserService;
import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ArtistVerificationStatus;
import kkmljh.borrow.support.TestFixtures;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminUserController.class, AdminActivityController.class, AdminSpaceController.class})
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/admin — 관리자 콘솔 (기능명세 7)")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private AdminActivityService adminActivityService;

    @MockitoBean
    private AdminSpaceService adminSpaceService;

    private static AdminUserResponse userResponse() {
        return AdminUserResponse.of(TestFixtures.artist(), ArtistVerificationStatus.PENDING);
    }

    @Nested
    @DisplayName("회원 관리 (7.1)")
    class Users {

        @Test
        @DisplayName("7.1.1 관리자는 전체 회원 목록을 조회한다")
        void list() throws Exception {
            given(adminUserService.findAll()).willReturn(List.of(userResponse()));

            mockMvc.perform(get("/api/admin/users").with(TestUsers.admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].loginId").value("artist1"))
                    .andExpect(jsonPath("$.data[0].role").value("ARTIST"))
                    .andExpect(jsonPath("$.data[0].verificationStatus").value("PENDING"))
                    .andExpect(jsonPath("$.data[0].withdrawn").value(false));
        }

        @Test
        @DisplayName("조회할 회원이 없으면 빈 목록을 준다 (7.1.1 exceptions)")
        void emptyList() throws Exception {
            given(adminUserService.findAll()).willReturn(List.of());

            mockMvc.perform(get("/api/admin/users").with(TestUsers.admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("7.1.3 강제 탈퇴")
        void withdraw() throws Exception {
            given(adminUserService.withdraw(3L)).willReturn(userResponse());

            mockMvc.perform(post("/api/admin/users/3/withdraw").with(TestUsers.admin()))
                    .andExpect(status().isOk());
            verify(adminUserService).withdraw(3L);
        }

        @Test
        @DisplayName("MEMBER 는 회원 목록을 볼 수 없고 서비스도 호출되지 않는다")
        void memberForbidden() throws Exception {
            mockMvc.perform(get("/api/admin/users").with(TestUsers.member()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.FORBIDDEN.name()));
            verify(adminUserService, never()).findAll();
        }

        @Test
        @DisplayName("7.1.4 인증 신청 목록은 status 파라미터로 걸러진다")
        void verificationsWithStatus() throws Exception {
            given(adminUserService.findVerifications(ArtistVerificationStatus.APPROVED))
                    .willReturn(List.of());

            mockMvc.perform(get("/api/admin/artist-verifications")
                            .param("status", "APPROVED").with(TestUsers.admin()))
                    .andExpect(status().isOk());
            verify(adminUserService).findVerifications(ArtistVerificationStatus.APPROVED);
        }

        @Test
        @DisplayName("7.1.4 인증 승인")
        void approve() throws Exception {
            given(adminUserService.approve(5L)).willReturn(
                    AdminVerificationResponse.of(TestFixtures.verification(), "예술가"));

            mockMvc.perform(post("/api/admin/artist-verifications/5/approve").with(TestUsers.admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.loginId").value("artist1"));
        }

        @Test
        @DisplayName("7.1.4 이미 처리된 신청의 중복 승인은 409")
        void approveTwice() throws Exception {
            willThrow(new BusinessException(ErrorCode.REQUEST_ALREADY_HANDLED))
                    .given(adminUserService).approve(5L);

            mockMvc.perform(post("/api/admin/artist-verifications/5/approve").with(TestUsers.admin()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.REQUEST_ALREADY_HANDLED.name()));
        }
    }

    @Nested
    @DisplayName("프로그램 관리 (7.2)")
    class Activities {

        @Test
        @DisplayName("7.2.1 전체 프로그램 목록")
        void list() throws Exception {
            given(adminActivityService.findAll()).willReturn(
                    List.of(AdminActivityResponse.of(TestFixtures.activity(), null)));

            mockMvc.perform(get("/api/admin/activities").with(TestUsers.admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].title").value("수채화 모임"))
                    .andExpect(jsonPath("$.data[0].status").value("DRAFT"))
                    .andExpect(jsonPath("$.data[0].forceDeleted").value(false));
        }

        @Test
        @DisplayName("7.2.3 강제 삭제")
        void forceDelete() throws Exception {
            given(adminActivityService.forceDelete(1L)).willReturn(
                    AdminActivityResponse.of(TestFixtures.activity(), null));

            mockMvc.perform(post("/api/admin/activities/1/force-delete").with(TestUsers.admin()))
                    .andExpect(status().isOk());
            verify(adminActivityService).forceDelete(1L);
        }

        @Test
        @DisplayName("ARTIST 는 강제 삭제를 실행할 수 없다")
        void artistForbidden() throws Exception {
            mockMvc.perform(post("/api/admin/activities/1/force-delete").with(TestUsers.artist()))
                    .andExpect(status().isForbidden());
            verify(adminActivityService, never()).forceDelete(any());
        }
    }

    @Nested
    @DisplayName("공간 관리 (7.3)")
    class Spaces {

        @Test
        @DisplayName("7.3.1 전체 공간 목록 — 관리자에게는 주소 전문을 준다")
        void list() throws Exception {
            given(adminSpaceService.findAll())
                    .willReturn(List.of(AdminSpaceResponse.from(TestFixtures.space())));

            mockMvc.perform(get("/api/admin/spaces").with(TestUsers.admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].name").value("불당동 스튜디오"))
                    .andExpect(jsonPath("$.data[0].address").value("불당대로 1"))
                    .andExpect(jsonPath("$.data[0].ownerId").value("owner1"));
        }

        @Test
        @DisplayName("7.3.3 강제 삭제")
        void forceDelete() throws Exception {
            given(adminSpaceService.forceDelete(1L))
                    .willReturn(AdminSpaceResponse.from(TestFixtures.space()));

            mockMvc.perform(post("/api/admin/spaces/1/force-delete").with(TestUsers.admin()))
                    .andExpect(status().isOk());
            verify(adminSpaceService).forceDelete(1L);
        }

        @Test
        @DisplayName("HOST 는 남의 공간을 강제 삭제할 수 없다 — 관리자 경로는 HOST 에게 닫혀 있다")
        void hostForbidden() throws Exception {
            mockMvc.perform(post("/api/admin/spaces/1/force-delete").with(TestUsers.host()))
                    .andExpect(status().isForbidden());
            verify(adminSpaceService, never()).forceDelete(any());
        }

        @Test
        @DisplayName("비로그인은 401")
        void anonymous() throws Exception {
            mockMvc.perform(get("/api/admin/spaces"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value(ErrorCode.UNAUTHORIZED.name()));
        }
    }
}
