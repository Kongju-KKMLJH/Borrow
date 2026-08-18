package kkmljh.borrow.common.config;

import jakarta.servlet.http.HttpServletResponse;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * 인증(HTTP Basic) · 인가(경로별 권한) 설정. 권한 규칙은 이 한 곳에만 모은다.
 *
 * <p>규칙은 <b>위에서부터 먼저 매칭되는 것이 이긴다.</b> 비로그인 GET 줄이
 * {@code /api/spaces/**} HOST 줄보다 반드시 위에 있어야 목록 조회가 막히지 않는다.
 *
 * <p>Spring Security 7 은 람다 DSL만 지원한다({@code .and()} 체이닝 불가).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_DOCS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
    };

    /** Spring Boot 4.1 이 자동 구성하는 것은 Jackson 3({@code tools.jackson}) 쪽이다. */
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST + Basic 이므로 CSRF 토큰이 의미 없다. 세션도 만들지 않는다.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CorsConfigurationSource 빈을 읽어 필터체인에서 프리플라이트를 통과시킨다.
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // --- 누구나(비로그인) ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // CORS 프리플라이트
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/activities", "/api/activities/{activityId}").permitAll()
                        // "/mine" 은 {spaceId} 패턴에 삼켜지므로 공개 GET 줄보다 먼저 막는다.
                        .requestMatchers(HttpMethod.GET, "/api/spaces/mine").hasRole("HOST")
                        .requestMatchers(HttpMethod.GET,
                                "/api/spaces", "/api/spaces/{spaceId}", "/api/spaces/{spaceId}/slots").permitAll()
                        .requestMatchers("/files/**", "/error").permitAll()
                        .requestMatchers(PUBLIC_DOCS).permitAll()

                        // --- 로그인 전체(역할 무관) ---
                        // HOST 계정도 남의 활동에 참여할 수 있어야 하므로 역할로 가르지 않는다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/activities/{activityId}/participations").authenticated()
                        .requestMatchers("/api/me/**", "/api/ai/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/uploads").authenticated()

                        // --- 활동 개설·관리: MEMBER, ARTIST ---
                        // ARTIST 전용 엔드포인트는 없다. 같은 API를 쓰고 서버가 역할을 보고 CLASS로 분기한다.
                        .requestMatchers(HttpMethod.POST, "/api/activities").hasAnyRole("MEMBER", "ARTIST")
                        .requestMatchers(HttpMethod.PATCH, "/api/activities/{activityId}/requirement")
                        .hasAnyRole("MEMBER", "ARTIST")
                        .requestMatchers("/api/activities/{activityId}/hosting-request")
                        .hasAnyRole("MEMBER", "ARTIST")

                        // --- 공간 운영: HOST ---
                        .requestMatchers("/api/spaces/**").hasRole("HOST")
                        .requestMatchers("/api/host/**").hasRole("HOST")

                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint()))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    /** 해커톤용 전체 허용 (Expo 앱/웹/LAN). WebConfig가 아니라 여기 빈으로 둬야 필터가 읽는다. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        // Basic 은 Authorization 헤더를 직접 실어 보내므로 쿠키 자격증명은 필요 없다.
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 401. 커스텀 entryPoint 라서 WWW-Authenticate 를 안 내보내고,
     * 그 덕에 브라우저의 Basic 인증 팝업도 뜨지 않는다.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, e) -> writeError(response, ErrorCode.UNAUTHORIZED);
    }

    /** 403 — 로그인은 했지만 역할이 맞지 않는 경우 */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, e) -> writeError(response, ErrorCode.FORBIDDEN);
    }

    /**
     * 401/403 은 컨트롤러 도달 전 필터에서 나므로 GlobalExceptionHandler가 못 잡는다.
     * 프론트가 파싱하는 {success, data, error} 포맷을 여기서 직접 맞춰준다.
     * 코드명·메시지·상태값은 ErrorCode enum에서만 가져온다(리터럴 금지).
     */
    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code.name(), code.getMessage()));
    }
}
