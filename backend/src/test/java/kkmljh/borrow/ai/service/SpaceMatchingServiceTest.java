package kkmljh.borrow.ai.service;

import kkmljh.borrow.ai.dto.MatchRequest;
import kkmljh.borrow.ai.dto.SpaceMatchResponse;
import kkmljh.borrow.ai.repository.SpaceMatchRepository;
import kkmljh.borrow.ai.repository.SpaceSlotMatchRepository;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.support.Entities;
import com.anthropic.client.AnthropicClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * A-02 하드 필터 + A-03 규칙 기반 폴백 검증.
 *
 * <p>Claude 점수 산출 성공 경로는 SDK {@code StructuredMessage} 구성이 필요해 통합테스트로 남기고,
 * 여기서는 {@code anthropic.messages()}가 예외를 던지도록 스텁해 <b>규칙 기반 폴백</b> 경로를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceMatchingService — A-02 하드필터 / A-03 규칙 폴백")
class SpaceMatchingServiceTest {

    @Mock
    private SpaceMatchRepository spaceRepo;
    @Mock
    private SpaceSlotMatchRepository slotRepo;
    @Mock
    private AnthropicClient anthropic;

    @InjectMocks
    private SpaceMatchingService service;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 3); // 월요일
    private static final DayOfWeek DAY = DATE.getDayOfWeek();
    private static final LocalTime START = LocalTime.of(14, 0);
    private static final LocalTime END = LocalTime.of(16, 0);

    private MatchRequest request(List<FacilityType> facilities, boolean noisy, boolean messy,
                                 List<Long> excludeIds) {
        return new MatchRequest("천안시", 4, facilities, noisy, messy,
                ActivityField.ART, DATE, START, END, excludeIds);
    }

    private MatchRequest defaultRequest() {
        return request(List.of(), false, false, List.of());
    }

    /** 요청 시간대(START~END)를 감싸는 슬롯. */
    private SpaceSlot coveringSlot(long id, Space space) {
        return Entities.slot(id, space, DAY, LocalTime.of(9, 0), LocalTime.of(22, 0));
    }

    private void forceRuleFallback() {
        // 점수 산출 단계에서 Claude 호출이 실패하면 규칙 기반 폴백을 탄다.
        when(anthropic.messages()).thenThrow(new RuntimeException("offline"));
    }

    @Test
    @DisplayName("하드필터를 통과한 후보가 없으면 빈 리스트 (Claude 호출 없음)")
    void returnsEmptyWhenNoCandidate() {
        when(spaceRepo.findCandidates(anyInt(), anyString(), any())).thenReturn(List.of());

        assertThat(service.match(defaultRequest())).isEmpty();
    }

    @Test
    @DisplayName("excludeSpaceIds에 든 공간은 후보에서 제외 (A-04 대체 추천)")
    void excludesRejectedSpaces() {
        Space keep = Entities.space(1L, 10);
        Space drop = Entities.space(2L, 10);
        when(spaceRepo.findCandidates(anyInt(), anyString(), any())).thenReturn(List.of(keep, drop));
        when(slotRepo.findBySpaceIdsAndDay(any(), any())).thenReturn(List.of(coveringSlot(1L, keep)));
        forceRuleFallback();

        List<SpaceMatchResponse> result = service.match(request(List.of(), false, false, List.of(2L)));

        assertThat(result).extracting(SpaceMatchResponse::spaceId).containsExactly(1L);
    }

    @Test
    @DisplayName("소음 발생 요청이면 소음 불가 공간을 제외한다")
    void excludesNoiseDisallowedWhenNoisy() {
        Space noiseOk = Entities.space(1L, "천안시", 10, 10_000, Set.of(),
                Set.of(ActivityField.ART), true, true);
        Space noiseNo = Entities.space(2L, "천안시", 10, 10_000, Set.of(),
                Set.of(ActivityField.ART), false, true);
        when(spaceRepo.findCandidates(anyInt(), anyString(), any())).thenReturn(List.of(noiseOk, noiseNo));
        when(slotRepo.findBySpaceIdsAndDay(any(), any())).thenReturn(List.of(coveringSlot(1L, noiseOk)));
        forceRuleFallback();

        List<SpaceMatchResponse> result = service.match(request(List.of(), true, false, List.of()));

        assertThat(result).extracting(SpaceMatchResponse::spaceId).containsExactly(1L);
    }

    @Test
    @DisplayName("오염 발생 요청이면 오염 불가 공간을 제외한다")
    void excludesMessDisallowedWhenMessy() {
        Space messOk = Entities.space(1L, "천안시", 10, 10_000, Set.of(),
                Set.of(ActivityField.ART), true, true);
        Space messNo = Entities.space(2L, "천안시", 10, 10_000, Set.of(),
                Set.of(ActivityField.ART), true, false);
        when(spaceRepo.findCandidates(anyInt(), anyString(), any())).thenReturn(List.of(messOk, messNo));
        when(slotRepo.findBySpaceIdsAndDay(any(), any())).thenReturn(List.of(coveringSlot(1L, messOk)));
        forceRuleFallback();

        List<SpaceMatchResponse> result = service.match(request(List.of(), false, true, List.of()));

        assertThat(result).extracting(SpaceMatchResponse::spaceId).containsExactly(1L);
    }

    @Test
    @DisplayName("요청 시간대를 감싸는 슬롯이 없는 공간은 제외한다")
    void excludesSpacesWithoutCoveringSlot() {
        Space covered = Entities.space(1L, 10);
        Space notCovered = Entities.space(2L, 10);
        when(spaceRepo.findCandidates(anyInt(), anyString(), any())).thenReturn(List.of(covered, notCovered));
        // notCovered(2L)의 슬롯은 오전만 열려 요청 시간(14~16)을 못 감싼다.
        SpaceSlot morningOnly = Entities.slot(2L, notCovered, DAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(slotRepo.findBySpaceIdsAndDay(any(), any()))
                .thenReturn(List.of(coveringSlot(1L, covered), morningOnly));
        forceRuleFallback();

        List<SpaceMatchResponse> result = service.match(defaultRequest());

        assertThat(result).extracting(SpaceMatchResponse::spaceId).containsExactly(1L);
    }

    @Test
    @DisplayName("후보가 상한(12개)을 넘으면 최대 12개까지만 반환한다")
    void limitsToMaxCandidates() {
        List<Space> many = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> Entities.space(i, 10))
                .toList();
        List<SpaceSlot> slots = new ArrayList<>();
        for (Space s : many) {
            slots.add(coveringSlot(s.getId(), s));
        }
        when(spaceRepo.findCandidates(anyInt(), anyString(), any())).thenReturn(many);
        when(slotRepo.findBySpaceIdsAndDay(any(), any())).thenReturn(slots);
        forceRuleFallback();

        assertThat(service.match(defaultRequest())).hasSize(12);
    }

    @Test
    @DisplayName("Claude 실패 시 규칙 기반 점수로 폴백하고 점수 내림차순 정렬 (aiScored=false)")
    void fallsBackToRuleScoresSortedDescending() {
        Space a = Entities.space(1L, "천안시", 6, 5_000, Set.of(), Set.of(ActivityField.ART), true, true);
        Space b = Entities.space(2L, "천안시", 20, 25_000, Set.of(), Set.of(ActivityField.ART), true, true);
        Space c = Entities.space(3L, "천안시", 8, 15_000, Set.of(), Set.of(ActivityField.ART), true, true);
        when(spaceRepo.findCandidates(anyInt(), anyString(), any())).thenReturn(List.of(a, b, c));
        when(slotRepo.findBySpaceIdsAndDay(any(), any()))
                .thenReturn(List.of(coveringSlot(1L, a), coveringSlot(2L, b), coveringSlot(3L, c)));
        forceRuleFallback();

        List<SpaceMatchResponse> result = service.match(defaultRequest());

        assertThat(result).allSatisfy(r -> assertThat(r.aiScored()).isFalse());
        assertThat(result).extracting(SpaceMatchResponse::score).isSortedAccordingTo((x, y) -> Integer.compare(y, x));
    }
}
