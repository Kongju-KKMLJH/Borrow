package kkmljh.borrow.common.config;

import kkmljh.borrow.common.guest.GuestIdArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebConfig — @GuestId 리졸버 · 업로드 정적 서빙")
class WebConfigTest {

    @TempDir
    Path uploadDir;

    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "uploadDir", uploadDir.toString());
        ReflectionTestUtils.setField(webConfig, "urlPrefix", "/files");
    }

    @Test
    @DisplayName("@GuestId 리졸버를 등록한다 — 이게 빠지면 컨트롤러가 로그인 아이디를 못 받는다")
    void registersGuestIdResolver() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        webConfig.addArgumentResolvers(resolvers);

        assertThat(resolvers).hasAtLeastOneElementOfType(GuestIdArgumentResolver.class);
    }

    @Test
    @DisplayName("업로드 이미지를 설정한 URL 접두어로 정적 서빙한다")
    void registersResourceHandler() {
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
                new org.springframework.web.context.support.StaticWebApplicationContext(), null);

        webConfig.addResourceHandlers(registry);

        List<?> registrations = (List<?>) ReflectionTestUtils.getField(registry, "registrations");
        assertThat(registrations).hasSize(1);
        assertThat((String[]) ReflectionTestUtils.getField(registrations.get(0), "pathPatterns"))
                .containsExactly("/files/**");
    }

    @Test
    @DisplayName("CORS 는 WebMvc 가 아니라 SecurityConfig 의 빈이 담당한다 (프리플라이트 401 방지)")
    void corsIsNotConfiguredHere() {
        var registry = new org.springframework.web.servlet.config.annotation.CorsRegistry();

        webConfig.addCorsMappings(registry);

        assertThat(ReflectionTestUtils.getField(registry, "registrations"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .isEmpty();
    }
}
