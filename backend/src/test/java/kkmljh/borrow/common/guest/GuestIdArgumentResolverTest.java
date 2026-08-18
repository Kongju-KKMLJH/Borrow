package kkmljh.borrow.common.guest;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GuestIdArgumentResolver — @GuestId 에 로그인 아이디 주입")
class GuestIdArgumentResolverTest {

    private final GuestIdArgumentResolver resolver = new GuestIdArgumentResolver();

    /** 리졸버 판정 대상이 되는 파라미터 조합들 */
    @SuppressWarnings("unused")
    static class Handler {
        void required(@GuestId String guestId) {
        }

        void optional(@GuestId(required = false) String guestId) {
        }

        void notAnnotated(String guestId) {
        }

        void wrongType(@GuestId Long guestId) {
        }
    }

    private MethodParameter parameterOf(String methodName, Class<?> type) throws NoSuchMethodException {
        Method method = Handler.class.getDeclaredMethod(methodName, type);
        return new MethodParameter(method, 0);
    }

    private Object resolve(MethodParameter parameter) {
        return resolver.resolveArgument(parameter, null, null, null);
    }

    private void login(String loginId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginId, "n/a",
                        AuthorityUtils.createAuthorityList("ROLE_MEMBER")));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("@GuestId 가 붙은 String 파라미터만 지원한다")
    void supportsParameter() throws Exception {
        assertThat(resolver.supportsParameter(parameterOf("required", String.class))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("optional", String.class))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("notAnnotated", String.class))).isFalse();
        assertThat(resolver.supportsParameter(parameterOf("wrongType", Long.class))).isFalse();
    }

    @Test
    @DisplayName("로그인 상태면 SecurityContext 의 로그인 아이디를 주입한다")
    void injectsLoginId() throws Exception {
        login("hong");

        assertThat(resolve(parameterOf("required", String.class))).isEqualTo("hong");
    }

    @Test
    @DisplayName("required=false 도 로그인 상태면 아이디를 준다")
    void optionalInjectsLoginIdWhenAuthenticated() throws Exception {
        login("hong");

        assertThat(resolve(parameterOf("optional", String.class))).isEqualTo("hong");
    }

    @Test
    @DisplayName("인증 정보가 없으면 401 UNAUTHORIZED — 400 GUEST_ID_REQUIRED 가 아니다")
    void throwsUnauthorizedWhenNoAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        MethodParameter parameter = parameterOf("required", String.class);

        assertThatThrownBy(() -> resolve(parameter))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("익명 인증(비로그인)도 401 UNAUTHORIZED")
    void throwsUnauthorizedWhenAnonymous() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        MethodParameter parameter = parameterOf("required", String.class);

        assertThatThrownBy(() -> resolve(parameter))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("인증되지 않은 토큰도 401 UNAUTHORIZED")
    void throwsUnauthorizedWhenNotAuthenticated() throws Exception {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("hong", "pw"); // authenticated=false
        SecurityContextHolder.getContext().setAuthentication(token);
        MethodParameter parameter = parameterOf("required", String.class);

        assertThatThrownBy(() -> resolve(parameter))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("required=false 이면 비로그인일 때 null 을 준다 (비로그인 열람 허용 API)")
    void optionalReturnsNullWhenAnonymous() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(resolve(parameterOf("optional", String.class))).isNull();
    }

    @Test
    @DisplayName("required=false 이면 인증 정보 자체가 없어도 null")
    void optionalReturnsNullWhenNoAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        assertThat(resolve(parameterOf("optional", String.class))).isNull();
    }

    @Test
    @DisplayName("주입되는 값은 역할이 아니라 로그인 아이디다 (소유권 비교의 기준값)")
    void injectsLoginIdNotAuthorities() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("host1", "n/a",
                        AuthorityUtils.createAuthorityList(List.of("ROLE_HOST").toArray(new String[0]))));

        assertThat(resolve(parameterOf("required", String.class))).isEqualTo("host1");
    }
}
