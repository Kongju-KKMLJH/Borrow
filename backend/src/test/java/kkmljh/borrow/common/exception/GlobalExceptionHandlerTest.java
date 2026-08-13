package kkmljh.borrow.common.exception;

import kkmljh.borrow.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** MethodArgumentNotValidException 을 만들려면 실제 MethodParameter 가 필요하다. */
    @SuppressWarnings("unused")
    private void dummy(String value) {
    }

    private MethodParameter dummyParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class);
        return new MethodParameter(method, 0);
    }

    @Test
    @DisplayName("BusinessException 은 코드의 상태값과 이름·메시지로 변환된다")
    void handleBusiness() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBusiness(new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("ACTIVITY_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo("활동을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("커스텀 메시지를 준 BusinessException 은 그 메시지를 그대로 내보낸다")
    void handleBusinessWithCustomMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.INVALID_REQUEST, "개설자는 자신의 활동에 참여할 수 없습니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().error().message()).isEqualTo("개설자는 자신의 활동에 참여할 수 없습니다.");
    }

    @Test
    @DisplayName("검증 실패는 400 INVALID_REQUEST 와 '필드명: 사유' 메시지")
    void handleValidation() throws Exception {
        BindingResult withField = new BeanPropertyBindingResult(new Object(), "request");
        withField.addError(new org.springframework.validation.FieldError(
                "request", "loginId", "로그인 아이디는 필수입니다."));

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleValidation(new MethodArgumentNotValidException(dummyParameter(), withField));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().error().message()).isEqualTo("loginId: 로그인 아이디는 필수입니다.");
    }

    @Test
    @DisplayName("필드 오류가 없으면 기본 메시지로 대체된다")
    void handleValidationWithoutFieldError() throws Exception {
        BindingResult empty = new BeanPropertyBindingResult(new Object(), "request");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleValidation(new MethodArgumentNotValidException(dummyParameter(), empty));

        assertThat(response.getBody().error().message()).isEqualTo(ErrorCode.INVALID_REQUEST.getMessage());
    }

    @Test
    @DisplayName("해석 불가한 요청 본문은 400 INVALID_REQUEST")
    void handleUnreadable() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnreadable(new HttpMessageNotReadableException(
                        "broken", new MockHttpInputMessage("{".getBytes())));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().error().message()).contains("요청 본문");
    }

    @Test
    @DisplayName("예상치 못한 예외는 500 INTERNAL_ERROR 로 감싸고 내부 메시지를 노출하지 않는다")
    void handleUnknown() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnknown(new IllegalStateException("DB password is 1234"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().error().message())
                .isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage())
                .doesNotContain("1234");
    }
}
