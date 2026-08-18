package kkmljh.borrow.common.storage;

import kkmljh.borrow.common.config.SecurityConfig;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UploadController.class)
@Import({SecurityConfig.class, TestUsers.class})
@DisplayName("POST /api/uploads — 이미지 업로드")
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", "bytes".getBytes());
    }

    @Test
    @DisplayName("로그인하면 업로드할 수 있고 상대 URL 목록을 돌려준다")
    void upload() throws Exception {
        given(fileStorageService.storeAll(any())).willReturn(List.of("/files/a.jpg", "/files/b.jpg"));

        mockMvc.perform(multipart("/api/uploads")
                        .file(file("a.jpg"))
                        .file(file("b.jpg"))
                        .with(TestUsers.member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.urls[0]").value("/files/a.jpg"))
                .andExpect(jsonPath("$.data.urls[1]").value("/files/b.jpg"));
    }

    @Test
    @DisplayName("역할과 무관하게 로그인한 누구나 업로드할 수 있다 (공간·활동 공용)")
    void uploadAllowedForEveryRole() throws Exception {
        given(fileStorageService.storeAll(any())).willReturn(List.of("/files/a.jpg"));

        mockMvc.perform(multipart("/api/uploads").file(file("a.jpg")).with(TestUsers.host()))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/uploads").file(file("a.jpg")).with(TestUsers.artist()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비로그인 업로드는 401 UNAUTHORIZED")
    void uploadRequiresLogin() throws Exception {
        mockMvc.perform(multipart("/api/uploads").file(file("a.jpg")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("이미지가 아니면 400 UNSUPPORTED_FILE_TYPE")
    void unsupportedType() throws Exception {
        given(fileStorageService.storeAll(any()))
                .willThrow(new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE));

        mockMvc.perform(multipart("/api/uploads")
                        .file(new MockMultipartFile("files", "doc.pdf", "application/pdf", "bytes".getBytes()))
                        .with(TestUsers.member()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
    }

    @Test
    @DisplayName("files 파트가 없으면 400 INVALID_REQUEST 가 나간다 (#8·#30 회귀)")
    void missingFilePart() throws Exception {
        // MissingServletRequestPartException 전용 핸들러가 없던 시절에는
        // @ExceptionHandler(Exception.class) 가 통째로 잡아 500이 나갔다.
        mockMvc.perform(multipart("/api/uploads").with(TestUsers.member()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}
