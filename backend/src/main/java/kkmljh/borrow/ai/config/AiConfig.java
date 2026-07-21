package kkmljh.borrow.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Anthropic Java SDK 클라이언트 빈.
 *
 * <p>API 키가 없어도 앱은 기동된다(클라이언트 생성 시점엔 검증하지 않음).
 * 실제 호출은 A-01/A-03 서비스에서 이뤄지며, 키가 없으면 그 시점에 실패한다
 * (A-03은 규칙 기반 폴백, A-01은 AI_ANALYSIS_FAILED). B/C가 키 없이 서버를
 * 띄워도 다른 도메인은 정상 동작하도록 하기 위함.
 */
@Configuration
public class AiConfig {

    @Bean
    public AnthropicClient anthropicClient(@Value("${ANTHROPIC_API_KEY:}") String apiKey) {
        String key = (apiKey == null || apiKey.isBlank()) ? "missing-api-key" : apiKey;
        return AnthropicOkHttpClient.builder()
                .apiKey(key)
                .build();
    }
}