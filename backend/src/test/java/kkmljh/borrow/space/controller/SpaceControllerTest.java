package kkmljh.borrow.space.controller;

import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.space.dto.SpaceResponse;
import kkmljh.borrow.space.service.SpaceService;
import kkmljh.borrow.support.TestFixtures;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpaceController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/spaces — 공간 CRUD (B-02~B-06)")
class SpaceControllerTest {

    private static final String BODY = """
            {"name":"불당동 스튜디오","region":"천안시 서북구 불당동","address":"불당대로 1",
             "imageUrls":["/files/s.jpg"],"capacity":10,"hourlyFee":10000,"conditions":"음료 1잔 주문",
             "facilities":["TABLE"],"allowedFields":["ART"],"noiseAllowed":true,"messAllowed":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpaceService spaceService;

    private SpaceResponse response(Long id) {
        return new SpaceResponse(id, "불당동 스튜디오", "천안시 서북구 불당동", "불당대로 1",
                List.of("/files/s.jpg"), 10, 10_000, "음료 1잔 주문",
                Set.of(FacilityType.TABLE), Set.of(ActivityField.ART), true, false);
    }

    /** 공개 응답 — 주소 전문 없이 동 단위(region)까지만 (기능명세 6.1 rules) */
    private SpaceResponse publicResponse(Long id) {
        return SpaceResponse.from(TestFixtures.space(id, TestUsers.HOST));
    }

    @Test
    @DisplayName("B-02 HOST 가 공간을 등록하면 로그인 아이디가 소유자로 전달된다")
    void create() throws Exception {
        given(spaceService.create(eq(TestUsers.HOST), any())).willReturn(response(1L));

        mockMvc.perform(post("/api/spaces").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("불당동 스튜디오"));

        verify(spaceService).create(eq(TestUsers.HOST), any());
    }

    @Test
    @DisplayName("응답에 소유자(ownerId)는 노출하지 않는다")
    void responseHidesOwner() throws Exception {
        given(spaceService.create(eq(TestUsers.HOST), any())).willReturn(response(1L));

        mockMvc.perform(post("/api/spaces").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").doesNotExist());
    }

    @Test
    @DisplayName("MEMBER 는 공간을 등록할 수 없다 — 403")
    void memberCannotCreate() throws Exception {
        mockMvc.perform(post("/api/spaces").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("비로그인 등록은 401")
    void anonymousCannotCreate() throws Exception {
        mockMvc.perform(post("/api/spaces")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이름·지역이 비면 400 INVALID_REQUEST")
    void validation() throws Exception {
        mockMvc.perform(post("/api/spaces").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","region":"천안","capacity":10,"hourlyFee":0,
                                 "noiseAllowed":false,"messAllowed":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("name: 공간 이름은 필수입니다."));
    }

    @Test
    @DisplayName("수용 인원이 1명 미만이면 400")
    void capacityValidation() throws Exception {
        mockMvc.perform(post("/api/spaces").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"스튜디오","region":"천안","capacity":0,"hourlyFee":0,
                                 "noiseAllowed":false,"messAllowed":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("capacity: 수용 인원은 1명 이상이어야 합니다."));
    }

    @Test
    @DisplayName("⚠️ 프론트 주의: 원시 타입 필드(capacity·hourlyFee·noiseAllowed·messAllowed)를 빼면 본문 해석 실패로 400")
    void primitiveFieldsMustBePresent() throws Exception {
        mockMvc.perform(post("/api/spaces").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"스튜디오\",\"region\":\"천안\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("요청 본문을 해석할 수 없습니다. 필수 값을 확인하세요."));
    }

    @Test
    @DisplayName("B-03 목록은 비로그인 열람 가능")
    void listIsPublic() throws Exception {
        given(spaceService.findAll()).willReturn(List.of(response(1L), response(2L)));

        mockMvc.perform(get("/api/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("회귀: HOST 계정도 목록을 볼 수 있다 (공개 GET 규칙이 HOST 규칙보다 위)")
    void hostCanReadList() throws Exception {
        given(spaceService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/spaces").with(TestUsers.host()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("B-04 상세도 비로그인 열람 가능")
    void detailIsPublic() throws Exception {
        given(spaceService.findById(1L)).willReturn(response(1L));

        mockMvc.perform(get("/api/spaces/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(10))
                .andExpect(jsonPath("$.data.facilities[0]").value("TABLE"));
    }

    @Test
    @DisplayName("없는 공간 상세는 404 SPACE_NOT_FOUND")
    void detailNotFound() throws Exception {
        given(spaceService.findById(99L)).willThrow(new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        mockMvc.perform(get("/api/spaces/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("공개 목록 응답에는 주소 전문이 없다 (기능명세 6.1 rules)")
    void listHidesAddress() throws Exception {
        given(spaceService.findAll()).willReturn(List.of(publicResponse(1L)));

        mockMvc.perform(get("/api/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].address").doesNotExist())
                .andExpect(jsonPath("$.data[0].region").value("천안시 서북구 불당동"));
    }

    @Test
    @DisplayName("공개 상세 응답에도 주소 전문이 없다")
    void detailHidesAddress() throws Exception {
        given(spaceService.findById(1L)).willReturn(publicResponse(1L));

        mockMvc.perform(get("/api/spaces/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").doesNotExist())
                .andExpect(jsonPath("$.data.region").value("천안시 서북구 불당동"));
    }

    @Test
    @DisplayName("내 공간 목록(HOST 본인)에는 주소 전문이 나간다")
    void mineKeepsAddress() throws Exception {
        given(spaceService.findMySpaces(TestUsers.HOST))
                .willReturn(List.of(SpaceResponse.forOwner(TestFixtures.space(1L, TestUsers.HOST))));

        mockMvc.perform(get("/api/spaces/mine").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].address").value("불당대로 1"));
    }

    @Test
    @DisplayName("내 공간 목록은 HOST 전용이고 로그인 아이디로 조회한다")
    void mine() throws Exception {
        given(spaceService.findMySpaces(TestUsers.HOST)).willReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/spaces/mine").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(spaceService).findMySpaces(TestUsers.HOST);
    }

    @Test
    @DisplayName("MEMBER 의 내 공간 목록 조회는 403, 비로그인은 401")
    void mineIsHostOnly() throws Exception {
        mockMvc.perform(get("/api/spaces/mine").with(TestUsers.member()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/spaces/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("B-05 수정 성공")
    void update() throws Exception {
        given(spaceService.update(eq(TestUsers.HOST), eq(1L), any())).willReturn(response(1L));

        mockMvc.perform(put("/api/spaces/1").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("남의 공간 수정은 403 FORBIDDEN")
    void updateOthersSpace() throws Exception {
        given(spaceService.update(eq(TestUsers.HOST), eq(1L), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(put("/api/spaces/1").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("B-06 삭제는 본문 없는 성공 응답")
    void delete_() throws Exception {
        mockMvc.perform(delete("/api/spaces/1").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(spaceService).delete(TestUsers.HOST, 1L);
    }

    @Test
    @DisplayName("개최 요청이 있는 공간 삭제는 409 SPACE_HAS_REQUESTS")
    void deleteWithRequests() throws Exception {
        willThrow(new BusinessException(ErrorCode.SPACE_HAS_REQUESTS))
                .given(spaceService).delete(any(), any());

        mockMvc.perform(delete("/api/spaces/1").with(TestUsers.host()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SPACE_HAS_REQUESTS"));
    }

    @Test
    @DisplayName("MEMBER 는 수정·삭제할 수 없다 — 403")
    void memberCannotModify() throws Exception {
        mockMvc.perform(put("/api/spaces/1").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/spaces/1").with(TestUsers.member()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("이미 등록한 공간을 다시 등록하면 409 DUPLICATE_SPACE (기능명세 6.1 exceptions)")
    void createDuplicate() throws Exception {
        willThrow(new BusinessException(ErrorCode.DUPLICATE_SPACE))
                .given(spaceService).create(eq(TestUsers.HOST), any());

        mockMvc.perform(post("/api/spaces").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SPACE"));
    }

    @Test
    @DisplayName("수정으로 내 다른 공간과 겹쳐도 409 DUPLICATE_SPACE")
    void updateDuplicate() throws Exception {
        willThrow(new BusinessException(ErrorCode.DUPLICATE_SPACE))
                .given(spaceService).update(eq(TestUsers.HOST), eq(1L), any());

        mockMvc.perform(put("/api/spaces/1").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SPACE"));
    }
}
