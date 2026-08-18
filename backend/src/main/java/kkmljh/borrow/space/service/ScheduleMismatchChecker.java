package kkmljh.borrow.space.service;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.SpaceSlot;
import kkmljh.borrow.space.repository.SpaceSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;

/**
 * F-XOKOSU: 개최 요청의 활동 일정이 공간에 등록된 유휴시간(슬롯)과 맞는지 식별한다.
 * AI 매칭(A-02 하드필터)의 {@link SpaceSlot#covers} 판정과 동일한 기준을 사업자 검토 화면(B-07/B-08/B-01)에도 적용한다.
 */
@Component
@RequiredArgsConstructor
public class ScheduleMismatchChecker {

    private final SpaceSlotRepository spaceSlotRepository;

    /** 활동 일정을 완전히 포함하는 슬롯이 하나도 없으면 true(일정 조건 불일치) */
    public boolean isMismatch(HostingRequest request) {
        Activity activity = request.getActivity();
        DayOfWeek day = activity.getDate().getDayOfWeek();
        List<SpaceSlot> slots = spaceSlotRepository
                .findBySpaceIdOrderByDayOfWeekAscStartTimeAsc(request.getSpace().getId());
        boolean covered = slots.stream()
                .anyMatch(slot -> slot.covers(day, activity.getStartTime(), activity.getEndTime()));
        return !covered;
    }
}
