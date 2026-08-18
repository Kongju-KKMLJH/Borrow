package kkmljh.borrow.ai.config;

import kkmljh.borrow.ai.llm.AnthropicLlmClient;
import kkmljh.borrow.ai.llm.LlmClient;
import kkmljh.borrow.ai.llm.OpenAiLlmClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AiConfig — provider 별 LLM 클라이언트 생성")
class AiConfigTest {

    private final AiConfig config = new AiConfig();

    private LlmClient client(String provider, String model) {
        return config.llmClient(provider, model, "", "", "", "", "");
    }

    @Test
    @DisplayName("기본 provider 는 anthropic")
    void defaultsToAnthropic() {
        assertThat(client("anthropic", "")).isInstanceOf(AnthropicLlmClient.class);
        assertThat(client(null, "")).isInstanceOf(AnthropicLlmClient.class);
    }

    @Test
    @DisplayName("openai 와 gemini 는 OpenAI 호환 클라이언트를 쓴다")
    void openAiCompatibleProviders() {
        assertThat(client("openai", "")).isInstanceOf(OpenAiLlmClient.class);
        assertThat(client("gemini", "")).isInstanceOf(OpenAiLlmClient.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ANTHROPIC", "  anthropic  ", "OpenAI", "GEMINI"})
    @DisplayName("provider 이름은 대소문자·공백을 가리지 않는다")
    void providerIsCaseAndSpaceInsensitive(String provider) {
        assertThat(client(provider, "")).isNotNull();
    }

    @Test
    @DisplayName("모델을 지정하면 그 값을 쓴다 (빈 값이면 provider 기본 모델)")
    void modelOverride() {
        assertThat(client("anthropic", "claude-opus-4-8")).isInstanceOf(AnthropicLlmClient.class);
        assertThat(client("openai", "gpt-4o")).isInstanceOf(OpenAiLlmClient.class);
    }

    @Test
    @DisplayName("API 키가 없어도 빈은 만들어진다 — 키 없이도 서버가 뜨고 다른 도메인은 동작해야 한다")
    void missingApiKeyDoesNotFailStartup() {
        assertThat(client("anthropic", "")).isNotNull();
        assertThat(client("openai", "")).isNotNull();
    }

    @Test
    @DisplayName("알 수 없는 provider 는 기동 시점에 바로 실패한다")
    void unknownProvider() {
        assertThatThrownBy(() -> client("bedrock", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bedrock");
    }
}
