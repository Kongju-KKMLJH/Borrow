package kkmljh.borrow.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 전역 설정.
 * - API 기본 정보(제목/설명/버전)
 * - HTTP Basic SecurityScheme → 우측 상단 Authorize 버튼으로 아이디/비밀번호 입력
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI borrowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Borrow API")
                        .version("v1")
                        .description("""
                                천안 유휴공간 대여 서비스 백엔드 API.

                                - 인증은 HTTP Basic. 매 요청에 `Authorization: Basic base64(아이디:비밀번호)` 를 보낸다.
                                  Swagger 우측 상단 **Authorize** 버튼에 아이디/비밀번호를 넣으면 이후 호출에 자동 적용된다.
                                - 회원 유형: MEMBER(일반 회원) / HOST(공간 제공자) / ARTIST(예술가). 가입 시 하나만 고른다.
                                - 모든 응답은 `{ success, data, error }` 형태의 공통 포맷으로 감싸진다.
                                - 요약의 U-xx / B-xx / A-xx 는 기능명세 코드다."""))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("로그인 아이디 / 비밀번호")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
