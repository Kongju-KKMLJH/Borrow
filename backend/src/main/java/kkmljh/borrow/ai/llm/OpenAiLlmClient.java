package kkmljh.borrow.ai.llm;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import lombok.RequiredArgsConstructor;

/**
 * OpenAI 호환 API 기반 {@link LlmClient}.
 *
 * <p>baseUrl만 바꾸면 OpenAI 외에 Groq·Google Gemini·OpenRouter·Ollama 등
 * OpenAI 호환 엔드포인트를 그대로 쓸 수 있다(무료/저가 모델 사용 목적).
 * structured output은 OpenAI의 json_schema(strict) 형식을 사용하므로,
 * 엔드포인트가 json_schema 응답을 지원해야 한다.
 */
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final OpenAIClient openai;
    private final String model;

    @Override
    public <T> T complete(String prompt, Class<T> schema, long maxTokens) {
        var params = ChatCompletionCreateParams.builder()
                .model(model)
                .maxCompletionTokens(maxTokens)
                .addUserMessage(prompt)
                .responseFormat(schema)
                .build();

        StructuredChatCompletion<T> completion = openai.chat().completions().create(params);
        return completion.choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .findFirst()
                .orElse(null);
    }
}
