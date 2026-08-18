package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.HostingRequest;
import kkmljh.borrow.domain.Participation;
import kkmljh.borrow.domain.Space;
import kkmljh.borrow.support.RepositoryTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
@DisplayName("활동 · 참여 리포지토리 쿼리")
class ActivityRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private ActivityHostingRequestRepository hostingRequestRepository;

    private Activity save(String guestId, ActivityType type, ActivityField field,
                          String title, String description, LocalDate date, LocalTime start, boolean publish) {
        Activity activity = Activity.builder()
                .guestId(guestId)
                .hostNickname("개설자")
                .hostCertified(type == ActivityType.CLASS)
                .type(type)
                .field(field)
                .title(title)
                .description(description)
                .date(date)
                .startTime(start)
                .endTime(start.plusHours(2))
                .capacity(8)
                .entryFee(0)
                .build();
        if (publish) {
            activity.publish();
        }
        return em.persistAndFlush(activity);
    }

    private Space space(String name, String region) {
        return em.persistAndFlush(Space.builder()
                .ownerId("owner1")
                .name(name)
                .region(region)
                .address(region + " 123-4 2층")
                .capacity(20)
                .hourlyFee(15_000)
                .build());
    }

    /** 개최 요청을 만들고, approve=true면 승인(=개최지 확정, S-01 공개)까지 시킨다. */
    private HostingRequest request(Activity activity, Space space, boolean approve) {
        HostingRequest request = HostingRequest.builder().activity(activity).space(space).build();
        if (approve) {
            request.approve();
        }
        return em.persistAndFlush(request);
    }

    private Activity published(String title) {
        return save("member1", ActivityType.HOBBY, ActivityField.ART, title, "설명",
                LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);
    }

    @Nested
    @DisplayName("U-01/U-02 목록·검색")
    class Search {

        @Test
        @DisplayName("PUBLISHED 상태만 노출한다 (S-01 공개 전 활동은 목록에 없다)")
        void onlyPublished() {
            published("공개된 모임");
            save("member1", ActivityType.HOBBY, ActivityField.ART, "초안 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), false);

            List<Activity> result = activityRepository.search(null, null, null, null, null, null);

            assertThat(result).extracting(Activity::getTitle).containsExactly("공개된 모임");
        }

        @Test
        @DisplayName("유형 필터")
        void filterByType() {
            published("취미 모임");
            save("artist1", ActivityType.CLASS, ActivityField.ART, "원데이 클래스", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);

            assertThat(activityRepository.search(ActivityType.CLASS, null, null, null, null, null))
                    .extracting(Activity::getTitle).containsExactly("원데이 클래스");
        }

        @Test
        @DisplayName("분야 필터")
        void filterByField() {
            published("수채화 모임");
            save("member1", ActivityType.HOBBY, ActivityField.PHOTO, "출사 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);

            assertThat(activityRepository.search(null, ActivityField.PHOTO, null, null, null, null))
                    .extracting(Activity::getTitle).containsExactly("출사 모임");
        }

        @Test
        @DisplayName("키워드는 제목에서 부분 일치로 찾는다")
        void keywordMatchesTitle() {
            published("수채화 모임");
            published("사진 모임");

            assertThat(activityRepository.search(null, null, "수채화", null, null, null))
                    .extracting(Activity::getTitle).containsExactly("수채화 모임");
        }

        @Test
        @DisplayName("키워드는 설명에서도 찾는다")
        void keywordMatchesDescription() {
            save("member1", ActivityType.HOBBY, ActivityField.ART, "모임", "초보자를 위한 수채화 클래스",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);
            published("다른 모임");

            assertThat(activityRepository.search(null, null, "수채화", null, null, null))
                    .extracting(Activity::getTitle).containsExactly("모임");
        }

        @Test
        @DisplayName("필터가 모두 null 이면 공개된 활동 전체가 나온다")
        void noFilters() {
            published("모임1");
            published("모임2");

            assertThat(activityRepository.search(null, null, null, null, null, null)).hasSize(2);
        }

        @Test
        @DisplayName("여러 필터는 AND 로 묶인다")
        void combinedFilters() {
            save("artist1", ActivityType.CLASS, ActivityField.PHOTO, "사진 클래스", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);
            save("artist1", ActivityType.CLASS, ActivityField.ART, "그림 클래스", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);

            assertThat(activityRepository.search(ActivityType.CLASS, ActivityField.PHOTO, "사진", null, null, null))
                    .extracting(Activity::getTitle).containsExactly("사진 클래스");
        }

        @Test
        @DisplayName("날짜 → 시작 시각 오름차순으로 정렬된다")
        void sortedByDateAndTime() {
            save("member1", ActivityType.HOBBY, ActivityField.ART, "늦은 날", "설명",
                    LocalDate.of(2026, 9, 20), LocalTime.of(10, 0), true);
            save("member1", ActivityType.HOBBY, ActivityField.ART, "같은 날 늦은 시각", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(18, 0), true);
            save("member1", ActivityType.HOBBY, ActivityField.ART, "같은 날 이른 시각", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(9, 0), true);

            assertThat(activityRepository.search(null, null, null, null, null, null))
                    .extracting(Activity::getTitle)
                    .containsExactly("같은 날 이른 시각", "같은 날 늦은 시각", "늦은 날");
        }

        @Test
        @DisplayName("일치하는 활동이 없으면 빈 목록")
        void noMatch() {
            published("수채화 모임");

            assertThat(activityRepository.search(null, null, "존재하지않는키워드", null, null, null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("U-13 내가 개설한 활동")
    class MyActivities {

        @Test
        @DisplayName("내 것만, 상태와 무관하게, 최신순으로 나온다")
        void findByGuestId() {
            save("member1", ActivityType.HOBBY, ActivityField.ART, "첫 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), false);
            save("member1", ActivityType.HOBBY, ActivityField.ART, "둘째 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);
            save("other", ActivityType.HOBBY, ActivityField.ART, "남의 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);

            assertThat(activityRepository.findByGuestIdOrderByIdDesc("member1"))
                    .extracting(Activity::getTitle)
                    .containsExactly("둘째 모임", "첫 모임");
        }

        @Test
        @DisplayName("개설한 활동이 없으면 빈 목록")
        void empty() {
            assertThat(activityRepository.findByGuestIdOrderByIdDesc("nobody")).isEmpty();
        }
    }

    @Nested
    @DisplayName("참여 쿼리")
    class Participations {

        private Activity activity;

        @BeforeEach
        void setUp() {
            activity = published("수채화 모임");
        }

        private Participation join(String guestId, int headcount) {
            return em.persistAndFlush(Participation.builder()
                    .activity(activity)
                    .guestId(guestId)
                    .nickname(guestId)
                    .headcount(headcount)
                    .build());
        }

        @Test
        @DisplayName("참여 인원 합계 — 신청이 없으면 0 (null 아님)")
        void sumHeadcountEmpty() {
            assertThat(participationRepository.sumHeadcountByActivityId(activity.getId())).isZero();
        }

        @Test
        @DisplayName("참여 인원 합계는 여러 신청을 더한다")
        void sumHeadcount() {
            join("guest1", 2);
            join("guest2", 3);

            assertThat(participationRepository.sumHeadcountByActivityId(activity.getId())).isEqualTo(5);
        }

        @Test
        @DisplayName("다른 활동의 신청은 합계에 섞이지 않는다")
        void sumHeadcountScopedToActivity() {
            join("guest1", 2);
            Activity other = published("다른 모임");
            em.persistAndFlush(Participation.builder()
                    .activity(other).guestId("guest2").nickname("guest2").headcount(4).build());

            assertThat(participationRepository.sumHeadcountByActivityId(activity.getId())).isEqualTo(2);
            assertThat(participationRepository.sumHeadcountByActivityId(other.getId())).isEqualTo(4);
        }

        @Test
        @DisplayName("게스트가 참여 중인 활동 id 집합을 한 번에 가져온다")
        void findActivityIdsByGuestId() {
            join("guest1", 1);
            Activity other = published("다른 모임");
            em.persistAndFlush(Participation.builder()
                    .activity(other).guestId("guest1").nickname("guest1").headcount(1).build());

            Set<Long> ids = participationRepository.findActivityIdsByGuestId("guest1");

            assertThat(ids).containsExactlyInAnyOrder(activity.getId(), other.getId());
        }

        @Test
        @DisplayName("참여하지 않은 게스트는 빈 집합")
        void noParticipation() {
            assertThat(participationRepository.findActivityIdsByGuestId("nobody")).isEmpty();
        }

        @Test
        @DisplayName("활동+게스트로 단건 조회·존재 확인")
        void findAndExists() {
            join("guest1", 2);

            assertThat(participationRepository.findByActivityIdAndGuestId(activity.getId(), "guest1"))
                    .isPresent()
                    .get()
                    .extracting(Participation::getHeadcount).isEqualTo(2);
            assertThat(participationRepository.existsByActivityIdAndGuestId(activity.getId(), "guest1")).isTrue();
            assertThat(participationRepository.existsByActivityIdAndGuestId(activity.getId(), "guest2")).isFalse();
        }

        @Test
        @DisplayName("내가 참여한 활동은 최신순으로 나온다")
        void myParticipationsDesc() {
            join("guest1", 1);
            Activity other = published("다른 모임");
            em.persistAndFlush(Participation.builder()
                    .activity(other).guestId("guest1").nickname("guest1").headcount(1).build());

            assertThat(participationRepository.findByGuestIdOrderByIdDesc("guest1"))
                    .extracting(p -> p.getActivity().getTitle())
                    .containsExactly("다른 모임", "수채화 모임");
        }
    }

    @Nested
    @DisplayName("기능명세 4.1 지역·일정 필터")
    class RegionAndDateFilter {

        /** 승인된 개최지가 붙은 공개 활동 하나를 만든다. */
        private Activity hostedAt(String title, LocalDate date, String region) {
            Activity activity = save("member1", ActivityType.HOBBY, ActivityField.ART, title, "설명",
                    date, LocalTime.of(14, 0), false);
            request(activity, space(region + " 공간", region), true);   // approve() 안에서 publish 된다
            em.flush();
            return activity;
        }

        @Test
        @DisplayName("지역은 부분일치 — 동 단위로 검색해도 걸린다")
        void regionPartialMatch() {
            hostedAt("불당동 모임", LocalDate.of(2026, 9, 12), "천안시 서북구 불당동");

            assertThat(activityRepository.search(null, null, null, "불당동", null, null))
                    .extracting(Activity::getTitle).containsExactly("불당동 모임");
        }

        @Test
        @DisplayName("지역은 부분일치 — 시 단위로 검색해도 걸린다")
        void regionMatchesByCity() {
            hostedAt("불당동 모임", LocalDate.of(2026, 9, 12), "천안시 서북구 불당동");

            assertThat(activityRepository.search(null, null, null, "천안시", null, null))
                    .extracting(Activity::getTitle).containsExactly("불당동 모임");
        }

        @Test
        @DisplayName("다른 지역은 걸리지 않는다")
        void regionMismatch() {
            hostedAt("불당동 모임", LocalDate.of(2026, 9, 12), "천안시 서북구 불당동");

            assertThat(activityRepository.search(null, null, null, "아산시", null, null)).isEmpty();
        }

        @Test
        @DisplayName("승인되지 않은(PENDING) 개최 요청의 공간으로는 걸리지 않는다")
        void pendingRequestNotMatched() {
            Activity activity = published("심사 중 모임");
            request(activity, space("불당 카페", "천안시 서북구 불당동"), false);
            em.flush();

            assertThat(activityRepository.search(null, null, null, "불당동", null, null)).isEmpty();
        }

        @Test
        @DisplayName("개최 요청이 여러 건이어도 활동이 중복 행으로 나오지 않는다 (EXISTS 서브쿼리)")
        void noDuplicateRowsForMultipleRequests() {
            Activity activity = save("member1", ActivityType.HOBBY, ActivityField.ART, "재요청 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), false);
            request(activity, space("A 카페", "천안시 서북구 불당동"), false);
            request(activity, space("B 카페", "천안시 서북구 불당동"), true);
            em.flush();

            assertThat(activityRepository.search(null, null, null, "불당동", null, null))
                    .extracting(Activity::getTitle).containsExactly("재요청 모임");
        }

        @Test
        @DisplayName("지역으로 걸러도 PUBLISHED 가 아닌 활동은 나오지 않는다")
        void regionStillOnlyPublished() {
            Activity draft = save("member1", ActivityType.HOBBY, ActivityField.ART, "초안 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), false);
            // 승인 없이 요청만 — 활동은 DRAFT 로 남는다
            request(draft, space("불당 카페", "천안시 서북구 불당동"), false);
            em.flush();

            assertThat(activityRepository.search(null, null, null, "불당동", null, null)).isEmpty();
        }

        @Test
        @DisplayName("dateFrom 은 당일을 포함한다")
        void dateFromIsInclusive() {
            published("9/12 모임");

            assertThat(activityRepository.search(null, null, null, null, LocalDate.of(2026, 9, 12), null))
                    .extracting(Activity::getTitle).containsExactly("9/12 모임");
        }

        @Test
        @DisplayName("dateTo 는 당일을 포함한다")
        void dateToIsInclusive() {
            published("9/12 모임");

            assertThat(activityRepository.search(null, null, null, null, null, LocalDate.of(2026, 9, 12)))
                    .extracting(Activity::getTitle).containsExactly("9/12 모임");
        }

        @Test
        @DisplayName("범위 밖 날짜는 걸리지 않는다")
        void outOfRange() {
            published("9/12 모임");

            assertThat(activityRepository.search(null, null, null, null,
                    LocalDate.of(2026, 9, 13), null)).isEmpty();
            assertThat(activityRepository.search(null, null, null, null,
                    null, LocalDate.of(2026, 9, 11))).isEmpty();
        }

        @Test
        @DisplayName("dateFrom·dateTo 를 함께 주면 그 구간만 나온다")
        void dateRange() {
            save("member1", ActivityType.HOBBY, ActivityField.ART, "이른 모임", "설명",
                    LocalDate.of(2026, 9, 1), LocalTime.of(14, 0), true);
            save("member1", ActivityType.HOBBY, ActivityField.ART, "구간 안 모임", "설명",
                    LocalDate.of(2026, 9, 15), LocalTime.of(14, 0), true);
            save("member1", ActivityType.HOBBY, ActivityField.ART, "늦은 모임", "설명",
                    LocalDate.of(2026, 10, 1), LocalTime.of(14, 0), true);

            assertThat(activityRepository.search(null, null, null, null,
                    LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20)))
                    .extracting(Activity::getTitle).containsExactly("구간 안 모임");
        }

        @Test
        @DisplayName("지역·일정·유형·분야·키워드는 AND 로 묶인다")
        void combinedWithExistingFilters() {
            hostedAt("불당동 수채화 모임", LocalDate.of(2026, 9, 12), "천안시 서북구 불당동");
            hostedAt("두정동 수채화 모임", LocalDate.of(2026, 9, 12), "천안시 서북구 두정동");

            assertThat(activityRepository.search(ActivityType.HOBBY, ActivityField.ART, "수채화",
                    "불당동", LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 12)))
                    .extracting(Activity::getTitle).containsExactly("불당동 수채화 모임");
        }
    }

    @Nested
    @DisplayName("기능명세 4.1 확정 공간 배치 조회")
    class ConfirmedSpaces {

        @Test
        @DisplayName("승인된 요청의 공간만, 활동 id와 함께 돌려준다")
        void approvedOnly() {
            Activity approved = save("member1", ActivityType.HOBBY, ActivityField.ART, "확정 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), false);
            request(approved, space("불당 카페", "천안시 서북구 불당동"), true);

            Activity pending = published("심사 중 모임");
            request(pending, space("두정 카페", "천안시 서북구 두정동"), false);
            em.flush();

            List<ConfirmedSpace> result = hostingRequestRepository
                    .findConfirmedSpaces(List.of(approved.getId(), pending.getId()));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).activityId()).isEqualTo(approved.getId());
            assertThat(result.get(0).name()).isEqualTo("불당 카페");
            assertThat(result.get(0).region()).isEqualTo("천안시 서북구 불당동");
        }

        @Test
        @DisplayName("거절된 요청은 확정으로 보지 않는다")
        void rejectedNotConfirmed() {
            Activity activity = save("member1", ActivityType.HOBBY, ActivityField.ART, "거절 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), false);
            HostingRequest rejected = HostingRequest.builder()
                    .activity(activity).space(space("불당 카페", "천안시 서북구 불당동")).build();
            rejected.reject("일정 불가");
            em.persistAndFlush(rejected);

            assertThat(hostingRequestRepository.findConfirmedSpaces(List.of(activity.getId()))).isEmpty();
        }

        @Test
        @DisplayName("여러 활동을 한 번에 조회한다 (목록 N+1 방지)")
        void batch() {
            Activity first = save("member1", ActivityType.HOBBY, ActivityField.ART, "모임1", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), false);
            Activity second = save("member1", ActivityType.HOBBY, ActivityField.ART, "모임2", "설명",
                    LocalDate.of(2026, 9, 13), LocalTime.of(14, 0), false);
            request(first, space("불당 카페", "천안시 서북구 불당동"), true);
            request(second, space("두정 카페", "천안시 서북구 두정동"), true);
            em.flush();

            assertThat(hostingRequestRepository.findConfirmedSpaces(List.of(first.getId(), second.getId())))
                    .extracting(ConfirmedSpace::name)
                    .containsExactlyInAnyOrder("불당 카페", "두정 카페");
        }

        @Test
        @DisplayName("확정된 개최지가 없으면 빈 목록")
        void none() {
            Activity activity = published("요청 없는 모임");

            assertThat(hostingRequestRepository.findConfirmedSpaces(List.of(activity.getId()))).isEmpty();
        }
    }
}
