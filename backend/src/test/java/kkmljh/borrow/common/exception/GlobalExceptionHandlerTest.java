package kkmljh.borrow.common.exception;

import kkmljh.borrow.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler — 예외 → 표준 에러 응답 매핑")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException은 ErrorCode의 status와 code/message로 매핑된다")
    void handleBusiness() {
        BusinessException ex = new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("ACTIVITY_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("BusinessException의 커스텀 메시지가 그대로 전달된다")
    void handleBusinessWithCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시각은 시작 시각보다 늦어야 합니다.");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().error().message()).isEqualTo("종료 시각은 시작 시각보다 늦어야 합니다.");
    }

    @Test
    @DisplayName("검증 실패(MethodArgumentNotValid)는 400 INVALID_REQUEST + 첫 필드 메시지")
    void handleValidation() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("req", "headcount", "수용 인원은 1명 이상이어야 합니다.")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().error().message()).isEqualTo("headcount: 수용 인원은 1명 이상이어야 합니다.");
    }

    @Test
    @DisplayName("본문 파싱 실패(HttpMessageNotReadable)는 400 INVALID_REQUEST")
    void handleUnreadable() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    @DisplayName("미처리 예외는 500 INTERNAL_ERROR로 감싼다")
    void handleUnknown() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnknown(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
    }
}
