package kkmljh.borrow.support;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Participation;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceSlot;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * 단위테스트용 엔티티 팩토리.
 *
 * <p>엔티티 ID는 JPA가 채우므로 순수 단위테스트에서는 null이다. 매칭 서비스의
 * 제외/그룹핑처럼 ID에 의존하는 로직을 검증하려면 ID 주입이 필요해
 * {@link ReflectionTestUtils}로 넣어준다(프로덕션 코드는 setter를 두지 않음).
 */
public final class Entities {

    private Entities() {
    }

    /** 엔티티의 {@code id} 필드를 리플렉션으로 주입한다. */
    public static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    // ===== Space =====

    public static Space space(long id, String region, int capacity, int hourlyFee,
                              Set<FacilityType> facilities, Set<ActivityField> allowedFields,
                              boolean noiseAllowed, boolean messAllowed) {
        Space space = Space.builder()
                .name("공간-" + id)
                .region(region)
                .capacity(capacity)
                .hourlyFee(hourlyFee)
                .conditions("이용 조건 없음")
                .facilities(facilities)
                .allowedFields(allowedFields)
                .noiseAllowed(noiseAllowed)
                .messAllowed(messAllowed)
                .build();
        return withId(space, id);
    }

    /** 시설/분야 제약 없이 넉넉한 기본 공간. */
    public static Space space(long id, int capacity) {
        return space(id, "천안시 서북구", capacity, 10_000,
                Set.of(), Set.of(ActivityField.ART, ActivityField.PHOTO), true, true);
    }

    // ===== SpaceSlot =====

    public static SpaceSlot slot(long id, Space space, DayOfWeek day, LocalTime start, LocalTime end) {
        SpaceSlot slot = SpaceSlot.builder()
                .space(space)
                .dayOfWeek(day)
                .startTime(start)
                .endTime(end)
                .build();
        return withId(slot, id);
    }

    // ===== Activity =====

    /** 개설 직후 상태(DRAFT)의 활동. */
    public static Activity activity(long id, String guestId, int capacity) {
        return activity(id, guestId, ActivityType.HOBBY, ActivityField.ART, capacity,
                LocalDate.of(2026, 8, 1), LocalTime.of(14, 0), LocalTime.of(16, 0));
    }

    public static Activity activity(long id, String guestId, ActivityType type, ActivityField field,
                                    int capacity, LocalDate date, LocalTime start, LocalTime end) {
        Activity activity = Activity.builder()
                .guestId(guestId)
                .hostNickname("host-" + guestId)
                .hostCertified(false)
                .type(type)
                .field(field)
                .title("활동-" + id)
                .description("설명")
                .date(date)
                .startTime(start)
                .endTime(end)
                .capacity(capacity)
                .entryFee(0)
                .requirement(null)
                .build();
        return withId(activity, id);
    }

    /** PUBLISHED 상태(모집 중)의 활동. */
    public static Activity publishedActivity(long id, String guestId, int capacity) {
        Activity activity = activity(id, guestId, capacity);
        activity.publish();
        return activity;
    }

    // ===== HostingRequest =====

    public static HostingRequest hostingRequest(long id, Activity activity, Space space) {
        HostingRequest request = HostingRequest.builder()
                .activity(activity)
                .space(space)
                .build();
        return withId(request, id);
    }

    // ===== Participation =====

    public static Participation participation(long id, Activity activity, String guestId, int headcount) {
        Participation participation = Participation.builder()
                .activity(activity)
                .guestId(guestId)
                .nickname("guest-" + guestId)
                .headcount(headcount)
                .build();
        return withId(participation, id);
    }
}
