package kkmljh.borrow.activity.repository;

import kkmljh.borrow.domain.Activity;
import kkmljh.borrow.domain.ActivityField;
import kkmljh.borrow.domain.ActivityType;
import kkmljh.borrow.domain.Participation;
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

            List<Activity> result = activityRepository.search(null, null, null);

            assertThat(result).extracting(Activity::getTitle).containsExactly("공개된 모임");
        }

        @Test
        @DisplayName("유형 필터")
        void filterByType() {
            published("취미 모임");
            save("artist1", ActivityType.CLASS, ActivityField.ART, "원데이 클래스", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);

            assertThat(activityRepository.search(ActivityType.CLASS, null, null))
                    .extracting(Activity::getTitle).containsExactly("원데이 클래스");
        }

        @Test
        @DisplayName("분야 필터")
        void filterByField() {
            published("수채화 모임");
            save("member1", ActivityType.HOBBY, ActivityField.PHOTO, "출사 모임", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);

            assertThat(activityRepository.search(null, ActivityField.PHOTO, null))
                    .extracting(Activity::getTitle).containsExactly("출사 모임");
        }

        @Test
        @DisplayName("키워드는 제목에서 부분 일치로 찾는다")
        void keywordMatchesTitle() {
            published("수채화 모임");
            published("사진 모임");

            assertThat(activityRepository.search(null, null, "수채화"))
                    .extracting(Activity::getTitle).containsExactly("수채화 모임");
        }

        @Test
        @DisplayName("키워드는 설명에서도 찾는다")
        void keywordMatchesDescription() {
            save("member1", ActivityType.HOBBY, ActivityField.ART, "모임", "초보자를 위한 수채화 클래스",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);
            published("다른 모임");

            assertThat(activityRepository.search(null, null, "수채화"))
                    .extracting(Activity::getTitle).containsExactly("모임");
        }

        @Test
        @DisplayName("필터가 모두 null 이면 공개된 활동 전체가 나온다")
        void noFilters() {
            published("모임1");
            published("모임2");

            assertThat(activityRepository.search(null, null, null)).hasSize(2);
        }

        @Test
        @DisplayName("여러 필터는 AND 로 묶인다")
        void combinedFilters() {
            save("artist1", ActivityType.CLASS, ActivityField.PHOTO, "사진 클래스", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);
            save("artist1", ActivityType.CLASS, ActivityField.ART, "그림 클래스", "설명",
                    LocalDate.of(2026, 9, 12), LocalTime.of(14, 0), true);

            assertThat(activityRepository.search(ActivityType.CLASS, ActivityField.PHOTO, "사진"))
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

            assertThat(activityRepository.search(null, null, null))
                    .extracting(Activity::getTitle)
                    .containsExactly("같은 날 이른 시각", "같은 날 늦은 시각", "늦은 날");
        }

        @Test
        @DisplayName("일치하는 활동이 없으면 빈 목록")
        void noMatch() {
            published("수채화 모임");

            assertThat(activityRepository.search(null, null, "존재하지않는키워드")).isEmpty();
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
}
