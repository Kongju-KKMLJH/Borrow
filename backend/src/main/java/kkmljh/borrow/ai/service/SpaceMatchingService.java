package kkmljh.borrow.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kkmljh.borrow.ai.dto.MatchRequest;
import kkmljh.borrow.ai.dto.SpaceMatchResponse;
import kkmljh.borrow.ai.dto.SpaceMatchResult;
import kkmljh.borrow.ai.dto.SpaceScores;
import kkmljh.borrow.ai.llm.LlmClient;
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
import java.util.ArrayList;
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

    private static final int MAX_CANDIDATES = 12;

    /** 이 값을 넘으면 "이용료가 높은 편"이라고 짚어 준다 (규칙 기반 주의사항). */
    private static final int EXPENSIVE_HOURLY_FEE = 20_000;

    private final SpaceMatchRepository spaceRepo;
    private final SpaceSlotMatchRepository slotRepo;
    private final LlmClient llm;

    // 초기화자가 있는 final 필드는 @RequiredArgsConstructor 대상에서 제외된다.
    // Spring Boot 4 webmvc 스타터는 Jackson 자동설정(ObjectMapper 빈)을 포함하지 않으므로 직접 생성.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public SpaceMatchResult match(MatchRequest request) {
        List<Space> candidates = hardFilter(request);          // A-02 (+ A-04 제외 반영)
        if (candidates.isEmpty()) {
            // 기능명세 3.1.1 exceptions — 후보 0건은 오류가 아니다. 조건을 어떻게 고치면 되는지 알려준다.
            return SpaceMatchResult.noMatch(suggestions(request));
        }
        return SpaceMatchResult.matched(score(candidates, request));   // A-03
    }

    /** A-02: 하드 필터. 통과한 후보를 최대 {@link #MAX_CANDIDATES}개까지 반환. */
    private List<Space> hardFilter(MatchRequest req) {
        Set<Long> excluded = Set.copyOf(req.excludeSpaceIdsOrEmpty());

        List<Space> byQuery = spaceRepo.findCandidates(req.headcount(), req.regionOrEmpty(), req.field());

        List<Space> filtered = byQuery.stream()
                .filter(s -> !excluded.contains(s.getId()))
                // 소음·오염 제한 (B-04 제한 조건)
                .filter(s -> !req.noisyOrFalse() || s.isNoiseAllowed())
                .filter(s -> !req.messyOrFalse() || s.isMessAllowed())
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
                        return SpaceMatchResponse.of(s, bounded, ai.reason(), ai.cautionsOrEmpty(), true);
                    }
                    // 폴백에서도 주의사항을 반드시 채운다 — LLM 실패로 주의사항이 통째로 사라지면
                    // "AI가 판정한 게 아니다"를 알려 줄 장치가 없어진다 (기능명세 3.1 display).
                    return SpaceMatchResponse.of(s, ruleScore(s, req), ruleReason(s, req),
                            ruleCautions(s, req), false);
                })
                .sorted(Comparator.comparingInt(SpaceMatchResponse::score).reversed())
                .toList();
    }

    /** LLM 호출로 spaceId→점수 맵을 얻는다. 실패하면 빈 맵(→ 규칙 기반 폴백). */
    private Map<Long, SpaceScores.SpaceScore> tryAiScores(List<Space> candidates, MatchRequest req) {
        try {
            String prompt = buildScoringPrompt(candidates, req);
            // 4096: 후보 다수 점수 출력 + thinking 모델(Gemini)의 추론 토큰까지 감안한 여유 상한.
            // 부족해 잘려도 규칙 기반 폴백이 있어 데모는 안정적이다.
            SpaceScores result = llm.complete(prompt, SpaceScores.class, 4096L);

            if (result == null || result.scores() == null) {
                return Map.of();
            }
            return result.scores().stream()
                    .collect(Collectors.toMap(SpaceScores.SpaceScore::spaceId, Function.identity(),
                            (a, b) -> a));
        } catch (Exception e) {
            log.warn("A-03 LLM 점수 산출 실패 — 규칙 기반 폴백 사용", e);
            return Map.of();
        }
    }

    private String buildScoringPrompt(List<Space> candidates, MatchRequest req) {
        String candidatesJson = toJson(candidates);
        return """
                취미 모임을 열 게스트가 요청한 조건과, 하드 필터를 통과한 후보 공간 목록이다.
                각 후보가 이 활동에 얼마나 적합한지 0~100점으로 평가하고, 추천/비추천 이유를 한 문장(한국어)으로 달아라.
                함께 개최 전에 확인해야 할 주의사항(cautions)도 적어라 — 후보 공간 정보에 근거가 있는 것만이고,
                짚을 것이 없으면 빈 목록으로 둔다. 최종 승인 권한은 공간 파트너에게 있으므로 확정처럼 쓰지 마라.
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
                req.noisyOrFalse() ? "예" : "아니오",
                req.messyOrFalse() ? "예" : "아니오",
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

    /**
     * 규칙 기반 주의사항 (기능명세 3.1 {@code display}). 재료는 전부 이미 엔티티에 있다 —
     * 수용 인원 여유, 이용 조건, 소음·오염 허용 여부, 시간당 이용료, 요청 시설 충족도.
     * <b>새 엔티티 필드를 만들지 않는다.</b>
     */
    private List<String> ruleCautions(Space s, MatchRequest req) {
        List<String> cautions = new ArrayList<>();

        List<FacilityType> missing = req.requiredFacilitiesOrEmpty().stream()
                .filter(f -> !s.getFacilities().contains(f))
                .toList();
        if (!missing.isEmpty()) {
            cautions.add("요청한 시설 중 %s 이(가) 없습니다. 직접 준비해야 할 수 있습니다.".formatted(missing));
        }

        int slack = s.getCapacity() - req.headcount();
        if (slack <= 2) {
            cautions.add("수용 인원 여유가 %d명뿐입니다. 참여 인원이 늘면 자리가 부족할 수 있습니다.".formatted(slack));
        }

        if (!s.isNoiseAllowed()) {
            cautions.add("소음이 발생하는 활동은 허용되지 않는 공간입니다.");
        }
        if (!s.isMessAllowed()) {
            cautions.add("물감·흙 등 오염이 발생하는 활동은 허용되지 않는 공간입니다.");
        }

        if (s.getHourlyFee() > EXPENSIVE_HOURLY_FEE) {
            cautions.add("시간당 이용료가 %,d원으로 높은 편입니다.".formatted(s.getHourlyFee()));
        }

        if (s.getConditions() != null && !s.getConditions().isBlank()) {
            cautions.add("이용 조건을 확인하세요 — " + s.getConditions().trim());
        }

        return List.copyOf(cautions);
    }

    /**
     * 기능명세 3.1.1 {@code exceptions} — 후보 0건일 때의 조건 수정 안내.
     * 요청에 실제로 걸린 조건만 짚는다 (걸지도 않은 조건을 풀라고 하면 안내가 아니라 소음이다).
     */
    private List<String> suggestions(MatchRequest req) {
        List<String> suggestions = new ArrayList<>();

        if (!req.regionOrEmpty().isBlank()) {
            suggestions.add("희망 지역을 넓혀 보세요 — 동 단위 대신 구·시 단위로 검색하면 후보가 늘어납니다.");
        }
        suggestions.add("모집 인원(%d명)을 줄이면 수용 가능한 공간이 늘어납니다.".formatted(req.headcount()));
        if (!req.requiredFacilitiesOrEmpty().isEmpty()) {
            suggestions.add("필수 시설 %s 중 일부를 빼고 다시 찾아 보세요.".formatted(req.requiredFacilitiesOrEmpty()));
        }
        if (req.noisyOrFalse() || req.messyOrFalse()) {
            suggestions.add("소음·오염이 발생하지 않는 형태로 활동을 조정하면 이용 가능한 공간이 늘어납니다.");
        }
        suggestions.add("활동 날짜·시간대를 조정해 보세요 — 해당 요일에 그 시간대를 여는 공간이 없을 수 있습니다.");
        if (!req.excludeSpaceIdsOrEmpty().isEmpty()) {
            suggestions.add("앞서 제외한 공간을 다시 후보에 넣으면 추천을 받을 수 있습니다.");
        }

        return List.copyOf(suggestions);
    }

    /** LLM 프롬프트에 직렬화해 넣을 후보 요약 뷰. */
    private record CandidateView(
            long spaceId, String name, String region, int capacity, int hourlyFee,
            Set<FacilityType> facilities, Set<kkmljh.borrow.domain.ActivityField> allowedFields,
            boolean noiseAllowed, boolean messAllowed, String conditions) {
    }
}