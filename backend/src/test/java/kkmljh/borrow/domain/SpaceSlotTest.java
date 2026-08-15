package kkmljh.borrow.domain;

import kkmljh.borrow.support.Entities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpaceSlot.covers — A-02 슬롯 시간 겹침 판정")
class SpaceSlotTest {

    private final Space space = Entities.space(1L, 10);
    // 월요일 09:00~18:00 유휴 슬롯
    private final SpaceSlot slot =
            Entities.slot(1L, space, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

    @Test
    @DisplayName("같은 요일, 시간 범위가 슬롯 안에 완전히 포함되면 true")
    void coversWhenFullyContained() {
        assertThat(slot.covers(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))).isTrue();
    }

    @Test
    @DisplayName("시작·종료가 슬롯 경계와 정확히 일치해도 true (경계 포함)")
    void coversAtExactBoundary() {
        assertThat(slot.covers(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))).isTrue();
    }

    @Test
    @DisplayName("요일이 다르면 false")
    void doesNotCoverDifferentDay() {
        assertThat(slot.covers(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))).isFalse();
    }

    @Test
    @DisplayName("시작 시각이 슬롯 시작보다 이르면 false")
    void doesNotCoverWhenStartsTooEarly() {
        assertThat(slot.covers(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))).isFalse();
    }

    @Test
    @DisplayName("종료 시각이 슬롯 종료보다 늦으면 false")
    void doesNotCoverWhenEndsTooLate() {
        assertThat(slot.covers(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(19, 0))).isFalse();
    }
}
