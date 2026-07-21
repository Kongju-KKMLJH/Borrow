package kkmljh.borrow.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kkmljh.borrow.ai.dto.MatchRequest;
import kkmljh.borrow.ai.dto.SpaceMatchResponse;
import kkmljh.borrow.ai.dto.SpaceScores;
import kkmljh.borrow.ai.repository.SpaceMatchRepository;
import kkmljh.borrow.ai.repository.SpaceSlotMatchRepository;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A-02(하드 필터) → A-03(적합도 점수) → A-04(대체 추천).
 *
 * <ul>
 *   <li>A-02: JPA 하드 필터 — 지역/수용인원/허용분야(DB) + 슬롯 시간 겹침/소음·오염(자바). LLM 아님.</li>
 *   <li>A-03: 필터 통과 후보를 Claude 1회 호출로 점수+이유 산출. <b>API 실패 시 규칙 기반 폴백</b>.</li>
 *   <li>A-04: {@code excludeSpaceIds}로 거절 공간을 제외하고 A-02+A-03 재실행(같은 진입점).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceMatchingService {

    private static final String MODEL = "claude-opus-4-8";
    private static final int MAX_CANDIDATES = 12;

    private final SpaceMatchRepository spaceRepo;
    private final SpaceSlotMatchRepository slotRepo;
    private final AnthropicClient anthropic;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SpaceMatchResponse> match(MatchRequest request) {
        List<Space> candidates = hardFilter(request);          // A-02 (+ A-04 제외 반영)
        if (candidates.isEmpty()) {
            return SpaceMatchResponse.emptyList();
        }
        return score(candidates, request);                     // A-03
    }

    /** A-02: 하드 필터. 통과한 후보를 최대 {@link #MAX_CANDIDATES}개까지 반환. */
    private List<Space> hardFilter(MatchRequest req) {
        Set<Long> excluded = Set.copyOf(req.excludeSpaceIdsOrEmpty());

        List<Space> byQuery = spaceRepo.findCandidates(req.headcount(), req.regionOrEmpty(), req.field());

        List<Space> filtered = byQuery.stream()
                .filter(s -> !excluded.contains(s.getId()))
                // 소음·오염 제한 (B-04 제한 조건)
                .filter(s -> !req.noisy() || s.isNoiseAllowed())
                .filter(s -> !req.messy() || s.isMessAllowed())
                .toList();

        if (filtered.isEmpty()) {
            return List.of();
        }

        // 슬롯 시간 겹침: 활동 날짜의 요일에 [startTime, endTime]을 완전히 포함하는 슬롯이 있어야 함
        DayOfWeek day = req.date().getDayOfWeek();
        Set<Long> ids = filtered.stream().map(Space::getId).collect(Collectors.toSet());
        Map<Long, List<SpaceSlot>> slotsBySpace = slotRepo.findBySpaceIdsAndDay(ids, day).stream()
                .collect(Collectors.groupingBy(sl -> sl.getSpace().getId()));

        return filtered.stream()
                .filter(s -> slotsBySpace.getOrDefault(s.getId(), List.of()).stream()
                        .anyMatch(sl -> sl.covers(day, req.startTime(), req.endTime())))
                .limit(MAX_CANDIDATES)
                .toList();
    }

    /** A-03: Claude로 점수 산출, 실패 시 규칙 기반 폴백. 점수 내림차순 정렬. */
    private List<SpaceMatchResponse> score(List<Space> candidates, MatchRequest req) {
        Map<Long, SpaceScores.SpaceScore> aiScores = tryAiScores(candidates, req);

        return candidates.stream()
                .map(s -> {
                    SpaceScores.SpaceScore ai = aiScores.get(s.getId());
                    if (ai != null) {
                        int bounded = Math.max(0, Math.min(100, ai.score()));
                        return SpaceMatchResponse.of(s, bounded, ai.reason(), true);
                    }
                    return SpaceMatchResponse.of(s, ruleScore(s, req), ruleReason(s, req), false);
                })
                .sorted(Comparator.comparingInt(SpaceMatchResponse::score).reversed())
                .toList();
    }

    /** Claude 호출로 spaceId→점수 맵을 얻는다. 실패하면 빈 맵(→ 규칙 기반 폴백). */
    private Map<Long, SpaceScores.SpaceScore> tryAiScores(List<Space> candidates, MatchRequest req) {
        try {
            String prompt = buildScoringPrompt(candidates, req);
            MessageCreateParams.Builder base = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(2048L)
                    .addUserMessage(prompt);

            StructuredMessage<SpaceScores> message =
                    anthropic.messages().create(base.outputConfig(SpaceScores.class).build());

            SpaceScores result = message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .findFirst()
                    .orElse(null);

            if (result == null || result.scores() == null) {
                return Map.of();
            }
            return result.scores().stream()
                    .collect(Collectors.toMap(SpaceScores.SpaceScore::spaceId, Function.identity(),
                            (a, b) -> a));
        } catch (Exception e) {
            log.warn("A-03 Claude 점수 산출 실패 — 규칙 기반 폴백 사용", e);
            return Map.of();
        }
    }

    private String buildScoringPrompt(List<Space> candidates, MatchRequest req) {
        String candidatesJson = toJson(candidates);
        return """
                취미 모임을 열 게스트가 요청한 조건과, 하드 필터를 통과한 후보 공간 목록이다.
                각 후보가 이 활동에 얼마나 적합한지 0~100점으로 평가하고, 추천/비추천 이유를 한 문장(한국어)으로 달아라.
                모든 후보(spaceId)에 대해 빠짐없이 점수를 매겨라.

                평가 관점:
                - 필요 시설 충족도(requiredFacilities가 공간 facilities에 포함되는지)
                - 수용 인원 여유(너무 크지도 작지도 않게)
                - 시간당 이용료(저렴할수록 가점)
                - 활동 성격과 공간 조건(conditions)의 부합

                [요청 조건]
                지역: %s
                필요 인원: %d명
                분야: %s
                필요 시설: %s
                소음 발생: %s / 오염 발생: %s

                [후보 공간 목록(JSON)]
                %s
                """.formatted(
                req.regionOrEmpty().isBlank() ? "(무관)" : req.regionOrEmpty(),
                req.headcount(),
                req.field(),
                req.requiredFacilitiesOrEmpty().isEmpty() ? "(없음)" : req.requiredFacilitiesOrEmpty(),
                req.noisy() ? "예" : "아니오",
                req.messy() ? "예" : "아니오",
                candidatesJson
        );
    }

    private String toJson(List<Space> candidates) {
        List<CandidateView> views = candidates.stream()
                .map(s -> new CandidateView(
                        s.getId(), s.getName(), s.getRegion(), s.getCapacity(), s.getHourlyFee(),
                        s.getFacilities(), s.getAllowedFields(),
                        s.isNoiseAllowed(), s.isMessAllowed(), s.getConditions()))
                .toList();
        try {
            return objectMapper.writeValueAsString(views);
        } catch (JsonProcessingException e) {
            // 직렬화 실패 시 사람이 읽는 형태로라도 전달
            return views.toString();
        }
    }

    // ===== 규칙 기반 폴백 (A-03 데모 안정성) =====

    private int ruleScore(Space s, MatchRequest req) {
        int score = 55;
        List<FacilityType> required = req.requiredFacilitiesOrEmpty();
        if (required.isEmpty()) {
            score += 15;
        } else {
            long matched = required.stream().filter(s.getFacilities()::contains).count();
            score += (int) Math.round((double) matched / required.size() * 30);
        }
        int slack = s.getCapacity() - req.headcount();
        if (slack >= 0) {
            score += slack <= 4 ? 15 : slack <= 10 ? 8 : 3;
        }
        int fee = s.getHourlyFee();
        score += fee <= 10_000 ? 10 : fee <= 20_000 ? 5 : 0;
        return Math.min(score, 100);
    }

    private String ruleReason(Space s, MatchRequest req) {
        List<FacilityType> required = req.requiredFacilitiesOrEmpty();
        long matched = required.stream().filter(s.getFacilities()::contains).count();
        return "수용 %d명 / 시간당 %,d원, 요청 시설 %d개 중 %d개 충족 (규칙 기반 추천)"
                .formatted(s.getCapacity(), s.getHourlyFee(), required.size(), matched);
    }

    /** LLM 프롬프트에 직렬화해 넣을 후보 요약 뷰. */
    private record CandidateView(
            long spaceId, String name, String region, int capacity, int hourlyFee,
            Set<FacilityType> facilities, Set<kkmljh.borrow.domain.ActivityField> allowedFields,
            boolean noiseAllowed, boolean messAllowed, String conditions) {
    }
}