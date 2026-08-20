package kkmljh.borrow.common.config;

import kkmljh.borrow.auth.repository.AppUserRepository;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.Role;
import kkmljh.borrow.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인가 매트릭스 — {@code SecurityConfig} 의 경로별 권한 규칙을 실제 필터체인으로 검증한다.
 *
 * <p>이 설정에서 가장 흔한 버그는 <b>규칙 순서</b>다. 위에서 먼저 매칭되는 줄이 이기므로
 * 비로그인 GET 줄이 {@code hasRole("HOST")} 줄 아래로 내려가면 목록 조회가 막히고,
 * {@code GET /api/activities/**} 처럼 뭉뚱그리면 개설자 전용 정보가 샌다.
 */
@IntegrationTest
@DisplayName("SecurityConfig 인가 매트릭스")
class SecurityConfigTest {

    private static final String PW = "pw1234";
    private static final String ADMIN_ID = "admin1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private void signup(String loginId, String role) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"%s","nickname":"%s","role":"%s"}
                                """.formatted(loginId, PW, loginId, role)))
                .andExpect(status().isOk());
    }

    @BeforeEach
    void setUpAccounts() throws Exception {
        signup("member1", "MEMBER");
        signup("host1", "HOST");
        signup("artist1", "ARTIST");
        // ADMIN 은 가입 API 로 만들 수 없으므로(그게 규칙이다) 운영의 시드 러너처럼 직접 저장한다.
        appUserRepository.save(AppUser.builder()
                .loginId(ADMIN_ID)
                .password(passwordEncoder.encode(PW))
                .nickname("관리자")
                .role(Role.ADMIN)
                .build());
    }

    private int statusOf(RequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        return result.getResponse().getStatus();
    }

    /** 인가를 통과했는지만 본다 — 리소스가 없어 404가 나는 것은 통과로 친다. */
    private void assertAllowed(RequestBuilder request) throws Exception {
        assertThat(statusOf(request)).isNotIn(401, 403);
    }

    private void assertUnauthorized(RequestBuilder request) throws Exception {
        assertThat(statusOf(request)).isEqualTo(401);
    }

    private void assertForbidden(RequestBuilder request) throws Exception {
        assertThat(statusOf(request)).isEqualTo(403);
    }

    @Nested
    @DisplayName("비로그인 공개 범위")
    class PublicAccess {

        @Test
        @DisplayName("회원가입은 비로그인으로 호출할 수 있다")
        void signupIsPublic() throws Exception {
            assertAllowed(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"loginId":"newbie","password":"pw1234","nickname":"뉴비","role":"MEMBER"}
                            """));
        }

        @Test
        @DisplayName("활동 목록·상세는 비로그인 열람 가능 (U-01, U-03)")
        void activityReadIsPublic() throws Exception {
            mockMvc.perform(get("/api/activities")).andExpect(status().isOk());
            assertAllowed(get("/api/activities/1"));
        }

        @Test
        @DisplayName("공간 목록·상세·슬롯은 비로그인 열람 가능")
        void spaceReadIsPublic() throws Exception {
            mockMvc.perform(get("/api/spaces")).andExpect(status().isOk());
            assertAllowed(get("/api/spaces/1"));
            assertAllowed(get("/api/spaces/1/slots"));
        }

        @Test
        @DisplayName("Swagger 문서는 비로그인 접근 가능")
        void swaggerIsPublic() throws Exception {
            assertAllowed(get("/v3/api-docs"));
            assertAllowed(get("/swagger-ui/index.html"));
        }

        @Test
        @DisplayName("업로드된 이미지 정적 경로는 비로그인 접근 가능")
        void filesArePublic() throws Exception {
            assertAllowed(get("/files/none.jpg"));
        }

        @Test
        @DisplayName("CORS 프리플라이트(OPTIONS)는 인증 없이 통과한다")
        void preflightIsPublic() throws Exception {
            mockMvc.perform(options("/api/activities")
                            .header("Origin", "http://localhost:8081")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("로그인 필요 (역할 무관)")
    class AuthenticatedOnly {

        @Test
        @DisplayName("내 정보 조회는 로그인 필요")
        void me() throws Exception {
            assertUnauthorized(get("/api/auth/me"));
            assertAllowed(get("/api/auth/me").with(httpBasic("member1", PW)));
        }

        @Test
        @DisplayName("내 활동·내 참여는 로그인만 하면 역할과 무관하게 열린다")
        void myPages() throws Exception {
            assertUnauthorized(get("/api/me/activities"));
            assertUnauthorized(get("/api/me/participations"));

            for (String user : new String[]{"member1", "host1", "artist1"}) {
                assertAllowed(get("/api/me/activities").with(httpBasic(user, PW)));
                assertAllowed(get("/api/me/participations").with(httpBasic(user, PW)));
            }
        }

        @Test
        @DisplayName("예술가 인증은 /api/me/** 규칙을 그대로 탄다 — 비로그인 401, 조회는 역할 무관 (기능명세 1.2)")
        void artistVerificationFollowsMeRule() throws Exception {
            String body = """
                    {"portfolioUrl":"https://portfolio.example/artist1","career":"수채화 3년"}
                    """;

            assertUnauthorized(get("/api/me/artist-verification"));
            assertUnauthorized(post("/api/me/artist-verification")
                    .contentType(MediaType.APPLICATION_JSON).content(body));

            for (String user : new String[]{"member1", "host1", "artist1"}) {
                assertAllowed(get("/api/me/artist-verification").with(httpBasic(user, PW)));
            }
        }

        @Test
        @DisplayName("인증 신청은 ARTIST 만 — 이 403은 SecurityConfig가 아니라 서비스 판정이다 (기능명세 1.2)")
        void artistVerificationApplyIsArtistOnly() throws Exception {
            String body = """
                    {"portfolioUrl":"https://portfolio.example/artist1","career":"수채화 3년"}
                    """;

            assertAllowed(post("/api/me/artist-verification").with(httpBasic("artist1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertForbidden(post("/api/me/artist-verification").with(httpBasic("member1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertForbidden(post("/api/me/artist-verification").with(httpBasic("host1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        @Test
        @DisplayName("HOST 계정도 활동에 참여할 수 있다 (역할로 참여를 막지 않는다)")
        void participationIsRoleAgnostic() throws Exception {
            assertUnauthorized(post("/api/activities/1/participations")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"headcount\":1}"));

            for (String user : new String[]{"member1", "host1", "artist1"}) {
                assertAllowed(post("/api/activities/1/participations")
                        .with(httpBasic(user, PW))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"headcount\":1}"));
                assertAllowed(delete("/api/activities/1/participations").with(httpBasic(user, PW)));
            }
        }

        @Test
        @DisplayName("AI 매칭은 로그인만 하면 역할과 무관하게 호출 가능")
        void aiRequiresLoginOnly() throws Exception {
            assertUnauthorized(post("/api/ai/analyze")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"수채화\"}"));
            assertAllowed(post("/api/ai/analyze").with(httpBasic("host1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"수채화\"}"));
        }

        @Test
        @DisplayName("이미지 업로드는 로그인 필요")
        void uploadRequiresLogin() throws Exception {
            assertUnauthorized(post("/api/uploads"));
        }
    }

    @Nested
    @DisplayName("활동 개설은 ARTIST 전용, 개설된 활동의 관리는 MEMBER · ARTIST")
    class ActivityWriteAccess {

        private RequestBuilder createActivity(String user) {
            return post("/api/activities")
                    .with(httpBasic(user, PW))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"field":"ART","title":"수채화","date":"2026-09-12",
                             "startTime":"14:00","endTime":"16:00","capacity":8,"entryFee":0}
                            """);
        }

        @Test
        @DisplayName("MEMBER 는 활동을 개설할 수 없다 — 개설은 예술가로 한정 (기능명세 1.2)")
        void memberCannotCreate() throws Exception {
            assertForbidden(createActivity("member1"));
        }

        @Test
        @DisplayName("ARTIST 는 활동을 개설할 수 있다")
        void artistCanCreate() throws Exception {
            assertAllowed(createActivity("artist1"));
        }

        @Test
        @DisplayName("HOST 는 활동을 개설할 수 없다")
        void hostCannotCreate() throws Exception {
            assertForbidden(createActivity("host1"));
        }

        @Test
        @DisplayName("비로그인 개설은 401")
        void anonymousCannotCreate() throws Exception {
            assertUnauthorized(post("/api/activities")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"field\":\"ART\",\"title\":\"수채화\"}"));
        }

        @Test
        @DisplayName("요구조건 수정은 MEMBER · ARTIST 만")
        void requirementUpdate() throws Exception {
            String body = "{\"requirement\":{\"region\":\"천안\",\"headcount\":4}}";

            assertUnauthorized(patch("/api/activities/1/requirement")
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertForbidden(patch("/api/activities/1/requirement").with(httpBasic("host1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertAllowed(patch("/api/activities/1/requirement").with(httpBasic("member1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        @Test
        @DisplayName("활동 수정(PUT)은 MEMBER · ARTIST 만 (기능명세 2.1)")
        void activityUpdate() throws Exception {
            String body = """
                    {"field":"ART","title":"수채화","date":"2026-12-01",
                     "startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":0}
                    """;

            assertUnauthorized(put("/api/activities/1")
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertForbidden(put("/api/activities/1").with(httpBasic("host1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertAllowed(put("/api/activities/1").with(httpBasic("member1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertAllowed(put("/api/activities/1").with(httpBasic("artist1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        @Test
        @DisplayName("활동 삭제(DELETE)는 MEMBER · ARTIST 만 (기능명세 2.1)")
        void activityDelete() throws Exception {
            assertUnauthorized(delete("/api/activities/1"));
            assertForbidden(delete("/api/activities/1").with(httpBasic("host1", PW)));
            assertAllowed(delete("/api/activities/1").with(httpBasic("member1", PW)));
            assertAllowed(delete("/api/activities/1").with(httpBasic("artist1", PW)));
        }

        @Test
        @DisplayName("매칭 이용료 Mock 결제(POST payment)는 MEMBER · ARTIST 만 (기능명세 3.3)")
        void activityPayment() throws Exception {
            assertUnauthorized(post("/api/activities/1/payment"));
            assertForbidden(post("/api/activities/1/payment").with(httpBasic("host1", PW)));
            assertAllowed(post("/api/activities/1/payment").with(httpBasic("member1", PW)));
            assertAllowed(post("/api/activities/1/payment").with(httpBasic("artist1", PW)));
        }

        /**
         * 이 절에서 제일 나기 쉬운 버그 — PUT/DELETE 규칙에 HttpMethod 를 빼고 경로만 쓰면
         * 같은 URL 의 비로그인 GET(U-03 상세)까지 함께 잡혀 목록에서 상세로 못 들어간다.
         */
        @Test
        @DisplayName("회귀: PUT·DELETE 를 막아도 GET /api/activities/{id} 는 비로그인으로 열려 있어야 한다")
        void detailStaysPublicAfterWriteRules() throws Exception {
            assertAllowed(get("/api/activities/1"));
            assertThat(statusOf(get("/api/activities"))).isEqualTo(200);
        }

        @Test
        @DisplayName("개최 요청 전송·상태 조회는 MEMBER · ARTIST 만 (U-11, U-12)")
        void hostingRequest() throws Exception {
            assertForbidden(get("/api/activities/1/hosting-request").with(httpBasic("host1", PW)));
            assertAllowed(get("/api/activities/1/hosting-request").with(httpBasic("member1", PW)));
            assertAllowed(get("/api/activities/1/hosting-request").with(httpBasic("artist1", PW)));
        }

        @Test
        @DisplayName("회귀: 비로그인 개최요청 상태 조회는 401 — GET /api/activities/** 를 통째로 열면 안 된다")
        void hostingRequestIsNotPublic() throws Exception {
            assertUnauthorized(get("/api/activities/1/hosting-request"));
            assertUnauthorized(post("/api/activities/1/hosting-request")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"spaceId\":1}"));
        }
    }

    @Nested
    @DisplayName("공간 운영은 HOST")
    class SpaceWriteAccess {

        private RequestBuilder createSpace(String user) {
            return post("/api/spaces")
                    .with(httpBasic(user, PW))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"스튜디오","region":"천안시 서북구","capacity":10,"hourlyFee":10000}
                            """);
        }

        @Test
        @DisplayName("HOST 만 공간을 등록할 수 있다")
        void onlyHostCanCreate() throws Exception {
            assertAllowed(createSpace("host1"));
            assertForbidden(createSpace("member1"));
            assertForbidden(createSpace("artist1"));
        }

        @Test
        @DisplayName("공간 수정·삭제·슬롯 등록은 HOST 만")
        void onlyHostCanModify() throws Exception {
            String body = "{\"name\":\"스튜디오\",\"region\":\"천안\",\"capacity\":10,\"hourlyFee\":10000}";

            assertForbidden(put("/api/spaces/1").with(httpBasic("member1", PW))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
            assertForbidden(delete("/api/spaces/1").with(httpBasic("member1", PW)));
            assertForbidden(post("/api/spaces/1/slots").with(httpBasic("member1", PW))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"dayOfWeek\":\"SATURDAY\",\"startTime\":\"09:00\",\"endTime\":\"22:00\"}"));
        }

        @Test
        @DisplayName("회귀: HOST 계정의 공간 목록 조회는 막히지 않는다 (공개 GET 줄이 HOST 줄보다 위)")
        void hostCanStillReadSpaceList() throws Exception {
            mockMvc.perform(get("/api/spaces").with(httpBasic("host1", PW)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/spaces/mine 은 HOST 전용 — {spaceId} 패턴에 삼켜지지 않아야 한다")
        void mineIsHostOnly() throws Exception {
            assertUnauthorized(get("/api/spaces/mine"));
            assertForbidden(get("/api/spaces/mine").with(httpBasic("member1", PW)));
            mockMvc.perform(get("/api/spaces/mine").with(httpBasic("host1", PW)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("사업자 홈·요청 처리는 HOST 만 (B-01, B-07~B-11)")
        void hostAreaIsHostOnly() throws Exception {
            assertUnauthorized(get("/api/host/home"));
            assertForbidden(get("/api/host/home").with(httpBasic("member1", PW)));
            assertForbidden(get("/api/host/requests").with(httpBasic("artist1", PW)));

            mockMvc.perform(get("/api/host/home").with(httpBasic("host1", PW))).andExpect(status().isOk());
            mockMvc.perform(get("/api/host/schedules").with(httpBasic("host1", PW))).andExpect(status().isOk());
            mockMvc.perform(get("/api/host/requests").with(httpBasic("host1", PW))).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("관리자 콘솔은 ADMIN 전용 (기능명세 7)")
    class AdminConsole {

        private static final String[] READ_PATHS = {
                "/api/admin/users", "/api/admin/activities", "/api/admin/spaces",
                "/api/admin/artist-verifications"
        };

        @Test
        @DisplayName("비로그인은 401")
        void anonymousBlocked() throws Exception {
            for (String path : READ_PATHS) {
                assertUnauthorized(get(path));
            }
            assertUnauthorized(delete("/api/admin/users/1"));
        }

        @Test
        @DisplayName("MEMBER · HOST · ARTIST 는 403 — 로그인해도 관리자 경로에 들어오지 못한다")
        void otherRolesForbidden() throws Exception {
            for (String loginId : new String[]{"member1", "host1", "artist1"}) {
                for (String path : READ_PATHS) {
                    assertForbidden(get(path).with(httpBasic(loginId, PW)));
                }
                assertForbidden(delete("/api/admin/activities/1").with(httpBasic(loginId, PW)));
                assertForbidden(delete("/api/admin/spaces/1").with(httpBasic(loginId, PW)));
            }
        }

        @Test
        @DisplayName("ADMIN 은 통과한다")
        void adminAllowed() throws Exception {
            for (String path : READ_PATHS) {
                assertAllowed(get(path).with(httpBasic(ADMIN_ID, PW)));
            }
        }

        @Test
        @DisplayName("실제 데이터 CRUD 경로도 ADMIN 전용이다 (기능명세 7.1.2 · 7.2.2 · 7.3.2)")
        void dataCrudIsAdminOnly() throws Exception {
            String[] collections = {"/api/admin/users", "/api/admin/activities", "/api/admin/spaces"};

            for (String path : collections) {
                assertUnauthorized(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"));
                assertUnauthorized(put(path + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"));
                assertUnauthorized(delete(path + "/1"));

                for (String loginId : new String[]{"member1", "host1", "artist1"}) {
                    assertForbidden(post(path).with(httpBasic(loginId, PW))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"));
                    assertForbidden(put(path + "/1").with(httpBasic(loginId, PW))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"));
                    assertForbidden(delete(path + "/1").with(httpBasic(loginId, PW)));
                }

                // ADMIN 은 인가를 통과한다 — 본문이 비어 400 이 나는 것은 통과로 친다.
                assertAllowed(post(path).with(httpBasic(ADMIN_ID, PW))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"));
            }
        }

        @Test
        @DisplayName("관리자 콘솔에서도 ADMIN 계정은 만들 수 없다")
        void consoleCannotCreateAdmin() throws Exception {
            mockMvc.perform(post("/api/admin/users").with(httpBasic(ADMIN_ID, PW))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"loginId":"mockadmin","password":"pw1234","nickname":"가짜관리자","role":"ADMIN"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        }

        @Test
        @DisplayName("관리자는 다른 역할의 경로를 쓰지 못한다 — 대신 편집하지 않는다는 범위 제외를 지킨다")
        void adminHasNoOtherRolePower() throws Exception {
            assertForbidden(get("/api/spaces/mine").with(httpBasic(ADMIN_ID, PW)));
            assertForbidden(get("/api/host/home").with(httpBasic(ADMIN_ID, PW)));
            assertForbidden(post("/api/activities").with(httpBasic(ADMIN_ID, PW))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"));
            assertForbidden(delete("/api/activities/1").with(httpBasic(ADMIN_ID, PW)));
        }

        @Test
        @DisplayName("회원가입으로는 ADMIN 이 될 수 없다 — 비로그인 가입 경로가 관리자 승격 통로가 되면 안 된다")
        void signupCannotClaimAdmin() throws Exception {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"loginId":"sneaky","password":"pw1234","nickname":"침입자","role":"ADMIN"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        }

        /**
         * 소프트 삭제(강제 탈퇴)를 걷어낸 뒤에도 <b>지워진 회원은 못 들어온다</b>는 결론은 그대로다.
         * 다만 그 근거가 {@code .disabled(...)} 플래그가 아니라 <b>행이 사라져서</b>로 바뀌었다 —
         * {@code AppUserDetailsService} 가 조회에 실패해 401 이 난다.
         */
        @Test
        @DisplayName("삭제된 회원은 로그인 자체가 막힌다 (기능명세 7.1.2)")
        void deletedUserCannotAuthenticate() throws Exception {
            AppUser member = appUserRepository.findByLoginId("member1").orElseThrow();
            mockMvc.perform(delete("/api/admin/users/" + member.getId())
                            .with(httpBasic(ADMIN_ID, PW)))
                    .andExpect(status().isOk());

            assertUnauthorized(get("/api/auth/me").with(httpBasic("member1", PW)));
        }

        @Test
        @DisplayName("관리자 계정은 콘솔에서 삭제할 수 없다 — 콘솔 자체가 잠긴다 (403)")
        void consoleCannotDeleteAdmin() throws Exception {
            AppUser admin = appUserRepository.findByLoginId(ADMIN_ID).orElseThrow();

            mockMvc.perform(delete("/api/admin/users/" + admin.getId()).with(httpBasic(ADMIN_ID, PW)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("401 / 403 응답 포맷")
    class ErrorFormat {

        @Test
        @DisplayName("401 도 공통 포맷이고 브라우저 팝업을 부르는 WWW-Authenticate 를 내보내지 않는다")
        void unauthorizedFormat() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.error.message").value("로그인이 필요합니다."))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .header().doesNotExist("WWW-Authenticate"));
        }

        @Test
        @DisplayName("403 도 공통 포맷")
        void forbiddenFormat() throws Exception {
            mockMvc.perform(get("/api/host/home").with(httpBasic("member1", PW)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.error.message").value("권한이 없습니다."));
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401")
        void wrongPassword() throws Exception {
            mockMvc.perform(get("/api/auth/me").with(httpBasic("member1", "wrong")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("없는 아이디로 인증하면 401")
        void unknownUser() throws Exception {
            mockMvc.perform(get("/api/auth/me").with(httpBasic("ghost", PW)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    @DisplayName("세션을 만들지 않는다 (STATELESS) — 매 요청 Basic 자격증명으로 인증한다")
    void statelessSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me").with(httpBasic("member1", PW)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("정의되지 않은 경로는 기본적으로 인증을 요구한다 (anyRequest().authenticated())")
    void unknownPathRequiresAuth() throws Exception {
        assertThat(statusOf(get("/api/whatever"))).isEqualTo(401);
    }

    @Test
    @DisplayName("CORS 설정이 프리플라이트 응답에 반영된다")
    void corsHeaders() throws Exception {
        mockMvc.perform(options("/api/spaces")
                        .header("Origin", "http://192.168.0.10:8081")
                        .header("Access-Control-Request-Method", HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://192.168.0.10:8081"));
    }
}
