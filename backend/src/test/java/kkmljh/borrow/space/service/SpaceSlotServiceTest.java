package kkmljh.borrow.space.service;

import kkmljh.borrow.common.exception.BusinessException;
import kkmljh.borrow.common.exception.ErrorCode;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.space.dto.SpaceSlotRequest;
import kkmljh.borrow.space.dto.SpaceSlotResponse;
import kkmljh.borrow.space.repository.SpaceRepository;
import kkmljh.borrow.space.repository.SpaceSlotRepository;
import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceSlotService — 유휴 시간대 (B-05)")
class SpaceSlotServiceTest {

    private static final String OWNER = "host1";
    private static final String OTHER = "other-host";

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceSlotRepository spaceSlotRepository;

    @InjectMocks
    private SpaceSlotService spaceSlotService;

    private final SpaceSlotRequest request =
            new SpaceSlotRequest(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(22, 0));

    @Nested
    @DisplayName("목록 조회")
    class Read {

        @Test
        @DisplayName("공간의 슬롯을 요일·시각 순으로 돌려준다")
        void findBySpace() {
            Space space = TestFixtures.space(1L, OWNER);
            given(spaceRepository.existsById(1L)).willReturn(true);
            given(spaceSlotRepository.findBySpaceIdOrderByDayOfWeekAscStartTimeAsc(1L)).willReturn(List.of(
                    TestFixtures.slot(10L, space, DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                    TestFixtures.slot(11L, space, DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(12, 0))));

            List<SpaceSlotResponse> result = spaceSlotService.findBySpace(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(10L);
            assertThat(result.get(0).dayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
            assertThat(result.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("목록 조회는 비로그인 열람이므로 소유자를 따지지 않는다")
        void readDoesNotCheckOwner() {
            given(spaceRepository.existsById(1L)).willReturn(true);
            given(spaceSlotRepository.findBySpaceIdOrderByDayOfWeekAscStartTimeAsc(1L)).willReturn(List.of());

            assertThat(spaceSlotService.findBySpace(1L)).isEmpty();
            verify(spaceRepository, never()).findById(any());
        }

        @Test
        @DisplayName("없는 공간이면 SPACE_NOT_FOUND")
        void spaceNotFound() {
            given(spaceRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> spaceSlotService.findBySpace(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("추가")
    class Add {

        @Test
        @DisplayName("내 공간에 슬롯을 추가한다")
        void add() {
            Space space = TestFixtures.space(1L, OWNER);
            given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
            given(spaceSlotRepository.save(any(SpaceSlot.class)))
                    .willAnswer(inv -> TestFixtures.withId(inv.getArgument(0), 10L));

            SpaceSlotResponse response = spaceSlotService.add(OWNER, 1L, request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
            assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(response.endTime()).isEqualTo(LocalTime.of(22, 0));
        }

        @Test
        @DisplayName("종료 시각이 시작보다 빠르면 INVALID_REQUEST — 소유자 확인보다 먼저 걸러진다")
        void invalidTimeRange() {
            SpaceSlotRequest invalid =
                    new SpaceSlotRequest(DayOfWeek.SATURDAY, LocalTime.of(22, 0), LocalTime.of(9, 0));

            assertThatThrownBy(() -> spaceSlotService.add(OWNER, 1L, invalid))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);

            verify(spaceSlotRepository, never()).save(any());
        }

        @Test
        @DisplayName("시작과 종료가 같아도 INVALID_REQUEST")
        void zeroLength() {
            SpaceSlotRequest invalid =
                    new SpaceSlotRequest(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(9, 0));

            assertThatThrownBy(() -> spaceSlotService.add(OWNER, 1L, invalid))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }

        @Test
        @DisplayName("남의 공간에는 슬롯을 추가할 수 없다 — FORBIDDEN")
        void cannotAddToOthersSpace() {
            given(spaceRepository.findById(1L)).willReturn(Optional.of(TestFixtures.space(1L, OTHER)));

            assertThatThrownBy(() -> spaceSlotService.add(OWNER, 1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(spaceSlotRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 공간이면 SPACE_NOT_FOUND")
        void spaceNotFound() {
            given(spaceRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spaceSlotService.add(OWNER, 99L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SPACE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("내 공간의 슬롯을 지운다")
        void delete() {
            Space space = TestFixtures.space(1L, OWNER);
            SpaceSlot slot = TestFixtures.slot(10L, space);
            given(spaceRepository.findById(1L)).willReturn(Optional.of(space));
            given(spaceSlotRepository.findById(10L)).willReturn(Optional.of(slot));

            spaceSlotService.delete(OWNER, 1L, 10L);

            verify(spaceSlotRepository).delete(slot);
        }

        @Test
        @DisplayName("다른 공간의 슬롯 id 를 주면 SLOT_NOT_FOUND")
        void slotBelongsToAnotherSpace() {
            given(spaceRepository.findById(1L)).willReturn(Optional.of(TestFixtures.space(1L, OWNER)));
            given(spaceSlotRepository.findById(10L))
                    .willReturn(Optional.of(TestFixtures.slot(10L, TestFixtures.space(2L, OWNER))));

            assertThatThrownBy(() -> spaceSlotService.delete(OWNER, 1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SLOT_NOT_FOUND);

            verify(spaceSlotRepository, never()).delete(any());
        }

        @Test
        @DisplayName("없는 슬롯이면 SLOT_NOT_FOUND")
        void slotNotFound() {
            given(spaceRepository.findById(1L)).willReturn(Optional.of(TestFixtures.space(1L, OWNER)));
            given(spaceSlotRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> spaceSlotService.delete(OWNER, 1L, 99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SLOT_NOT_FOUND);
        }

        @Test
        @DisplayName("남의 공간 슬롯은 지울 수 없다 — FORBIDDEN")
        void cannotDeleteOthers() {
            given(spaceRepository.findById(1L)).willReturn(Optional.of(TestFixtures.space(1L, OTHER)));

            assertThatThrownBy(() -> spaceSlotService.delete(OWNER, 1L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(spaceSlotRepository, never()).delete(any());
        }
    }
}
