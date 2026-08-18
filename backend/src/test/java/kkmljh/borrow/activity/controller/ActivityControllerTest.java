package kkmljh.borrow.activity.controller;

import kkmljh.borrow.activity.dto.ActivityDetailResponse;
import kkmljh.borrow.activity.dto.ActivitySummaryResponse;
import kkmljh.borrow.activity.dto.ActivityUpdateRequest;
import kkmljh.borrow.activity.dto.RequirementUpdateRequest;
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
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/activities — 개설 · 수정 · 삭제 · 목록 · 상세 · 요구조건")
class ActivityControllerTest {

    private static final String CREATE_BODY = """
            {"field":"ART","title":"수채화 모임","description":"설명","imageUrls":["/files/a.jpg"],
             "date":"2026-09-12","startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":10000,
             "requirement":{"region":"천안시 서북구","headcount":6,"requiredFacilities":["WATER"],
                            "noisy":false,"messy":true}}
            """;

    /** 수정 요청에는 type·hostCertified·hostNickname·requirement 를 담지 않는다. */
    private static final String UPDATE_BODY = """
            {"field":"PHOTO","title":"출사 모임","description":"바뀐 설명","imageUrls":["/files/new.jpg"],
             "date":"2026-12-01","startTime":"09:00:00","endTime":"11:30:00","capacity":12,"entryFee":25000}
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
                LocalTime.of(14, 0), LocalTime.of(16, 0), 8, 0, 8, 10_000,
                ActivityStatus.DRAFT, null,
                new SpaceRequirementDto("천안시 서북구", 6, Set.of(FacilityType.WATER), false, true),
                false, false);
    }

    private ActivitySummaryResponse summary(Long id, boolean joined, boolean mine) {
        return new ActivitySummaryResponse(
                id, ActivityType.HOBBY, ActivityField.ART, "수채화 모임", List.of(),
                "일반회원", false, LocalDate.of(2026, 9, 12),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 8, 3, 5, 10_000,
                ActivityStatus.PUBLISHED,
                new ActivityDetailResponse.SpaceInfo(7L, "불당 카페", "천안시 서북구 불당동"),
                joined, mine);
    }

    @Test
    @DisplayName("MEMBER 는 활동을 개설할 수 없다 — 403 (개설은 예술가로 한정, 기능명세 1.2)")
    void memberCannotCreate() throws Exception {
        mockMvc.perform(post("/api/activities").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(activityService, never()).create(any(), any());
    }

    @Test
    @DisplayName("U-06 ARTIST 가 개설하면 CLASS · 인증 배지로 만들어진다 (F-01)")
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
        // 서버가 정하는 값만 응답에 실린다 — 요청이 CLASS·배지·닉네임을 보내도 반영되지 않는다.
        given(activityService.create(eq(TestUsers.ARTIST), any()))
                .willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(post("/api/activities").with(TestUsers.artist())
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
        mockMvc.perform(post("/api/activities").with(TestUsers.artist())
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
        mockMvc.perform(post("/api/activities").with(TestUsers.artist())
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

        mockMvc.perform(post("/api/activities").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("종료 시각은 시작 시각보다 늦어야 합니다."));
    }

    @Test
    @DisplayName("U-01 목록은 비로그인으로 조회할 수 있고 참여 여부는 false 로 나간다")
    void listAnonymous() throws Exception {
        given(activityService.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
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
        given(activityService.search(eq(TestUsers.MEMBER), eq(ActivityType.CLASS), eq(ActivityField.PHOTO), eq("출사"), isNull(), isNull(), isNull()))
                .willReturn(List.of());

        mockMvc.perform(get("/api/activities")
                        .param("type", "CLASS").param("field", "PHOTO").param("keyword", "출사")
                        .with(TestUsers.member()))
                .andExpect(status().isOk());

        verify(activityService).search(TestUsers.MEMBER, ActivityType.CLASS, ActivityField.PHOTO, "출사", null, null, null);
    }

    @Test
    @DisplayName("로그인 상태로 목록을 보면 참여 여부·개설자 여부가 표시된다")
    void listAuthenticated() throws Exception {
        given(activityService.search(eq(TestUsers.MEMBER), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .willReturn(List.of(summary(1L, true, true)));

        mockMvc.perform(get("/api/activities").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].alreadyJoined").value(true))
                .andExpect(jsonPath("$.data[0].mine").value(true));
    }

    @Test
    @DisplayName("기능명세 4.1 region·dateFrom·dateTo 가 서비스로 그대로 전달된다")
    void listWithRegionAndDateFilters() throws Exception {
        given(activityService.search(isNull(), isNull(), isNull(), isNull(), eq("불당동"),
                eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 30))))
                .willReturn(List.of(summary(1L, false, false)));

        mockMvc.perform(get("/api/activities")
                        .param("region", "불당동")
                        .param("dateFrom", "2026-09-01")
                        .param("dateTo", "2026-09-30"))
                .andExpect(status().isOk());

        verify(activityService).search(null, null, null, null, "불당동",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    }

    @Test
    @DisplayName("기능명세 4.1 목록 응답에 잔여 인원과 확정 공간이 실린다")
    void listCarriesRemainingCapacityAndSpace() throws Exception {
        given(activityService.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .willReturn(List.of(summary(1L, false, false)));

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].remainingCapacity").value(5))
                .andExpect(jsonPath("$.data[0].space.id").value(7))
                .andExpect(jsonPath("$.data[0].space.name").value("불당 카페"))
                .andExpect(jsonPath("$.data[0].space.region").value("천안시 서북구 불당동"))
                .andExpect(jsonPath("$.data[0].space.address").doesNotExist());
    }

    @Test
    @DisplayName("dateFrom 이 dateTo 보다 늦으면 400 INVALID_REQUEST")
    void listWithInvertedDateRange() throws Exception {
        given(activityService.search(isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.of(2026, 9, 30)), eq(LocalDate.of(2026, 9, 1))))
                .willThrow(new BusinessException(ErrorCode.INVALID_REQUEST,
                        "조회 시작일(dateFrom)은 종료일(dateTo)보다 늦을 수 없습니다."));

        mockMvc.perform(get("/api/activities")
                        .param("dateFrom", "2026-09-30")
                        .param("dateTo", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("기능명세 4.1 상세 응답에도 잔여 인원·확정 공간이 실리고, 공간에 주소는 없다 (회귀)")
    void detailCarriesRemainingCapacityAndSpaceWithoutAddress() throws Exception {
        given(activityService.detail(isNull(), eq(1L))).willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remainingCapacity").value(8))
                .andExpect(jsonPath("$.data.space").doesNotExist());
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
    @DisplayName("비공개 활동 상세는 비로그인에게 404 — 존재 자체를 감춘다 (기능명세 4.1)")
    void detailHiddenAnonymous() throws Exception {
        given(activityService.detail(isNull(), eq(1L)))
                .willThrow(new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACTIVITY_NOT_FOUND"));
    }

    @Test
    @DisplayName("남의 비공개 활동 상세는 로그인 회원에게도 404 — 403 이면 그 id 에 뭔가 있다고 알려준다")
    void detailHiddenOtherMember() throws Exception {
        given(activityService.detail(eq(TestUsers.MEMBER), eq(1L)))
                .willThrow(new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));

        mockMvc.perform(get("/api/activities/1").with(TestUsers.member()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACTIVITY_NOT_FOUND"));
    }

    @Test
    @DisplayName("공개된 활동 상세는 비로그인으로 계속 열린다 (회귀)")
    void detailPublishedAnonymous() throws Exception {
        ActivityDetailResponse published = new ActivityDetailResponse(
                1L, ActivityType.HOBBY, ActivityField.ART, "수채화 모임", "설명", List.of("/files/a.jpg"),
                "일반회원", false, LocalDate.of(2026, 9, 12),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 8, 0, 8, 10_000,
                ActivityStatus.PUBLISHED,
                new ActivityDetailResponse.SpaceInfo(7L, "불당 카페", "천안시 서북구 불당동"),
                new SpaceRequirementDto("천안시 서북구", 6, Set.of(FacilityType.WATER), false, true),
                false, false);
        given(activityService.detail(isNull(), eq(1L))).willReturn(published);

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
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

    /**
     * 이 테스트는 원래 "요구조건 필드를 빼면 400"(#31)을 고정하고 있었다. #27 수정으로
     * headcount·noisy·messy 가 래퍼 타입이 되면서 해당 결함이 이 하위 DTO 에 한해 해소됐다.
     * 이제 생략은 400 이 아니라 null 로 전달된다 — 요구조건은 원래 선택 항목이므로 이쪽이 맞다.
     */
    @Test
    @DisplayName("요구조건의 인원·소음·오염은 생략할 수 있다 — 생략분은 null 로 전달")
    void requirementFieldsAreOptional() throws Exception {
        given(activityService.updateRequirement(eq(TestUsers.MEMBER), eq(1L), any()))
                .willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(patch("/api/activities/1/requirement").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirement\":{\"region\":\"천안시 서북구\"}}"))
                .andExpect(status().isOk());

        ArgumentCaptor<RequirementUpdateRequest> captor =
                ArgumentCaptor.forClass(RequirementUpdateRequest.class);
        verify(activityService).updateRequirement(eq(TestUsers.MEMBER), eq(1L), captor.capture());

        SpaceRequirementDto sent = captor.getValue().requirement();
        assertThat(sent.region()).isEqualTo("천안시 서북구");
        assertThat(sent.headcount()).isNull();
        assertThat(sent.noisy()).isNull();
        assertThat(sent.messy()).isNull();
    }

    /**
     * #31 은 개설 요청 쪽에는 그대로 남아 있다. capacity·entryFee 는 여전히 원시 타입이라
     * 생략하면 본문 해석 단계에서 실패한다. 프론트는 이 둘을 항상 담아 보내야 한다.
     */
    @Test
    @DisplayName("⚠️ 프론트 주의: 개설 요청의 원시 타입 필드(capacity·entryFee)를 빼면 400")
    void primitiveFieldsMustBePresent() throws Exception {
        mockMvc.perform(post("/api/activities").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"ART","title":"수채화 모임","date":"2026-09-12",
                                 "startTime":"14:00:00","endTime":"16:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    // --- 기능명세 2.1 활동 수정·삭제 ---

    @Test
    @DisplayName("개설자 본인은 활동을 수정할 수 있다")
    void updateByOwner() throws Exception {
        given(activityService.update(eq(TestUsers.MEMBER), eq(1L), any()))
                .willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(put("/api/activities/1").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("ARTIST 도 자기 활동을 수정할 수 있다 (hasAnyRole 이어야 한다)")
    void updateByArtist() throws Exception {
        given(activityService.update(eq(TestUsers.ARTIST), eq(1L), any()))
                .willReturn(detail(ActivityType.CLASS, true));

        mockMvc.perform(put("/api/activities/1").with(TestUsers.artist())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("CLASS"));
    }

    @Test
    @DisplayName("남의 활동 수정은 403 FORBIDDEN")
    void updateOthersActivity() throws Exception {
        given(activityService.update(eq("other-member"), eq(1L), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(put("/api/activities/1").with(TestUsers.as("other-member"))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("비로그인 수정은 401")
    void anonymousCannotUpdate() throws Exception {
        mockMvc.perform(put("/api/activities/1")
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("HOST 는 활동을 수정할 수 없다 — 403")
    void hostCannotUpdate() throws Exception {
        mockMvc.perform(put("/api/activities/1").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("수정 요청의 type·hostCertified·hostNickname·requirement 는 서비스로 전달되지 않는다 (위조 차단)")
    void updateIgnoresForgedFields() throws Exception {
        given(activityService.update(eq(TestUsers.MEMBER), eq(1L), any()))
                .willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(put("/api/activities/1").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"ART","title":"수채화","date":"2026-12-01",
                                 "startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":0,
                                 "type":"CLASS","hostCertified":true,"hostNickname":"사칭",
                                 "requirement":{"region":"위조"}}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<ActivityUpdateRequest> captor = ArgumentCaptor.forClass(ActivityUpdateRequest.class);
        verify(activityService).update(eq(TestUsers.MEMBER), eq(1L), captor.capture());

        // 레코드에 필드 자체가 없으므로 위조 값이 들어올 자리가 없다.
        assertThat(captor.getValue().title()).isEqualTo("수채화");
        assertThat(ActivityUpdateRequest.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("type", "hostCertified", "hostNickname", "guestId", "status", "requirement");
    }

    @Test
    @DisplayName("제목이 비면 400 INVALID_REQUEST")
    void updateTitleRequired() throws Exception {
        mockMvc.perform(put("/api/activities/1").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"ART","title":"","date":"2026-12-01",
                                 "startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("지난 날짜로 수정하면 400 (@FutureOrPresent)")
    void updateRejectsPastDate() throws Exception {
        mockMvc.perform(put("/api/activities/1").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"field":"ART","title":"수채화","date":"2020-01-01",
                                 "startTime":"14:00:00","endTime":"16:00:00","capacity":8,"entryFee":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("PUBLISHED 활동 수정은 400 과 안내 메시지")
    void updatePublishedIsRejected() throws Exception {
        given(activityService.update(eq(TestUsers.MEMBER), eq(1L), any())).willThrow(
                new BusinessException(ErrorCode.INVALID_REQUEST, "개최 요청 후에는 활동을 수정할 수 없습니다."));

        mockMvc.perform(put("/api/activities/1").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("개최 요청 후에는 활동을 수정할 수 없습니다."));
    }

    @Test
    @DisplayName("개설자 본인은 활동을 삭제할 수 있다")
    void deleteByOwner() throws Exception {
        mockMvc.perform(delete("/api/activities/1").with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(activityService).delete(TestUsers.MEMBER, 1L);
    }

    @Test
    @DisplayName("남의 활동 삭제는 403 FORBIDDEN")
    void deleteOthersActivity() throws Exception {
        willThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .given(activityService).delete("other-member", 1L);

        mockMvc.perform(delete("/api/activities/1").with(TestUsers.as("other-member")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("비로그인 삭제는 401")
    void anonymousCannotDelete() throws Exception {
        mockMvc.perform(delete("/api/activities/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("없는 활동 삭제는 404 ACTIVITY_NOT_FOUND")
    void deleteNotFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND))
                .given(activityService).delete(TestUsers.MEMBER, 99L);

        mockMvc.perform(delete("/api/activities/99").with(TestUsers.member()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACTIVITY_NOT_FOUND"));
    }

    @Test
    @DisplayName("회귀: PUT·DELETE 를 열어도 비로그인 상세 조회(U-03)는 여전히 200 이다")
    void publicDetailStillOpenAfterAddingWriteRules() throws Exception {
        given(activityService.detail(isNull(), eq(1L))).willReturn(detail(ActivityType.HOBBY, false));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
