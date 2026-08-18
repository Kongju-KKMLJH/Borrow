package kkmljh.borrow.auth.controller;

import kkmljh.borrow.auth.dto.MeResponse;
import kkmljh.borrow.auth.service.AuthService;
import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Role;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("POST /api/auth/signup · GET /api/auth/me")
class AuthControllerTest {

    private static final String SIGNUP_BODY = """
            {"loginId":"hong","password":"pw1234","nickname":"홍길동","role":"MEMBER"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("회원가입은 비로그인으로 호출할 수 있고 가입 결과를 공통 포맷으로 돌려준다")
    void signup_permitAll() throws Exception {
        given(authService.signup(any())).willReturn(new MeResponse("hong", "홍길동", Role.MEMBER));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("hong"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("가입 응답에 비밀번호는 담기지 않는다")
    void signup_neverExposesPassword() throws Exception {
        given(authService.signup(any())).willReturn(new MeResponse("hong", "홍길동", Role.MEMBER));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.id").doesNotExist());
    }

    @Test
    @DisplayName("필수 값이 빠지면 400 INVALID_REQUEST")
    void signup_validationFails() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"","password":"pw1234","nickname":"홍길동","role":"MEMBER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("역할(role)이 없으면 400 INVALID_REQUEST")
    void signup_roleRequired() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"hong","password":"pw1234","nickname":"홍길동"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("중복 아이디면 409 DUPLICATE_LOGIN_ID")
    void signup_duplicate() throws Exception {
        given(authService.signup(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    @DisplayName("로그인 상태에서 내 정보를 조회하면 로그인 아이디 기준으로 조회한다")
    void me_authenticated() throws Exception {
        given(authService.me(TestUsers.MEMBER)).willReturn(new MeResponse(TestUsers.MEMBER, "홍길동", Role.MEMBER));

        mockMvc.perform(get("/api/auth/me").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value(TestUsers.MEMBER))
                .andExpect(jsonPath("$.data.nickname").value("홍길동"));
    }

    @Test
    @DisplayName("비로그인으로 내 정보를 조회하면 401 UNAUTHORIZED 를 공통 포맷으로 준다")
    void me_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    @DisplayName("HOST · ARTIST 계정도 내 정보 조회는 가능하다 (역할 무관)")
    void me_anyRole() throws Exception {
        given(authService.me(TestUsers.HOST)).willReturn(new MeResponse(TestUsers.HOST, "공간주인", Role.HOST));
        given(authService.me(TestUsers.ARTIST)).willReturn(new MeResponse(TestUsers.ARTIST, "예술가", Role.ARTIST));

        mockMvc.perform(get("/api/auth/me").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("HOST"));

        mockMvc.perform(get("/api/auth/me").with(TestUsers.artist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ARTIST"));
    }
}
