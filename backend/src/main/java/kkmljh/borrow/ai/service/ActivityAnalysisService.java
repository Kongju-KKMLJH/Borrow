package kkmljh.borrow.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import kkmljh.borrow.ai.dto.AnalyzeRequest;
import kkmljh.borrow.ai.dto.AnalyzedRequirement;
import kkmljh.borrow.ai.dto.RequirementResponse;
import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A-01: 활동 설명 → 구조화 공간 요구조건.
 * Claude 1회 호출(structured output)로 분야/인원/시설/소음·오염을 추출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityAnalysisService {

    private static final String MODEL = "claude-opus-4-8";

    private final AnthropicClient anthropic;

    public RequirementResponse analyze(AnalyzeRequest request) {
        String prompt = """
                다음은 사용자가 개설하려는 취미 모임(그림 또는 촬영 분야) 설명이다.
                이 설명을 읽고 활동에 적합한 공간의 요구조건을 구조화해서 채워라.

                - field: 그림/드로잉/미술이면 ART, 사진/영상 촬영이면 PHOTO.
                - headcount: 예상 참여 인원. 명시가 없으면 활동 성격에 맞게 합리적으로 추정(보통 4~8명).
                - requiredFacilities: 활동에 꼭 필요한 시설만. 예) 수채화는 WATER, 실내 촬영은 LIGHTING, 장비는 OUTLET.
                - noisy: 소음이 크게 나는 활동인지.
                - messy: 물감·흙·음식물 등으로 공간이 더러워질 수 있는 활동인지.

                [활동 설명]
                %s
                """.formatted(request.description());

        try {
            MessageCreateParams.Builder base = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(512L)
                    .addUserMessage(prompt);

            StructuredMessage<AnalyzedRequirement> message =
                    anthropic.messages().create(base.outputConfig(AnalyzedRequirement.class).build());

            AnalyzedRequirement analyzed = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.AI_ANALYSIS_FAILED));

            return new RequirementResponse(
                    request.region() == null ? "" : request.region().trim(),
                    Math.max(analyzed.headcount(), 1),
                    analyzed.requiredFacilities() == null ? List.of() : analyzed.requiredFacilities(),
                    analyzed.noisy(),
                    analyzed.messy(),
                    analyzed.field()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("A-01 활동 분석 실패", e);
            throw new BusinessException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }
}