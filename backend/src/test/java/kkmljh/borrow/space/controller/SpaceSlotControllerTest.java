package kkmljh.borrow.space.controller;

import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.space.dto.SpaceSlotResponse;
import kkmljh.borrow.space.service.SpaceSlotService;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpaceSlotController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("/api/spaces/{spaceId}/slots — 유휴 시간대 (B-05)")
class SpaceSlotControllerTest {

    private static final String BODY = """
            {"dayOfWeek":"SATURDAY","startTime":"09:00:00","endTime":"22:00:00"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpaceSlotService spaceSlotService;

    private SpaceSlotResponse slot(Long id) {
        return new SpaceSlotResponse(id, DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(22, 0));
    }

    @Test
    @DisplayName("슬롯 목록은 비로그인 열람 가능")
    void listIsPublic() throws Exception {
        given(spaceSlotService.findBySpace(1L)).willReturn(List.of(slot(10L)));

        mockMvc.perform(get("/api/spaces/1/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value("SATURDAY"))
                .andExpect(jsonPath("$.data[0].startTime").value("09:00:00"));
    }

    @Test
    @DisplayName("없는 공간의 슬롯 목록은 404 SPACE_NOT_FOUND")
    void listSpaceNotFound() throws Exception {
        given(spaceSlotService.findBySpace(99L)).willThrow(new BusinessException(ErrorCode.SPACE_NOT_FOUND));

        mockMvc.perform(get("/api/spaces/99/slots"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SPACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("HOST 가 내 공간에 슬롯을 추가한다")
    void add() throws Exception {
        given(spaceSlotService.add(eq(TestUsers.HOST), eq(1L), any())).willReturn(slot(10L));

        mockMvc.perform(post("/api/spaces/1/slots").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));

        verify(spaceSlotService).add(eq(TestUsers.HOST), eq(1L), any());
    }

    @Test
    @DisplayName("남의 공간에 슬롯 추가는 403 FORBIDDEN")
    void addToOthersSpace() throws Exception {
        given(spaceSlotService.add(any(), any(), any()))
                .willThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/spaces/1/slots").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("MEMBER 는 슬롯을 추가할 수 없다 — 403, 비로그인은 401")
    void addRequiresHost() throws Exception {
        mockMvc.perform(post("/api/spaces/1/slots").with(TestUsers.member())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/spaces/1/slots")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("요일·시각이 빠지면 400")
    void validation() throws Exception {
        mockMvc.perform(post("/api/spaces/1/slots").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"09:00:00\",\"endTime\":\"22:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("dayOfWeek: 요일은 필수입니다."));
    }

    @Test
    @DisplayName("종료 시각이 시작보다 빠르면 400 과 안내 메시지")
    void invalidTimeRange() throws Exception {
        given(spaceSlotService.add(any(), any(), any())).willThrow(
                new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다."));

        mockMvc.perform(post("/api/spaces/1/slots").with(TestUsers.host())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("종료 시각은 시작 시각보다 늦어야 합니다."));
    }

    @Test
    @DisplayName("슬롯 삭제는 본문 없는 성공 응답")
    void delete_() throws Exception {
        mockMvc.perform(delete("/api/spaces/1/slots/10").with(TestUsers.host()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(spaceSlotService).delete(TestUsers.HOST, 1L, 10L);
    }

    @Test
    @DisplayName("없는 슬롯 삭제는 404 SLOT_NOT_FOUND")
    void deleteNotFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.SLOT_NOT_FOUND))
                .given(spaceSlotService).delete(any(), any(), any());

        mockMvc.perform(delete("/api/spaces/1/slots/99").with(TestUsers.host()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SLOT_NOT_FOUND"));
    }
}
