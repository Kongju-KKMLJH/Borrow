package kkmljh.borrow.common.guest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터(String)에 붙이면 X-Guest-Id 헤더 값이 주입된다.
 * required=false면 헤더가 없을 때 null이 주입된다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuestId {
    boolean required() default true;
}
