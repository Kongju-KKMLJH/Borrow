package kkmljh.borrow.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import kkmljh.borrow.ai.llm.AnthropicLlmClient;
import kkmljh.borrow.ai.llm.LlmClient;
import kkmljh.borrow.ai.llm.OpenAiLlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 클라이언트 빈. {@code ai.provider} 설정으로 Anthropic / OpenAI 호환 중 하나를 서버 기동 시 고정한다.
 *
 * <p>API 키가 없어도 앱은 기동된다(생성 시점엔 검증하지 않음). 실제 호출은 A-01/A-03에서 이뤄지며
 * 키가 없으면 그 시점에 실패한다(A-03은 규칙 기반 폴백, A-01은 AI_ANALYSIS_FAILED).
 * B/C가 키 없이 서버를 띄워도 다른 도메인은 정상 동작하도록 하기 위함.
 *
 * <ul>
 *   <li>{@code ai.provider} — {@code anthropic}(기본) | {@code openai} | {@code gemini}</li>
 *   <li>{@code ai.model} — 비우면 provider 기본값(anthropic=claude-opus-4-8, openai=gpt-4o-mini, gemini=gemini-2.0-flash)</li>
 *   <li>{@code ai.openai.base-url} — OpenAI 호환 엔드포인트. 비우면 api.openai.com. Groq/OpenRouter 등 사용 시 지정</li>
 * </ul>
 *
 * <p>{@code gemini}는 Google의 OpenAI 호환 엔드포인트를 통해 {@link OpenAiLlmClient}를 재사용한다.
 * base-url·API 키(GEMINI_API_KEY)만 자동 세팅되며 별도 SDK는 필요 없다. 무료 티어로 사용 가능.
 */
@Slf4j
@Configuration
public class AiConfig {

    /** Google Gemini의 OpenAI 호환 엔드포인트(structured output 지원). */
    private static final String GEMINI_OPENAI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/openai/";

    @Bean
    public LlmClient llmClient(
            @Value("${ai.provider:anthropic}") String provider,
            @Value("${ai.model:}") String model,
            @Value("${ANTHROPIC_API_KEY:}") String anthropicApiKey,
            @Value("${ai.openai.api-key:${OPENAI_API_KEY:}}") String openAiApiKey,
            @Value("${ai.openai.base-url:${OPENAI_BASE_URL:}}") String openAiBaseUrl,
            @Value("${ai.gemini.api-key:${GEMINI_API_KEY:}}") String geminiApiKey,
            @Value("${ai.gemini.base-url:}") String geminiBaseUrl) {

        String p = provider == null ? "anthropic" : provider.trim().toLowerCase();
        return switch (p) {
            case "openai" -> {
                String m = model.isBlank() ? "gpt-4o-mini" : model.trim();
                log.info("LLM provider=openai model={} baseUrl={}", m,
                        openAiBaseUrl.isBlank() ? "(default: api.openai.com)" : openAiBaseUrl.trim());
                yield new OpenAiLlmClient(openAiClient(openAiApiKey, openAiBaseUrl), m);
            }
            case "gemini" -> {
                String m = model.isBlank() ? "gemini-2.0-flash" : model.trim();
                String url = geminiBaseUrl.isBlank() ? GEMINI_OPENAI_BASE_URL : geminiBaseUrl.trim();
                log.info("LLM provider=gemini model={} baseUrl={}", m, url);
                yield new OpenAiLlmClient(openAiClient(geminiApiKey, url), m);
            }
            case "anthropic" -> {
                String m = model.isBlank() ? "claude-opus-4-8" : model.trim();
                log.info("LLM provider=anthropic model={}", m);
                yield new AnthropicLlmClient(anthropicClient(anthropicApiKey), m);
            }
            default -> throw new IllegalStateException(
                    "알 수 없는 ai.provider: '" + provider + "' (anthropic | openai | gemini 중 하나여야 함)");
        };
    }

    private AnthropicClient anthropicClient(String apiKey) {
        String key = (apiKey == null || apiKey.isBlank()) ? "missing-api-key" : apiKey;
        return AnthropicOkHttpClient.builder().apiKey(key).build();
    }

    private OpenAIClient openAiClient(String apiKey, String baseUrl) {
        String key = (apiKey == null || apiKey.isBlank()) ? "missing-api-key" : apiKey;
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder().apiKey(key);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }
}
