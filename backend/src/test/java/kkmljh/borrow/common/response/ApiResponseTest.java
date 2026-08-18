package kkmljh.borrow.common.response;

import kkmljh.borrow.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse 공통 응답 포맷")
class ApiResponseTest {

    @Test
    @DisplayName("ok(data) 는 success=true, error=null")
    void okWithData() {
        ApiResponse<String> response = ApiResponse.ok("결과");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("결과");
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("본문 없는 ok() 는 data 도 null")
    void okWithoutData() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("빈 목록도 그대로 감싼다 (null 로 바꾸지 않는다)")
    void okWithEmptyList() {
        ApiResponse<List<String>> response = ApiResponse.ok(List.of());

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNotNull().asInstanceOf(
                org.assertj.core.api.InstanceOfAssertFactories.LIST).isEmpty();
    }

    @Test
    @DisplayName("error(code, message) 는 success=false, data=null")
    void error() {
        ApiResponse<Void> response = ApiResponse.error("ACTIVITY_NOT_FOUND", "활동을 찾을 수 없습니다.");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isEqualTo(
                new ApiResponse.ErrorBody("ACTIVITY_NOT_FOUND", "활동을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("ErrorCode 의 이름·메시지를 그대로 실어 보낼 수 있다 (401/403 포맷)")
    void errorFromErrorCode() {
        ErrorCode code = ErrorCode.UNAUTHORIZED;

        ApiResponse<Void> response = ApiResponse.error(code.name(), code.getMessage());

        assertThat(response.error().code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.error().message()).isEqualTo("로그인이 필요합니다.");
    }
}
