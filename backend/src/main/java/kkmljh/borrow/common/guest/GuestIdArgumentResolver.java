package kkmljh.borrow.common.guest;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @GuestId} 파라미터에 <b>로그인 아이디</b>를 주입한다.
 * 값의 출처만 헤더(X-Guest-Id) → SecurityContext 로 바뀌었고,
 * 서비스의 소유권 비교 로직과 컨트롤러 시그니처는 그대로 둔다.
 */
public class GuestIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(GuestId.class)
                && String.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        GuestId annotation = parameter.getParameterAnnotation(GuestId.class);
        boolean required = annotation == null || annotation.required();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            // 필터의 entryPoint와 같은 401로 통일한다 (400 GUEST_ID_REQUIRED 아님).
            if (required) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return null; // 비로그인 열람 허용 API는 기존대로 null
        }
        return auth.getName();
    }
}
