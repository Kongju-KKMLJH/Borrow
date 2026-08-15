package kkmljh.borrow.common.guest;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GuestIdArgumentResolver — X-Guest-Id 헤더 주입/필수 검증")
class GuestIdArgumentResolverTest {

    private final GuestIdArgumentResolver resolver = new GuestIdArgumentResolver();

    private MethodParameter parameterWith(GuestId annotation) {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.getParameterAnnotation(GuestId.class)).thenReturn(annotation);
        return parameter;
    }

    private GuestId annotation(boolean required) {
        GuestId annotation = mock(GuestId.class);
        when(annotation.required()).thenReturn(required);
        return annotation;
    }

    private NativeWebRequest requestWithHeader(String value) {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader(GuestIdArgumentResolver.HEADER_NAME)).thenReturn(value);
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(servletRequest);
        return webRequest;
    }

    @Test
    @DisplayName("supportsParameter: @GuestId + String 타입이면 지원")
    void supportsGuestIdStringParameter() {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.hasParameterAnnotation(GuestId.class)).thenReturn(true);
        when(parameter.getParameterType()).thenReturn((Class) String.class);

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    @DisplayName("supportsParameter: @GuestId가 없으면 미지원")
    void doesNotSupportWithoutAnnotation() {
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.hasParameterAnnotation(GuestId.class)).thenReturn(false);

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    @DisplayName("헤더가 있으면 그 값을 반환한다")
    void resolvesHeaderValue() throws Exception {
        Object result = resolver.resolveArgument(
                parameterWith(annotation(true)), null, requestWithHeader("guest-123"), null);

        assertThat(result).isEqualTo("guest-123");
    }

    @Test
    @DisplayName("required=true인데 헤더가 없으면 GUEST_ID_REQUIRED")
    void throwsWhenRequiredHeaderMissing() {
        assertThatThrownBy(() -> resolver.resolveArgument(
                parameterWith(annotation(true)), null, requestWithHeader(null), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GUEST_ID_REQUIRED);
    }

    @Test
    @DisplayName("required=true인데 헤더가 공백이면 GUEST_ID_REQUIRED")
    void throwsWhenRequiredHeaderBlank() {
        assertThatThrownBy(() -> resolver.resolveArgument(
                parameterWith(annotation(true)), null, requestWithHeader("   "), null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GUEST_ID_REQUIRED);
    }

    @Test
    @DisplayName("required=false면 헤더가 없어도 null 반환")
    void returnsNullWhenOptionalHeaderMissing() throws Exception {
        Object result = resolver.resolveArgument(
                parameterWith(annotation(false)), null, requestWithHeader(null), null);

        assertThat(result).isNull();
    }
}
