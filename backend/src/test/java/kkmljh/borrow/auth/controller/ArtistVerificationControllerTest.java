package kkmljh.borrow.auth.controller;

import kkmljh.borrow.auth.dto.ArtistVerificationResponse;
import kkmljh.borrow.auth.service.ArtistVerificationService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistVerificationController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/me/artist-verification — 예술가 인증 신청·상태 확인 (기능명세 1.2)")
class ArtistVerificationControllerTest {

    private static final String BODY = """
            {"portfolioUrl":"https://portfolio.example/artist1","career":"수채화 클래스 3년"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistVerificationService artistVerificationService;

    private ArtistVerificationResponse pending() {
        LocalDateTime at = LocalDateTime.of(2026, 8, 18, 10, 0);
        return new ArtistVerificationResponse("PENDING",
                "https://portfolio.example/artist1", "수채화 클래스 3년", null, at, at);
    }

    @Test
    @DisplayName("ARTIST 가 신청하면 로그인 아이디로 접수된다")
    void apply() throws Exception {
        given(artistVerificationService.apply(eq(TestUsers.ARTIST), any())).willReturn(pending());

        mockMvc.perform(post("/api/me/artist-verification").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.portfolioUrl").value("https://portfolio.example/artist1"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("포트폴리오 URL 이 비면 400 INVALID_REQUEST")
    void validation() throws Exception {
        mockMvc.perform(post("/api/me/artist-verification").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"portfolioUrl\":\"\",\"career\":\"경력\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("ARTIST 가 아닌 계정의 신청은 403 FORBIDDEN (서비스가 판정한다)")
    void memberForbidden() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .given(artistVerificationService).apply(eq(TestUsers.MEMBER), any());

        mockMvc.perform(post("/api/me/artist-verification").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("심사 중 재신청은 409 ALREADY_REQUESTED")
    void alreadyRequested() throws Exception {
        willThrow(new BusinessException(ErrorCode.ALREADY_REQUESTED))
                .given(artistVerificationService).apply(eq(TestUsers.ARTIST), any());

        mockMvc.perform(post("/api/me/artist-verification").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_REQUESTED"));
    }

    @Test
    @DisplayName("비로그인 신청은 401")
    void anonymousCannotApply() throws Exception {
        mockMvc.perform(post("/api/me/artist-verification")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("신청한 적이 없어도 200 + status NONE (404 로 만들지 않는다)")
    void statusNone() throws Exception {
        given(artistVerificationService.status(TestUsers.ARTIST))
                .willReturn(ArtistVerificationResponse.none());

        mockMvc.perform(get("/api/me/artist-verification").with(TestUsers.artist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("NONE"))
                .andExpect(jsonPath("$.data.appliedAt").doesNotExist());
    }

    @Test
    @DisplayName("상태 조회는 로그인만 하면 되고, 비로그인은 401")
    void statusRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/me/artist-verification"))
                .andExpect(status().isUnauthorized());
    }
}
