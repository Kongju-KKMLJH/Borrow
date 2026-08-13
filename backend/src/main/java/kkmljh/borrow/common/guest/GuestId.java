package kkmljh.borrow.common.guest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터(String)에 붙이면 <b>로그인한 회원의 아이디</b>가 주입된다.
 * required=false면 비로그인일 때 null이 주입된다(비로그인 열람 허용 API 전용).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuestId {
    boolean required() default true;
}
