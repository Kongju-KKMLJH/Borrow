package kkmljh.borrow.common.guest;

import jakarta.servlet.http.HttpServletRequest;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class GuestIdArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER_NAME = "X-Guest-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(GuestId.class)
                && String.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String guestId = request != null ? request.getHeader(HEADER_NAME) : null;

        GuestId annotation = parameter.getParameterAnnotation(GuestId.class);
        if ((guestId == null || guestId.isBlank()) && annotation != null && annotation.required()) {
            throw new BusinessException(ErrorCode.GUEST_ID_REQUIRED);
        }
        return guestId;
    }
}
