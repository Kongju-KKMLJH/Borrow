package kkmljh.borrow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** 공간의 유휴 시간대 (B-05): 요일 + 시작/종료 시각 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Builder
    private SpaceSlot(Space space, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.space = space;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** 주어진 요일/시간 범위가 이 슬롯 안에 완전히 포함되는지 */
    public boolean covers(DayOfWeek day, LocalTime from, LocalTime to) {
        return this.dayOfWeek == day
                && !from.isBefore(this.startTime)
                && !to.isAfter(this.endTime);
    }
}
