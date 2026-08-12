package kkmljh.borrow.activity.controller;

import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.SpaceRequirementDto;
import kkmljh.borrow.activity.service.ActivityService;
import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityStatus;
import kkmljh.borrow.domain.ActivityType;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/activities — 개설 · 목록 · 상세 · 요구조건")
class ActivityControllerTest {

    private static final String CREATE_BODY = """
            {"field":"ART","title":"수채화 모임","description":"설명","imageUrls":["/files/a.jpg"],
             "date":"2026-09-12","startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":10000,
             "requirement":{"region":"천안시 서북구","headcount":6,"requiredFacilities":["WATER"],
                            "noisy":false,"messy":true}}
            """;

    private static final String REQUIREMENT_BODY = """
            {"requirement":{"region":"천안시 서북구","headcount":6,
                            "requiredFacilities":["WATER"],"noisy":false,"messy":true}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    private ActivityDetailResponse detail(ActivityType type, boolean certified) {
        return new ActivityDetailResponse(
                1L, type, ActivityField.ART, "수채화 모임", "설명", List.of("/files/a.jpg"),
                "일반회원", certified, LocalDate.of(2026, 9, 12),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 8, 0, 10_000,
                ActivityStatus.DRAFT,
                new SpaceRequirementDto("천안시 서북구", 6, Set.of(FacilityType.WATER), false, true),
                false, false);
    }

    private ActivitySummaryResponse summary(Long id, boolean joined, boolean mine) {
        return new ActivitySummaryResponse(
                id, ActivityType.HOBBY, ActivityField.ART, "수채화 모임", List.of(),
                "일반회원", false, LocalDate.of(2026, 9, 12),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 8, 3, 10_000,
                ActivityStatus.PUBLISHED, joined, mine);
    }

    @Test
    @DisplayName("U-06 MEMBER 가 활동을 개설하면 HOBBY 로 만들어진다")
    void createByMember() throws Exception {
        given(activityService.create(eq(TestUsers.MEMBER), any()))
                .willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(post("/api/activities").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("HOBBY"))
                .andExpect(jsonPath("$.data.hostCertified").value(false))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("ARTIST 가 개설하면 CLASS · 인증 배지로 만들어진다 (F-01)")
    void createByArtist() throws Exception {
        given(activityService.create(eq(TestUsers.ARTIST), any()))
                .willReturn(detail(ActivityType.CLASS, true));

        mockMvc.perform(post("/api/activities").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("CLASS"))
                .andExpect(jsonPath("$.data.hostCertified").value(true));
    }

    @Test
    @DisplayName("요청 본문의 type·hostCertified·hostNickname 은 무시된다 (배지·이름 위조 차단)")
    void ignoresForgedFields() throws Exception {
        given(activityService.create(eq(TestUsers.MEMBER), any()))
                .willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(post("/api/activities").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"ART","title":"수채화","date":"2026-09-12",
                                 "startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":0,
                                 "type":"CLASS","hostCertified":true,"hostNickname":"사칭"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("HOBBY"))
                .andExpect(jsonPath("$.data.hostCertified").value(false))
                .andExpect(jsonPath("$.data.hostNickname").value("일반회원"));
    }

    @Test
    @DisplayName("HOST 는 활동을 개설할 수 없다 — 403")
    void hostCannotCreate() throws Exception {
        mockMvc.perform(post("/api/activities").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("비로그인 개설은 401")
    void anonymousCannotCreate() throws Exception {
        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("제목이 비면 400 INVALID_REQUEST")
    void titleRequired() throws Exception {
        mockMvc.perform(post("/api/activities").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"ART","title":"","date":"2026-09-12",
                                 "startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("정원이 0 이하면 400 INVALID_REQUEST")
    void capacityMustBePositive() throws Exception {
        mockMvc.perform(post("/api/activities").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"ART","title":"수채화","date":"2026-09-12",
                                 "startTime":"14:00:00","endTime":"16:00:00","capacity":0,"entryFee":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("종료 시각이 더 빠르면 400 과 안내 메시지")
    void invalidTimeRange() throws Exception {
        given(activityService.create(any(), any())).willThrow(
                new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다."));

        mockMvc.perform(post("/api/activities").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("종료 시각은 시작 시각보다 늦어야 합니다."));
    }

    @Test
    @DisplayName("U-01 목록은 비로그인으로 조회할 수 있고 참여 여부는 false 로 나간다")
    void listAnonymous() throws Exception {
        given(activityService.search(isNull(), isNull(), isNull(), isNull()))
                .willReturn(List.of(summary(1L, false, false)));

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].alreadyJoined").value(false))
                .andExpect(jsonPath("$.data[0].currentHeadcount").value(3));
    }

    @Test
    @DisplayName("U-02 type·field·keyword 필터가 서비스로 그대로 전달된다")
    void listWithFilters() throws Exception {
        given(activityService.search(eq(TestUsers.MEMBER), eq(ActivityType.CLASS), eq(ActivityField.PHOTO), eq("출사")))
                .willReturn(List.of());

        mockMvc.perform(get("/api/activities")
                        .param("type", "CLASS").param("field", "PHOTO").param("keyword", "출사")
                        .with(TestUsers.member()))
                .andExpect(status().isOk());

        verify(activityService).search(TestUsers.MEMBER, ActivityType.CLASS, ActivityField.PHOTO, "출사");
    }

    @Test
    @DisplayName("로그인 상태로 목록을 보면 참여 여부·개설자 여부가 표시된다")
    void listAuthenticated() throws Exception {
        given(activityService.search(eq(TestUsers.MEMBER), isNull(), isNull(), isNull()))
                .willReturn(List.of(summary(1L, true, true)));

        mockMvc.perform(get("/api/activities").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].alreadyJoined").value(true))
                .andExpect(jsonPath("$.data[0].mine").value(true));
    }

    @Test
    @DisplayName("⚠️ 알려진 제약: 알 수 없는 필터 값은 400이 아니라 500 INTERNAL_ERROR 로 나간다")
    void invalidFilterValue() throws Exception {
        // MethodArgumentTypeMismatchException 은 스프링이 400으로 처리하는 예외지만,
        // GlobalExceptionHandler 의 @ExceptionHandler(Exception.class) 가 먼저 잡아 500으로 바꾼다.
        mockMvc.perform(get("/api/activities").param("type", "UNKNOWN"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("U-03 상세는 비로그인으로 조회할 수 있다")
    void detailAnonymous() throws Exception {
        given(activityService.detail(isNull(), eq(1L))).willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("수채화 모임"))
                .andExpect(jsonPath("$.data.requirement.headcount").value(6));
    }

    @Test
    @DisplayName("없는 활동 상세는 404 ACTIVITY_NOT_FOUND")
    void detailNotFound() throws Exception {
        given(activityService.detail(isNull(), eq(99L)))
                .willThrow(new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));

        mockMvc.perform(get("/api/activities/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACTIVITY_NOT_FOUND"));
    }

    @Test
    @DisplayName("U-08 요구조건 수정은 개설자 본인만 — 남의 활동이면 403")
    void updateRequirementForbidden() throws Exception {
        given(activityService.updateRequirement(eq(TestUsers.MEMBER), eq(1L), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/activities/1/requirement").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUIREMENT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("U-08 요구조건 수정 성공")
    void updateRequirement() throws Exception {
        given(activityService.updateRequirement(eq(TestUsers.MEMBER), eq(1L), any()))
                .willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(patch("/api/activities/1/requirement").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUIREMENT_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requirement.region").value("천안시 서북구"));
    }

    @Test
    @DisplayName("요구조건 본문이 없으면 400")
    void updateRequirementValidation() throws Exception {
        mockMvc.perform(patch("/api/activities/1/requirement").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("⚠️ 프론트 주의: 원시 타입 필드(headcount·noisy·messy)를 빼면 400 — 항상 전부 담아 보내야 한다")
    void primitiveFieldsMustBePresent() throws Exception {
        mockMvc.perform(patch("/api/activities/1/requirement").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirement\":{\"region\":\"천안시 서북구\",\"headcount\":6}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}
