package kkmljh.borrow.ai.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Anthropic(Claude) 기반 {@link LlmClient}. 기존 A-01/A-03 동작 그대로. */
@Slf4j
@RequiredArgsConstructor
public class AnthropicLlmClient implements LlmClient {

    private final AnthropicClient anthropic;
    private final String model;

    @Override
    public <T> T complete(String prompt, Class<T> schema, long maxTokens) {
        log.info("[LLM] Anthropic 호출 시작 — model={} schema={} maxTokens={} promptLen={}",
                model, schema.getSimpleName(), maxTokens, prompt.length());
        long startedAt = System.nanoTime();
        try {
            var params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(maxTokens)
                    .addUserMessage(prompt)
                    .outputConfig(schema)
                    .build();

            StructuredMessage<T> message = anthropic.messages().create(params);
            T result = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .findFirst()
                    .orElse(null);

            long ms = (System.nanoTime() - startedAt) / 1_000_000;
            if (result == null) {
                log.warn("[LLM] Anthropic 응답에 구조화 컨텐츠 없음 — model={} elapsed={}ms (null 반환)", model, ms);
            } else {
                log.info("[LLM] Anthropic 호출 성공 — model={} elapsed={}ms schema={}",
                        model, ms, schema.getSimpleName());
            }
            return result;
        } catch (Exception e) {
            long ms = (System.nanoTime() - startedAt) / 1_000_000;
            log.error("[LLM] Anthropic 호출 실패 — model={} elapsed={}ms cause={}: {}",
                    model, ms, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
