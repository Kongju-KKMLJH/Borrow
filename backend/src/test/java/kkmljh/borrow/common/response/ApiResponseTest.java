package kkmljh.borrow.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse — 공통 응답 포맷")
class ApiResponseTest {

    @Test
    @DisplayName("ok(data): success=true, error=null, data 그대로")
    void okWithData() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("ok(): success=true, data·error 모두 null")
    void okEmpty() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isNull();
    }

    @Test
    @DisplayName("error(code, message): success=false, data=null, error 채움")
    void error() {
        ApiResponse<Void> response = ApiResponse.error("ACTIVITY_NOT_FOUND", "활동을 찾을 수 없습니다.");

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isNotNull();
        assertThat(response.error().code()).isEqualTo("ACTIVITY_NOT_FOUND");
        assertThat(response.error().message()).isEqualTo("활동을 찾을 수 없습니다.");
    }
}
