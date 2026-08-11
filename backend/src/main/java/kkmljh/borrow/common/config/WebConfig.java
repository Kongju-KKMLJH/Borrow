package kkmljh.borrow.common.config;

import kkmljh.borrow.common.guest.GuestIdArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/files}")
    private String urlPrefix;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new GuestIdArgumentResolver());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 이미지 정적 서빙: /files/** → 로컬 디스크의 업로드 디렉터리
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/"; // 디렉터리가 아직 없으면 toUri()가 슬래시를 안 붙이므로 보정
        }
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location);
    }

    // CORS 설정은 SecurityConfig 의 CorsConfigurationSource 빈으로 옮겼다.
    // WebMvc의 addCorsMappings는 Security 필터체인에 적용되지 않아 프리플라이트가 401로 막힌다.
}
