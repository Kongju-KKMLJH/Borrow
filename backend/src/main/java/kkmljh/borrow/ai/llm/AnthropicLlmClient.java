package kkmljh.borrow.ai.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import lombok.RequiredArgsConstructor;

/** Anthropic(Claude) 기반 {@link LlmClient}. 기존 A-01/A-03 동작 그대로. */
@RequiredArgsConstructor
public class AnthropicLlmClient implements LlmClient {

    private final AnthropicClient anthropic;
    private final String model;

    @Override
    public <T> T complete(String prompt, Class<T> schema, long maxTokens) {
        var params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .addUserMessage(prompt)
                .outputConfig(schema)
                .build();

        StructuredMessage<T> message = anthropic.messages().create(params);
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElse(null);
    }
}
