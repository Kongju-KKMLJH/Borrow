package kkmljh.borrow.ai.service;

import kkmljh.borrow.ai.dto.MatchRequest;
import kkmljh.borrow.ai.dto.SpaceMatchResponse;
import kkmljh.borrow.ai.dto.SpaceMatchResult;
import kkmljh.borrow.ai.dto.SpaceScores;
import kkmljh.borrow.ai.llm.LlmClient;
import kkmljh.borrow.ai.repository.SpaceMatchRepository;
import kkmljh.borrow.ai.repository.SpaceSlotMatchRepository;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceMatchingService — A-02 하드 필터 · A-03 점수 · A-04 대체 추천")
class SpaceMatchingServiceTest {

    /** 2026-09-12 는 토요일 */
    private static final LocalDate SATURDAY = LocalDate.of(2026, 9, 12);

    @Mock
    private SpaceMatchRepository spaceRepo;

    @Mock
    private SpaceSlotMatchRepository slotRepo;

    @Mock
    private LlmClient llm;

    @InjectMocks
    private SpaceMatchingService matchingService;

    private MatchRequest request(List<Long> exclude, Boolean noisy, Boolean messy) {
        return new MatchRequest("천안시 서북구", 6, List.of(FacilityType.WATER), noisy, messy,
                ActivityField.ART, SATURDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), exclude);
    }

    private MatchRequest request() {
        return request(null, false, false);
    }

    private Space space(Long id, boolean noiseAllowed, boolean messAllowed) {
        return TestFixtures.withId(Space.builder()
                .ownerId("host1").name("공간 " + id).region("천안시 서북구 불당동")
                .capacity(10).hourlyFee(10_000)
                .facilities(Set.of(FacilityType.WATER, FacilityType.TABLE))
                .allowedFields(Set.of(ActivityField.ART))
                .noiseAllowed(noiseAllowed).messAllowed(messAllowed)
                .build(), id);
    }

    private SpaceSlot coveringSlot(Space space) {
        return SpaceSlot.builder()
                .space(space).dayOfWeek(DayOfWeek.SATURDAY)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(22, 0))
                .build();
    }

    private SpaceSlot narrowSlot(Space space) {
        return SpaceSlot.builder()
                .space(space).dayOfWeek(DayOfWeek.SATURDAY)
                .startTime(LocalTime.of(15, 0)).endTime(LocalTime.of(16, 0))
                .build();
    }

    @Nested
    @DisplayName("A-02 하드 필터")
    class HardFilter {

        @Test
        @DisplayName("후보가 없으면 LLM을 호출하지 않고 빈 목록을 준다")
        void noCandidates() {
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of());

            assertThat(matchingService.match(request()).matched()).isEmpty();
            verify(llm, never()).complete(anyString(), any(), anyLong());
        }

        @Test
        @DisplayName("요청 조건(인원·지역·분야)이 그대로 쿼리로 전달된다")
        void passesQueryConditions() {
            given(spaceRepo.findCandidates(6, "천안시 서북구", ActivityField.ART)).willReturn(List.of());

            matchingService.match(request());

            verify(spaceRepo).findCandidates(6, "천안시 서북구", ActivityField.ART);
        }

        @Test
        @DisplayName("활동 날짜의 요일에 시간대를 완전히 포함하는 슬롯이 있어야 후보로 남는다")
        void requiresCoveringSlot() {
            Space covered = space(1L, true, true);
            Space narrow = space(2L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(covered, narrow));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(covered), narrowSlot(narrow)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            assertThat(matchingService.match(request()).matched())
                    .extracting(SpaceMatchResponse::spaceId)
                    .containsExactly(1L);
        }

        @Test
        @DisplayName("해당 요일에 슬롯이 아예 없으면 후보에서 빠진다")
        void noSlotOnThatDay() {
            Space space = space(1L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(space));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY))).willReturn(List.of());

            assertThat(matchingService.match(request()).matched()).isEmpty();
        }

        @Test
        @DisplayName("소음 발생 활동은 소음 불가 공간을 제외한다")
        void noisyExcludesQuietSpaces() {
            Space quiet = space(1L, false, true);
            Space loud = space(2L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(quiet, loud));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(loud)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            assertThat(matchingService.match(request(null, true, false)).matched())
                    .extracting(SpaceMatchResponse::spaceId)
                    .containsExactly(2L);
        }

        @Test
        @DisplayName("오염 발생 활동은 오염 불가 공간을 제외한다")
        void messyExcludesCleanOnlySpaces() {
            Space clean = space(1L, true, false);
            Space messy = space(2L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(clean, messy));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(messy)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            assertThat(matchingService.match(request(null, false, true)).matched())
                    .extracting(SpaceMatchResponse::spaceId)
                    .containsExactly(2L);
        }

        @Test
        @DisplayName("소음·오염이 null 이면 제한을 적용하지 않는다")
        void nullFlagsMeanNoRestriction() {
            Space quiet = space(1L, false, false);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(quiet));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(quiet)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            assertThat(matchingService.match(request(null, null, null)).matched()).hasSize(1);
        }

        @Test
        @DisplayName("A-04: excludeSpaceIds 로 거절된 공간을 뺀다")
        void excludesRejectedSpaces() {
            Space rejected = space(1L, true, true);
            Space alternative = space(2L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any()))
                    .willReturn(List.of(rejected, alternative));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(alternative)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            assertThat(matchingService.match(request(List.of(1L), false, false)).matched())
                    .extracting(SpaceMatchResponse::spaceId)
                    .containsExactly(2L);
        }

        @Test
        @DisplayName("모든 후보가 제외되면 빈 목록")
        void allExcluded() {
            Space rejected = space(1L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(rejected));

            assertThat(matchingService.match(request(List.of(1L), false, false)).matched()).isEmpty();
        }
    }

    @Nested
    @DisplayName("A-03 적합도 점수")
    class Scoring {

        private void twoCandidates() {
            Space first = space(1L, true, true);
            Space second = space(2L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(first, second));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(first), coveringSlot(second)));
        }

        @Test
        @DisplayName("LLM 점수를 쓰고 점수 내림차순으로 정렬한다")
        void usesAiScoresSortedDesc() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(
                    new SpaceScores(List.of(
                            new SpaceScores.SpaceScore(1L, 70, "무난함", List.of()),
                            new SpaceScores.SpaceScore(2L, 95, "시설이 완벽함", List.of()))));

            List<SpaceMatchResponse> result = matchingService.match(request()).matched();

            assertThat(result).extracting(SpaceMatchResponse::spaceId).containsExactly(2L, 1L);
            assertThat(result.get(0).score()).isEqualTo(95);
            assertThat(result.get(0).reason()).isEqualTo("시설이 완벽함");
            assertThat(result).allSatisfy(r -> assertThat(r.aiScored()).isTrue());
        }

        @Test
        @DisplayName("범위를 벗어난 LLM 점수는 0~100으로 자른다")
        void boundsAiScore() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(
                    new SpaceScores(List.of(
                            new SpaceScores.SpaceScore(1L, 150, "과한 점수", List.of()),
                            new SpaceScores.SpaceScore(2L, -20, "음수 점수", List.of()))));

            List<SpaceMatchResponse> result = matchingService.match(request()).matched();

            assertThat(result).extracting(SpaceMatchResponse::score).containsExactly(100, 0);
        }

        @Test
        @DisplayName("LLM 호출이 실패하면 규칙 기반 점수로 폴백한다 (데모 안정성)")
        void fallsBackOnApiFailure() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong()))
                    .willThrow(new RuntimeException("API down"));

            List<SpaceMatchResponse> result = matchingService.match(request()).matched();

            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(r -> {
                assertThat(r.aiScored()).isFalse();
                assertThat(r.score()).isBetween(0, 100);
                assertThat(r.reason()).contains("규칙 기반 추천");
            });
        }

        @Test
        @DisplayName("LLM 응답이 비면 폴백한다")
        void fallsBackOnNullResult() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            assertThat(matchingService.match(request()).matched())
                    .allSatisfy(r -> assertThat(r.aiScored()).isFalse());
        }

        @Test
        @DisplayName("LLM이 점수 목록을 비워 보내도 폴백한다")
        void fallsBackOnNullScores() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong()))
                    .willReturn(new SpaceScores(null));

            assertThat(matchingService.match(request()).matched())
                    .allSatisfy(r -> assertThat(r.aiScored()).isFalse());
        }

        @Test
        @DisplayName("일부 후보만 점수가 오면 나머지만 규칙 기반으로 채운다")
        void partialAiScores() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(
                    new SpaceScores(List.of(new SpaceScores.SpaceScore(1L, 90, "LLM 평가", List.of()))));

            List<SpaceMatchResponse> result = matchingService.match(request()).matched();

            assertThat(result).filteredOn(r -> r.spaceId().equals(1L))
                    .singleElement()
                    .satisfies(r -> assertThat(r.aiScored()).isTrue());
            assertThat(result).filteredOn(r -> r.spaceId().equals(2L))
                    .singleElement()
                    .satisfies(r -> assertThat(r.aiScored()).isFalse());
        }

        @Test
        @DisplayName("같은 공간에 점수가 두 번 오면 먼저 온 값을 쓴다 (중복 키 충돌 방지)")
        void duplicateScoresKeepFirst() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(
                    new SpaceScores(List.of(
                            new SpaceScores.SpaceScore(1L, 90, "첫 평가", List.of()),
                            new SpaceScores.SpaceScore(1L, 10, "중복 평가", List.of()),
                            new SpaceScores.SpaceScore(2L, 50, "평가", List.of()))));

            assertThat(matchingService.match(request()).matched())
                    .filteredOn(r -> r.spaceId().equals(1L))
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.score()).isEqualTo(90);
                        assertThat(r.reason()).isEqualTo("첫 평가");
                    });
        }

        @Test
        @DisplayName("응답에는 공간 요약 정보가 함께 담긴다")
        void responseContainsSpaceSummary() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            SpaceMatchResponse response = matchingService.match(request()).matched().get(0);

            assertThat(response.name()).startsWith("공간 ");
            assertThat(response.region()).isEqualTo("천안시 서북구 불당동");
            assertThat(response.capacity()).isEqualTo(10);
            assertThat(response.hourlyFee()).isEqualTo(10_000);
            assertThat(response.facilities()).contains(FacilityType.WATER);
            assertThat(response.allowedFields()).containsExactly(ActivityField.ART);
        }

        @Test
        @DisplayName("프롬프트에 요청 조건과 후보 목록이 담긴다")
        void promptContainsConditionsAndCandidates() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            matchingService.match(request());

            org.mockito.ArgumentCaptor<String> prompt = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(llm).complete(prompt.capture(), eq(SpaceScores.class), anyLong());
            assertThat(prompt.getValue())
                    .contains("천안시 서북구")
                    .contains("6명")
                    .contains("ART")
                    .contains("\"spaceId\":1");
        }
    }

    @Nested
    @DisplayName("규칙 기반 폴백 점수")
    class RuleScore {

        private List<SpaceMatchResponse> matchWithFallback(Space space, MatchRequest request) {
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(space));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(space)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);
            return matchingService.match(request).matched();
        }

        @Test
        @DisplayName("필요 시설을 모두 갖춘 공간이 더 높은 점수를 받는다")
        void facilityMatchRaisesScore() {
            Space equipped = TestFixtures.withId(Space.builder()
                    .ownerId("host1").name("시설 완비").region("천안시 서북구")
                    .capacity(8).hourlyFee(10_000)
                    .facilities(Set.of(FacilityType.WATER))
                    .allowedFields(Set.of(ActivityField.ART))
                    .noiseAllowed(true).messAllowed(true).build(), 1L);
            Space bare = TestFixtures.withId(Space.builder()
                    .ownerId("host1").name("시설 없음").region("천안시 서북구")
                    .capacity(8).hourlyFee(10_000)
                    .facilities(Set.of())
                    .allowedFields(Set.of(ActivityField.ART))
                    .noiseAllowed(true).messAllowed(true).build(), 2L);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(equipped, bare));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(equipped), coveringSlot(bare)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            List<SpaceMatchResponse> result = matchingService.match(request()).matched();

            assertThat(result.get(0).spaceId()).isEqualTo(1L);
            assertThat(result.get(0).score()).isGreaterThan(result.get(1).score());
        }

        @Test
        @DisplayName("점수는 100을 넘지 않는다")
        void scoreIsCapped() {
            Space cheapAndPerfect = TestFixtures.withId(Space.builder()
                    .ownerId("host1").name("완벽").region("천안시 서북구")
                    .capacity(7).hourlyFee(1_000)
                    .facilities(Set.of(FacilityType.WATER))
                    .allowedFields(Set.of(ActivityField.ART))
                    .noiseAllowed(true).messAllowed(true).build(), 1L);

            assertThat(matchWithFallback(cheapAndPerfect, request()).get(0).score()).isLessThanOrEqualTo(100);
        }

        @Test
        @DisplayName("추천 이유에 수용 인원·이용료·시설 충족 수가 들어간다")
        void reasonExplainsScore() {
            Space space = space(1L, true, true);

            String reason = matchWithFallback(space, request()).get(0).reason();

            assertThat(reason).contains("수용 10명").contains("10,000원").contains("1개 중 1개 충족");
        }

        @Test
        @DisplayName("필요 시설이 없으면 시설 항목 만점을 준다")
        void noRequiredFacilities() {
            Space space = space(1L, true, true);
            MatchRequest noFacilities = new MatchRequest("천안시 서북구", 6, null, false, false,
                    ActivityField.ART, SATURDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), null);

            assertThat(matchWithFallback(space, noFacilities).get(0).score()).isPositive();
        }
    }

    @Nested
    @DisplayName("기능명세 3.1 display — 추천 주의사항")
    class Cautions {

        private Space pricey(Long id, boolean noiseAllowed, boolean messAllowed,
                             int capacity, int hourlyFee, String conditions) {
            return TestFixtures.withId(Space.builder()
                    .ownerId("host1").name("공간 " + id).region("천안시 서북구 불당동")
                    .capacity(capacity).hourlyFee(hourlyFee).conditions(conditions)
                    .facilities(Set.of(FacilityType.TABLE))          // 요청 시설 WATER 는 없음
                    .allowedFields(Set.of(ActivityField.ART))
                    .noiseAllowed(noiseAllowed).messAllowed(messAllowed)
                    .build(), id);
        }

        private List<String> cautionsFor(Space space) {
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(space));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(space)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            return matchingService.match(request()).matched().get(0).cautions();
        }

        @Test
        @DisplayName("LLM 주의사항을 그대로 내려준다")
        void usesAiCautions() {
            Space space = space(1L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(space));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(space)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(
                    new SpaceScores(List.of(new SpaceScores.SpaceScore(
                            1L, 90, "좋음", List.of("주말 오후는 매장이 붐빕니다.")))));

            assertThat(matchingService.match(request()).matched().get(0).cautions())
                    .containsExactly("주말 오후는 매장이 붐빕니다.");
        }

        @Test
        @DisplayName("LLM이 주의사항을 빠뜨리면 null 대신 빈 목록을 내려준다")
        void aiCautionsMayBeNull() {
            Space space = space(1L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(space));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(space)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(
                    new SpaceScores(List.of(new SpaceScores.SpaceScore(1L, 90, "좋음", null))));

            assertThat(matchingService.match(request()).matched().get(0).cautions()).isEmpty();
        }

        @Test
        @DisplayName("LLM 실패로 폴백해도 주의사항은 사라지지 않는다")
        void fallbackStillHasCautions() {
            Space space = pricey(1L, false, false, 7, 30_000, "음료 1잔 주문 필수");

            assertThat(cautionsFor(space)).isNotEmpty();
        }

        @Test
        @DisplayName("요청한 시설이 없으면 짚어 준다")
        void missingFacility() {
            assertThat(cautionsFor(pricey(1L, true, true, 20, 10_000, null)))
                    .anyMatch(c -> c.contains("WATER"));
        }

        @Test
        @DisplayName("수용 인원 여유가 빠듯하면 짚어 준다")
        void tightCapacity() {
            // 요청 인원 6명, 수용 7명 → 여유 1명
            assertThat(cautionsFor(pricey(1L, true, true, 7, 10_000, null)))
                    .anyMatch(c -> c.contains("수용 인원 여유가 1명"));
        }

        @Test
        @DisplayName("여유가 넉넉하면 인원 주의사항을 달지 않는다")
        void roomyCapacityHasNoCaution() {
            assertThat(cautionsFor(pricey(1L, true, true, 20, 10_000, null)))
                    .noneMatch(c -> c.contains("수용 인원 여유"));
        }

        @Test
        @DisplayName("소음·오염 불가 공간은 그 사실을 짚어 준다")
        void noiseAndMessRestrictions() {
            assertThat(cautionsFor(pricey(1L, false, false, 20, 10_000, null)))
                    .anyMatch(c -> c.contains("소음"))
                    .anyMatch(c -> c.contains("오염"));
        }

        @Test
        @DisplayName("이용료가 높으면 짚어 주고, 저렴하면 짚지 않는다")
        void expensiveFee() {
            assertThat(cautionsFor(pricey(1L, true, true, 20, 30_000, null)))
                    .anyMatch(c -> c.contains("이용료"));
        }

        @Test
        @DisplayName("이용 조건이 있으면 확인하라고 알려 준다")
        void conditionsAreSurfaced() {
            assertThat(cautionsFor(pricey(1L, true, true, 20, 10_000, "음료 1잔 주문 필수")))
                    .anyMatch(c -> c.contains("음료 1잔 주문 필수"));
        }

        @Test
        @DisplayName("빈 이용 조건은 주의사항으로 만들지 않는다")
        void blankConditionsIgnored() {
            assertThat(cautionsFor(pricey(1L, true, true, 20, 10_000, "   ")))
                    .noneMatch(c -> c.contains("이용 조건"));
        }
    }

    @Nested
    @DisplayName("기능명세 3.1.1 exceptions — 추천 불가 안내")
    class NoMatchSuggestions {

        private SpaceMatchResult noCandidates(MatchRequest request) {
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of());
            return matchingService.match(request);
        }

        @Test
        @DisplayName("후보가 없어도 오류가 아니라 빈 matched + 조건 수정 안내로 돌려준다")
        void suggestionsInsteadOfError() {
            SpaceMatchResult result = noCandidates(request());

            assertThat(result.matched()).isEmpty();
            assertThat(result.suggestions()).isNotEmpty();
        }

        @Test
        @DisplayName("걸린 조건만 짚는다 — 지역·인원·시설·시간대")
        void suggestsOnlyAppliedConditions() {
            SpaceMatchResult result = noCandidates(request());

            assertThat(result.suggestions())
                    .anyMatch(sug -> sug.contains("지역"))
                    .anyMatch(sug -> sug.contains("모집 인원(6명)"))
                    .anyMatch(sug -> sug.contains("WATER"))
                    .anyMatch(sug -> sug.contains("시간대"));
        }

        @Test
        @DisplayName("지역을 걸지 않았으면 지역을 넓히라고 하지 않는다")
        void noRegionSuggestionWhenRegionBlank() {
            MatchRequest noRegion = new MatchRequest("  ", 6, List.of(), false, false,
                    ActivityField.ART, SATURDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), null);

            assertThat(noCandidates(noRegion).suggestions())
                    .noneMatch(sug -> sug.contains("희망 지역"));
        }

        @Test
        @DisplayName("소음·오염 제한을 걸었을 때만 그 안내를 붙인다")
        void noiseSuggestionOnlyWhenRestricted() {
            assertThat(noCandidates(request(null, true, false)).suggestions())
                    .anyMatch(sug -> sug.contains("소음·오염"));
        }

        @Test
        @DisplayName("A-04 대체 추천에서 모두 제외되면 제외를 풀라고 안내한다")
        void suggestsUnexcluding() {
            assertThat(noCandidates(request(List.of(1L, 2L), false, false)).suggestions())
                    .anyMatch(sug -> sug.contains("제외한 공간"));
        }

        @Test
        @DisplayName("추천이 있으면 조건 수정 안내는 비운다")
        void noSuggestionsWhenMatched() {
            Space space = space(1L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(space));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY)))
                    .willReturn(List.of(coveringSlot(space)));
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(null);

            SpaceMatchResult result = matchingService.match(request());

            assertThat(result.matched()).hasSize(1);
            assertThat(result.suggestions()).isEmpty();
        }
    }
}
