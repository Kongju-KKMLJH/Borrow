package kkmljh.borrow.ai.service;

import kkmljh.borrow.ai.dto.MatchRequest;
import kkmljh.borrow.ai.dto.SpaceMatchResponse;
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

            assertThat(matchingService.match(request())).isEmpty();
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

            assertThat(matchingService.match(request()))
                    .extracting(SpaceMatchResponse::spaceId)
                    .containsExactly(1L);
        }

        @Test
        @DisplayName("해당 요일에 슬롯이 아예 없으면 후보에서 빠진다")
        void noSlotOnThatDay() {
            Space space = space(1L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(space));
            given(slotRepo.findBySpaceIdsAndDay(any(), eq(DayOfWeek.SATURDAY))).willReturn(List.of());

            assertThat(matchingService.match(request())).isEmpty();
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

            assertThat(matchingService.match(request(null, true, false)))
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

            assertThat(matchingService.match(request(null, false, true)))
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

            assertThat(matchingService.match(request(null, null, null))).hasSize(1);
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

            assertThat(matchingService.match(request(List.of(1L), false, false)))
                    .extracting(SpaceMatchResponse::spaceId)
                    .containsExactly(2L);
        }

        @Test
        @DisplayName("모든 후보가 제외되면 빈 목록")
        void allExcluded() {
            Space rejected = space(1L, true, true);
            given(spaceRepo.findCandidates(anyInt(), anyString(), any())).willReturn(List.of(rejected));

            assertThat(matchingService.match(request(List.of(1L), false, false))).isEmpty();
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
                            new SpaceScores.SpaceScore(1L, 70, "무난함"),
                            new SpaceScores.SpaceScore(2L, 95, "시설이 완벽함"))));

            List<SpaceMatchResponse> result = matchingService.match(request());

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
                            new SpaceScores.SpaceScore(1L, 150, "과한 점수"),
                            new SpaceScores.SpaceScore(2L, -20, "음수 점수"))));

            List<SpaceMatchResponse> result = matchingService.match(request());

            assertThat(result).extracting(SpaceMatchResponse::score).containsExactly(100, 0);
        }

        @Test
        @DisplayName("LLM 호출이 실패하면 규칙 기반 점수로 폴백한다 (데모 안정성)")
        void fallsBackOnApiFailure() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong()))
                    .willThrow(new RuntimeException("API down"));

            List<SpaceMatchResponse> result = matchingService.match(request());

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

            assertThat(matchingService.match(request()))
                    .allSatisfy(r -> assertThat(r.aiScored()).isFalse());
        }

        @Test
        @DisplayName("LLM이 점수 목록을 비워 보내도 폴백한다")
        void fallsBackOnNullScores() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong()))
                    .willReturn(new SpaceScores(null));

            assertThat(matchingService.match(request()))
                    .allSatisfy(r -> assertThat(r.aiScored()).isFalse());
        }

        @Test
        @DisplayName("일부 후보만 점수가 오면 나머지만 규칙 기반으로 채운다")
        void partialAiScores() {
            twoCandidates();
            given(llm.complete(anyString(), eq(SpaceScores.class), anyLong())).willReturn(
                    new SpaceScores(List.of(new SpaceScores.SpaceScore(1L, 90, "LLM 평가"))));

            List<SpaceMatchResponse> result = matchingService.match(request());

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
                            new SpaceScores.SpaceScore(1L, 90, "첫 평가"),
                            new SpaceScores.SpaceScore(1L, 10, "중복 평가"),
                            new SpaceScores.SpaceScore(2L, 50, "평가"))));

            assertThat(matchingService.match(request()))
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

            SpaceMatchResponse response = matchingService.match(request()).get(0);

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
            return matchingService.match(request);
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

            List<SpaceMatchResponse> result = matchingService.match(request());

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
}
