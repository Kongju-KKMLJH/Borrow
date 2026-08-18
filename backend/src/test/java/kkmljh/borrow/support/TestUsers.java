package kkmljh.borrow.support;

import kkmljh.borrow.domain.Role;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * 컨트롤러 테스트용 로그인 계정.
 *
 * <p>{@code SecurityConfig} 는 세션을 만들지 않으므로(STATELESS) {@code @WithMockUser} 의
 * SecurityContext 주입이 필터체인까지 전달되지 않는다. 그래서 실제 운영과 동일하게
 * <b>매 요청 Authorization: Basic 헤더</b>를 실어 인증한다 — 인가 규칙·401/403 포맷이 실제로 검증된다.
 */
@TestConfiguration
public class TestUsers {

    public static final String PASSWORD = "pw1234";

    public static final String MEMBER = "member1";
    public static final String HOST = "host1";
    public static final String ARTIST = "artist1";

    /** 일반 회원(MEMBER)으로 인증한다. */
    public static RequestPostProcessor member() {
        return httpBasic(MEMBER, PASSWORD);
    }

    /** 공간 제공자(HOST)로 인증한다. */
    public static RequestPostProcessor host() {
        return httpBasic(HOST, PASSWORD);
    }

    /** 예술가(ARTIST)로 인증한다. */
    public static RequestPostProcessor artist() {
        return httpBasic(ARTIST, PASSWORD);
    }

    /** 임의의 로그인 아이디로 인증한다(소유권 검증 테스트에서 "남의 계정" 역할). */
    public static RequestPostProcessor as(String loginId) {
        return httpBasic(loginId, PASSWORD);
    }

    @Bean
    public UserDetailsService testUserDetailsService(PasswordEncoder encoder) {
        String encoded = encoder.encode(PASSWORD);
        return new InMemoryUserDetailsManager(
                User.withUsername(MEMBER).password(encoded).authorities(Role.MEMBER.authority()).build(),
                User.withUsername(HOST).password(encoded).authorities(Role.HOST.authority()).build(),
                User.withUsername(ARTIST).password(encoded).authorities(Role.ARTIST.authority()).build(),
                // 소유권(내 것인지) 검증용 — 역할은 있지만 남의 리소스에 접근하는 계정
                User.withUsername("other-host").password(encoded).authorities(Role.HOST.authority()).build(),
                User.withUsername("other-member").password(encoded).authorities(Role.MEMBER.authority()).build()
        );
    }
}
