package kkmljh.borrow.ai.repository;

import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.support.RepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@DisplayName("A-02 하드 필터 쿼리")
class SpaceMatchRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SpaceMatchRepository spaceMatchRepository;

    @Autowired
    private SpaceSlotMatchRepository slotMatchRepository;

    private Space space(String name, String region, int capacity, Set<ActivityField> allowedFields) {
        return em.persistAndFlush(Space.builder()
                .ownerId("host1")
                .name(name)
                .region(region)
                .capacity(capacity)
                .hourlyFee(10_000)
                .facilities(Set.of(FacilityType.WATER))
                .allowedFields(allowedFields)
                .noiseAllowed(true)
                .messAllowed(true)
                .build());
    }

    private Space artSpace(String name, String region, int capacity) {
        return space(name, region, capacity, Set.of(ActivityField.ART));
    }

    @Nested
    @DisplayName("후보 조회")
    class Candidates {

        @Test
        @DisplayName("수용 인원이 모자란 공간은 제외한다")
        void filtersByCapacity() {
            artSpace("작은 공간", "천안시 서북구", 4);
            artSpace("충분한 공간", "천안시 서북구", 6);

            assertThat(spaceMatchRepository.findCandidates(6, "", ActivityField.ART))
                    .extracting(Space::getName)
                    .containsExactly("충분한 공간");
        }

        @Test
        @DisplayName("허용 분야에 없는 공간은 제외한다")
        void filtersByAllowedField() {
            artSpace("그림 공간", "천안시 서북구", 10);
            space("사진 공간", "천안시 서북구", 10, Set.of(ActivityField.PHOTO));

            assertThat(spaceMatchRepository.findCandidates(4, "", ActivityField.ART))
                    .extracting(Space::getName)
                    .containsExactly("그림 공간");
        }

        @Test
        @DisplayName("여러 분야를 허용하는 공간은 각 분야 모두에서 후보가 된다")
        void multipleAllowedFields() {
            space("복합 공간", "천안시 서북구", 10, Set.of(ActivityField.ART, ActivityField.PHOTO));

            assertThat(spaceMatchRepository.findCandidates(4, "", ActivityField.ART)).hasSize(1);
            assertThat(spaceMatchRepository.findCandidates(4, "", ActivityField.PHOTO)).hasSize(1);
        }

        @Test
        @DisplayName("지역이 빈 문자열이면 지역 조건을 무시한다")
        void emptyRegionMatchesAll() {
            artSpace("서북구 공간", "천안시 서북구 불당동", 10);
            artSpace("동남구 공간", "천안시 동남구 신부동", 10);

            assertThat(spaceMatchRepository.findCandidates(4, "", ActivityField.ART)).hasSize(2);
        }

        @Test
        @DisplayName("요청 지역이 공간 지역에 포함되면 매칭된다 (넓게 요청 → 좁은 공간)")
        void requestRegionContainedInSpaceRegion() {
            artSpace("불당동 공간", "천안시 서북구 불당동", 10);

            assertThat(spaceMatchRepository.findCandidates(4, "천안시 서북구", ActivityField.ART))
                    .extracting(Space::getName)
                    .containsExactly("불당동 공간");
        }

        @Test
        @DisplayName("공간 지역이 요청 지역에 포함돼도 매칭된다 (좁게 요청 → 넓은 공간)")
        void spaceRegionContainedInRequestRegion() {
            artSpace("서북구 공간", "천안시 서북구", 10);

            assertThat(spaceMatchRepository.findCandidates(4, "천안시 서북구 불당동", ActivityField.ART))
                    .extracting(Space::getName)
                    .containsExactly("서북구 공간");
        }

        @Test
        @DisplayName("다른 지역은 매칭되지 않는다")
        void differentRegion() {
            artSpace("서북구 공간", "천안시 서북구 불당동", 10);

            assertThat(spaceMatchRepository.findCandidates(4, "천안시 동남구", ActivityField.ART)).isEmpty();
        }

        @Test
        @DisplayName("허용 분야가 하나도 없는 공간은 후보가 되지 않는다")
        void noAllowedFields() {
            space("분야 미설정 공간", "천안시 서북구", 10, Set.of());

            assertThat(spaceMatchRepository.findCandidates(4, "", ActivityField.ART)).isEmpty();
        }
    }

    @Nested
    @DisplayName("슬롯 조회")
    class Slots {

        @Test
        @DisplayName("지정한 공간들의 해당 요일 슬롯만 한 번에 가져온다")
        void findBySpaceIdsAndDay() {
            Space first = artSpace("공간 1", "천안시 서북구", 10);
            Space second = artSpace("공간 2", "천안시 서북구", 10);
            Space excluded = artSpace("조회 대상 아님", "천안시 서북구", 10);
            em.persistAndFlush(SpaceSlot.builder().space(first)
                    .dayOfWeek(DayOfWeek.SATURDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(22, 0)).build());
            em.persistAndFlush(SpaceSlot.builder().space(first)
                    .dayOfWeek(DayOfWeek.SUNDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(22, 0)).build());
            em.persistAndFlush(SpaceSlot.builder().space(second)
                    .dayOfWeek(DayOfWeek.SATURDAY).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(18, 0)).build());
            em.persistAndFlush(SpaceSlot.builder().space(excluded)
                    .dayOfWeek(DayOfWeek.SATURDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(22, 0)).build());

            List<SpaceSlot> slots = slotMatchRepository.findBySpaceIdsAndDay(
                    List.of(first.getId(), second.getId()), DayOfWeek.SATURDAY);

            assertThat(slots).hasSize(2)
                    .allSatisfy(slot -> assertThat(slot.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY))
                    .extracting(slot -> slot.getSpace().getId())
                    .containsExactlyInAnyOrder(first.getId(), second.getId());
        }

        @Test
        @DisplayName("해당 요일 슬롯이 없으면 빈 목록")
        void noSlotsOnDay() {
            Space space = artSpace("공간", "천안시 서북구", 10);
            em.persistAndFlush(SpaceSlot.builder().space(space)
                    .dayOfWeek(DayOfWeek.SATURDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(22, 0)).build());

            assertThat(slotMatchRepository.findBySpaceIdsAndDay(List.of(space.getId()), DayOfWeek.MONDAY))
                    .isEmpty();
        }
    }
}
