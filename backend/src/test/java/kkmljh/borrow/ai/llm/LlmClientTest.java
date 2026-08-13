package kkmljh.borrow.ai.llm;

import com.anthropic.client.AnthropicClient;
import com.openai.client.OpenAIClient;
import kkmljh.borrow.ai.dto.AnalyzedRequirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("LlmClient 구현체 — 예외 전파 계약")
class LlmClientTest {

    /**
     * SDK 호출 실패는 감싸지 않고 그대로 던진다 — 폴백/에러 처리는 호출부(A-01/A-03) 책임이다.
     * (A-03은 규칙 기반 폴백, A-01은 AI_ANALYSIS_FAILED 로 각각 다르게 대응한다.)
     */
    @Test
    @DisplayName("Anthropic 호출 실패는 호출부로 그대로 전파된다")
    void anthropicPropagatesFailure() {
        AnthropicClient anthropic = mock(AnthropicClient.class);
        given(anthropic.messages()).willThrow(new IllegalStateException("401 invalid api key"));
        LlmClient client = new AnthropicLlmClient(anthropic, "claude-opus-4-8");

        assertThatThrownBy(() -> client.complete("프롬프트", AnalyzedRequirement.class, 2048L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("OpenAI 호환 호출 실패도 그대로 전파된다")
    void openAiPropagatesFailure() {
        OpenAIClient openai = mock(OpenAIClient.class);
        given(openai.chat()).willThrow(new IllegalStateException("429 rate limited"));
        LlmClient client = new OpenAiLlmClient(openai, "gpt-4o-mini");

        assertThatThrownBy(() -> client.complete("프롬프트", AnalyzedRequirement.class, 2048L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("429");
    }
}
