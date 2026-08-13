package kkmljh.borrow.domain;

import kkmljh.borrow.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpaceSlot 엔티티 (B-05 유휴 시간대)")
class SpaceSlotTest {

    private SpaceSlot slot(DayOfWeek day, int fromHour, int toHour) {
        return SpaceSlot.builder()
                .space(TestFixtures.space())
                .dayOfWeek(day)
                .startTime(LocalTime.of(fromHour, 0))
                .endTime(LocalTime.of(toHour, 0))
                .build();
    }

    @Test
    @DisplayName("전달한 값이 그대로 담긴다")
    void creation() {
        Space space = TestFixtures.space();

        SpaceSlot slot = SpaceSlot.builder()
                .space(space)
                .dayOfWeek(DayOfWeek.SATURDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(22, 0))
                .build();

        assertThat(slot.getSpace()).isSameAs(space);
        assertThat(slot.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slot.getEndTime()).isEqualTo(LocalTime.of(22, 0));
    }

    @Test
    @DisplayName("요일이 같고 시간이 슬롯 안에 완전히 들어가면 covers() 는 true")
    void coversInsideRange() {
        SpaceSlot slot = slot(DayOfWeek.SATURDAY, 9, 22);

        assertThat(slot.covers(DayOfWeek.SATURDAY, LocalTime.of(14, 0), LocalTime.of(16, 0))).isTrue();
    }

    @Test
    @DisplayName("슬롯 경계와 정확히 같은 시간도 포함으로 본다")
    void coversExactBoundary() {
        SpaceSlot slot = slot(DayOfWeek.SATURDAY, 9, 22);

        assertThat(slot.covers(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(22, 0))).isTrue();
    }

    @Test
    @DisplayName("요일이 다르면 false")
    void doesNotCoverOtherDay() {
        SpaceSlot slot = slot(DayOfWeek.SATURDAY, 9, 22);

        assertThat(slot.covers(DayOfWeek.SUNDAY, LocalTime.of(14, 0), LocalTime.of(16, 0))).isFalse();
    }

    @Test
    @DisplayName("시작이 슬롯보다 이르면 false")
    void doesNotCoverEarlierStart() {
        SpaceSlot slot = slot(DayOfWeek.SATURDAY, 9, 22);

        assertThat(slot.covers(DayOfWeek.SATURDAY, LocalTime.of(8, 59), LocalTime.of(16, 0))).isFalse();
    }

    @Test
    @DisplayName("종료가 슬롯보다 늦으면 false")
    void doesNotCoverLaterEnd() {
        SpaceSlot slot = slot(DayOfWeek.SATURDAY, 9, 22);

        assertThat(slot.covers(DayOfWeek.SATURDAY, LocalTime.of(20, 0), LocalTime.of(22, 1))).isFalse();
    }

    @Test
    @DisplayName("슬롯을 완전히 벗어나면 false — 부분 겹침은 매칭 대상이 아니다")
    void partialOverlapIsNotCovered() {
        SpaceSlot slot = slot(DayOfWeek.SATURDAY, 13, 15);

        assertThat(slot.covers(DayOfWeek.SATURDAY, LocalTime.of(14, 0), LocalTime.of(16, 0))).isFalse();
    }
}
