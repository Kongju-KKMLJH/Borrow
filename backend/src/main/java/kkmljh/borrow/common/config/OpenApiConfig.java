package kkmljh.borrow.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import kkmljh.borrow.common.guest.GuestId;
import kkmljh.borrow.common.guest.GuestIdArgumentResolver;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

/**
 * Swagger(OpenAPI) 전역 설정.
 * - API 기본 정보(제목/설명/버전)
 * - {@code @GuestId} 파라미터가 붙은 API에 X-Guest-Id 헤더를 자동 노출 (컨트롤러 파라미터를 건드리지 않음)
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI borrowOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Borrow API")
                .version("v1")
                .description("""
                        천안 유휴공간 대여 서비스 백엔드 API.

                        - 로그인 없음. 사용자 식별은 프론트가 생성한 UUID를 `X-Guest-Id` 헤더로 전달한다.
                        - 모든 응답은 `{ success, data, error }` 형태의 공통 포맷으로 감싸진다.
                        - 요약의 U-xx / B-xx / A-xx 는 기능명세 코드다."""));
    }

    /**
     * 컨트롤러 파라미터에 {@code @GuestId}가 있으면 해당 API에 X-Guest-Id 헤더 파라미터를 추가한다.
     * required 여부는 어노테이션 설정을 그대로 따른다.
     */
    @Bean
    public OperationCustomizer guestIdHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            for (MethodParameter param : handlerMethod.getMethodParameters()) {
                GuestId guestId = param.getParameterAnnotation(GuestId.class);
                if (guestId != null) {
                    operation.addParametersItem(new Parameter()
                            .in("header")
                            .name(GuestIdArgumentResolver.HEADER_NAME)
                            .required(guestId.required())
                            .schema(new StringSchema())
                            .description("게스트 식별 UUID (프론트에서 생성해 보관)")
                            .example("11111111-1111-1111-1111-111111111111"));
                }
            }
            return operation;
        };
    }
}
