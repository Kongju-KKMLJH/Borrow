package kkmljh.borrow.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig — Swagger Authorize 버튼")
class OpenApiConfigTest {

    private final OpenAPI openAPI = new OpenApiConfig().borrowOpenAPI();

    @Test
    @DisplayName("API 기본 정보가 채워진다")
    void info() {
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Borrow API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getInfo().getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("HTTP Basic 방식의 SecurityScheme 를 등록한다")
    void basicSecurityScheme() {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("basicAuth");

        assertThat(scheme).isNotNull();
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("basic");
    }

    @Test
    @DisplayName("등록한 SecurityScheme 를 전역 요구사항으로 건다 (Authorize 한 번이면 모든 호출에 적용)")
    void globalSecurityRequirement() {
        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getSecurity().get(0)).containsKey("basicAuth");
    }

    @Test
    @DisplayName("폐기된 X-Guest-Id 헤더 파라미터는 더 이상 노출하지 않는다")
    void noGuestIdHeader() {
        assertThat(openAPI.getComponents().getParameters()).isNullOrEmpty();
        assertThat(String.valueOf(openAPI)).doesNotContain("X-Guest-Id");
    }
}
