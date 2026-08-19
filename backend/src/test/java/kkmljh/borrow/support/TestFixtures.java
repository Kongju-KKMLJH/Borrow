package kkmljh.borrow.support;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.AppUser;
import kkmljh.borrow.domain.ArtistVerification;
import kkmljh.borrow.domain.FacilityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Participation;
import kkmljh.borrow.domain.Role;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.domain.SpaceRequirement;
import kkmljh.borrow.domain.SpaceSlot;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 테스트 공용 엔티티 픽스처.
 *
 * <p>엔티티의 id는 JPA가 채우므로 단위 테스트에서는 리플렉션으로 심는다.
 * (setter 금지 규칙을 프로덕션 코드에서 지키기 위해 테스트에서만 사용)
 */
public final class TestFixtures {

    public static final String HOST_LOGIN_ID = "owner1";
    public static final String MEMBER_LOGIN_ID = "member1";
    public static final String ADMIN_LOGIN_ID = "admin1";

    private TestFixtures() {
    }

    /** 엔티티에 id를 심는다 (JPA 저장을 흉내). */
    public static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    /**
     * 엔티티 컬렉션 필드는 <b>가변</b>으로 채운다.
     * 실제 런타임에도 Jackson 역직렬화 결과나 Hibernate가 로딩한 컬렉션은 가변이고,
     * {@code Space.update*} 는 기존 컬렉션을 clear() 후 채우는 방식이라 불변 컬렉션을 넣으면 깨진다.
     */
    private static <T> List<T> mutable(List<T> values) {
        return new ArrayList<>(values);
    }

    private static <T> Set<T> mutable(Set<T> values) {
        return new LinkedHashSet<>(values);
    }

    // --- AppUser ---

    public static AppUser user(String loginId, String nickname, Role role) {
        return withId(AppUser.builder()
                .loginId(loginId)
                .password("{bcrypt}hashed")
                .nickname(nickname)
                .role(role)
                .build(), 1L);
    }

    public static AppUser member() {
        return user(MEMBER_LOGIN_ID, "일반회원", Role.MEMBER);
    }

    public static AppUser artist() {
        return user("artist1", "예술가", Role.ARTIST);
    }

    public static AppUser host() {
        return user(HOST_LOGIN_ID, "공간주인", Role.HOST);
    }

    public static AppUser admin() {
        return user(ADMIN_LOGIN_ID, "관리자", Role.ADMIN);
    }

    // --- ArtistVerification (기능명세 1.2 · 7.1.4) ---

    /** 심사 대기(PENDING) 상태의 예술가 인증 신청 */
    public static ArtistVerification verification(Long id, String loginId) {
        return withId(ArtistVerification.builder()
                .loginId(loginId)
                .portfolioUrl("https://portfolio.example.com/" + loginId)
                .career("수채화 클래스 3년 진행")
                .build(), id);
    }

    public static ArtistVerification verification() {
        return verification(1L, "artist1");
    }

    // --- SpaceRequirement ---

    public static SpaceRequirement requirement() {
        return SpaceRequirement.builder()
                .region("천안시 서북구")
                .headcount(6)
                .requiredFacilities(mutable(Set.of(FacilityType.TABLE, FacilityType.WATER)))
                .noisy(false)
                .messy(true)
                .build();
    }

    // --- Activity ---

    public static Activity activity(Long id, String guestId) {
        return withId(Activity.builder()
                .guestId(guestId)
                .hostNickname("일반회원")
                .hostCertified(false)
                .type(kkmljh.borrow.domain.ActivityType.HOBBY)
                .field(ActivityField.ART)
                .title("수채화 모임")
                .description("초보자 환영 수채화 모임입니다.")
                .imageUrls(mutable(List.of("/files/a.jpg")))
                .date(LocalDate.of(2026, 9, 12))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .capacity(8)
                .entryFee(10_000)
                .requirement(requirement())
                .build(), id);
    }

    public static Activity activity() {
        return activity(1L, MEMBER_LOGIN_ID);
    }

    /** 모집 공개(PUBLISHED) 상태의 활동 */
    public static Activity publishedActivity(Long id, String guestId) {
        Activity activity = activity(id, guestId);
        activity.publish();
        return activity;
    }

    // --- Space ---

    public static Space space(Long id, String ownerId) {
        return withId(Space.builder()
                .ownerId(ownerId)
                .name("불당동 스튜디오")
                .region("천안시 서북구 불당동")
                .address("불당대로 1")
                .imageUrls(mutable(List.of("/files/s.jpg")))
                .capacity(10)
                .hourlyFee(10_000)
                .conditions("음료 1잔 주문")
                .facilities(mutable(Set.of(FacilityType.TABLE, FacilityType.WATER, FacilityType.WIFI)))
                .allowedFields(mutable(Set.of(ActivityField.ART, ActivityField.PHOTO)))
                .noiseAllowed(true)
                .messAllowed(true)
                .build(), id);
    }

    public static Space space() {
        return space(1L, HOST_LOGIN_ID);
    }

    // --- SpaceSlot ---

    public static SpaceSlot slot(Long id, Space space, DayOfWeek day, LocalTime from, LocalTime to) {
        return withId(SpaceSlot.builder()
                .space(space)
                .dayOfWeek(day)
                .startTime(from)
                .endTime(to)
                .build(), id);
    }

    public static SpaceSlot slot(Long id, Space space) {
        return slot(id, space, DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(22, 0));
    }

    // --- HostingRequest ---

    public static HostingRequest hostingRequest(Long id, Activity activity, Space space) {
        return withId(HostingRequest.builder()
                .activity(activity)
                .space(space)
                .build(), id);
    }

    public static HostingRequest hostingRequest() {
        return hostingRequest(1L, activity(), space());
    }

    // --- Participation ---

    public static Participation participation(Long id, Activity activity, String guestId, int headcount) {
        return withId(Participation.builder()
                .activity(activity)
                .guestId(guestId)
                .nickname("참여자")
                .headcount(headcount)
                .build(), id);
    }
}
