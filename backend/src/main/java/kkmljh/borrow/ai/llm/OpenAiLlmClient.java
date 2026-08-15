package kkmljh.borrow.ai.llm;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI 호환 API 기반 {@link LlmClient}.
 *
 * <p>baseUrl만 바꾸면 OpenAI 외에 Groq·Google Gemini·OpenRouter·Ollama 등
 * OpenAI 호환 엔드포인트를 그대로 쓸 수 있다(무료/저가 모델 사용 목적).
 * structured output은 OpenAI의 json_schema(strict) 형식을 사용하므로,
 * 엔드포인트가 json_schema 응답을 지원해야 한다.
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final OpenAIClient openai;
    private final String model;

    @Override
    public <T> T complete(String prompt, Class<T> schema, long maxTokens) {
        log.info("[LLM] OpenAI 호출 시작 — model={} schema={} maxTokens={} promptLen={}",
                model, schema.getSimpleName(), maxTokens, prompt.length());
        long startedAt = System.nanoTime();
        try {
            var params = ChatCompletionCreateParams.builder()
                    .model(model)
                    .maxCompletionTokens(maxTokens)
                    .addUserMessage(prompt)
                    .responseFormat(schema)
                    .build();

            StructuredChatCompletion<T> completion = openai.chat().completions().create(params);
            T result = completion.choices().stream()
                    .flatMap(choice -> choice.message().content().stream())
                    .findFirst()
                    .orElse(null);

            long ms = (System.nanoTime() - startedAt) / 1_000_000;
            if (result == null) {
                log.warn("[LLM] OpenAI 응답에 구조화 컨텐츠 없음 — model={} elapsed={}ms (null 반환)", model, ms);
            } else {
                log.info("[LLM] OpenAI 호출 성공 — model={} elapsed={}ms schema={}",
                        model, ms, schema.getSimpleName());
            }
            return result;
        } catch (Exception e) {
            long ms = (System.nanoTime() - startedAt) / 1_000_000;
            log.error("[LLM] OpenAI 호출 실패 — model={} elapsed={}ms cause={}: {}",
                    model, ms, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
